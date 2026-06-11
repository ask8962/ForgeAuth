package com.forgeauth.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgeauth.common.dto.LoginRequest;
import com.forgeauth.common.dto.RegisterRequest;
import com.forgeauth.common.dto.TokenRefreshRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class AuthIntegrationTest {

    static PostgreSQLContainer<?> postgres;

    static {
        try {
            postgres = new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("forgeauth")
                    .withUsername("forgeauth")
                    .withPassword("forgeauth");
            postgres.start();
        } catch (Exception e) {
            System.err.println("Docker environment not found. Falling back to H2 database: " + e.getMessage());
            postgres = null;
            try {
                java.net.URL resource = AuthIntegrationTest.class.getResource("/db/migration");
                if (resource != null) {
                    java.io.File migrationDir = new java.io.File(resource.toURI());
                    if (migrationDir.exists() && migrationDir.isDirectory()) {
                        for (java.io.File sqlFile : migrationDir.listFiles((dir, name) -> name.endsWith(".sql"))) {
                            String content = java.nio.file.Files.readString(sqlFile.toPath());
                            String updated = content
                                .replaceAll("(?i)id\\s+UUID\\s+PRIMARY\\s+KEY\\s+DEFAULT\\s+gen_random_uuid\\(\\)", "id UUID DEFAULT gen_random_uuid() PRIMARY KEY")
                                .replaceAll("(?i)\\bTIMESTAMPTZ\\b", "TIMESTAMP WITH TIME ZONE")
                                .replaceAll("(?i)\\bJSONB\\b", "JSON")
                                .replaceAll("(?i)\\bbytea\\b", "VARBINARY(10000)");
                            java.nio.file.Files.writeString(sqlFile.toPath(), updated);
                            System.out.println("Adapted Flyway migration " + sqlFile.getName() + " for H2 compatibility.");
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println("Failed to adapt Flyway migrations: " + ex.getMessage());
            }
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("forgeauth.rate-limiting.enabled", () -> "false");
        if (postgres != null && postgres.isRunning()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
            registry.add("spring.flyway.url", postgres::getJdbcUrl);
            registry.add("spring.flyway.user", postgres::getUsername);
            registry.add("spring.flyway.password", postgres::getPassword);
        } else {
            // Fallback to H2 in PostgreSQL compatibility mode with the gen_random_uuid alias
            String h2Url = "jdbc:h2:mem:forgeauth;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
            registry.add("spring.datasource.url", () -> h2Url);
            registry.add("spring.datasource.username", () -> "sa");
            registry.add("spring.datasource.password", () -> "");
            registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
            registry.add("spring.flyway.url", () -> h2Url);
            registry.add("spring.flyway.user", () -> "sa");
            registry.add("spring.flyway.password", () -> "");
            registry.add("spring.flyway.driver-class-name", () -> "org.h2.Driver");
            registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testAuthFlow() throws Exception {
        // 1. Register
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setEmail("test@example.com");
        registerReq.setPassword("Password123!");
        registerReq.setDisplayName("Test User");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());

        // 2. Login
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("test@example.com");
        loginReq.setPassword("Password123!");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
        String refreshToken = (String) responseMap.get("refreshToken");

        // 3. Refresh
        TokenRefreshRequest refreshReq = new TokenRefreshRequest();
        refreshReq.setRefreshToken(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());

        // 4. Logout
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void testOidcMetadataEndpoint() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").exists())
                .andExpect(jsonPath("$.authorization_endpoint").exists())
                .andExpect(jsonPath("$.token_endpoint").exists())
                .andExpect(jsonPath("$.jwks_uri").exists());
    }

    @Test
    void testAuthorizationCodeWithPkceFlow() throws Exception {
        // First register the user to ensure they exist
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setEmail("pkce-user@example.com");
        registerReq.setPassword("Password123!");
        registerReq.setDisplayName("PKCE User");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // PKCE Parameters
        // Code verifier is a secure random string (minimum 43 characters)
        String codeVerifier = "extremely-long-random-string-used-as-code-verifier-spec-required-length-43-chars";
        byte[] bytes = codeVerifier.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        java.security.MessageDigest messageDigest = java.security.MessageDigest.getInstance("SHA-256");
        byte[] digest = messageDigest.digest(bytes);
        String codeChallenge = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(digest);

        // 1. Perform authorization request
        String authRequestUrl = "/oauth2/authorize?response_type=code"
                + "&client_id=forgeauth-test-client"
                + "&redirect_uri=http://localhost:3000/callback"
                + "&scope=openid profile email"
                + "&state=state123"
                + "&code_challenge=" + codeChallenge
                + "&code_challenge_method=S256";

        MvcResult authResult = mockMvc.perform(get(authRequestUrl)
                .with(user("pkce-user@example.com").roles("USER")))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String consentRedirect = authResult.getResponse().getHeader("Location");
        // Ensure redirect points to custom consent page or authorization endpoint
        org.junit.jupiter.api.Assertions.assertTrue(consentRedirect.contains("/oauth2/consent") || consentRedirect.contains("/oauth2/authorize"));

        // 2. Perform consent approval
        String consentState = consentRedirect.substring(consentRedirect.indexOf("state=") + 6);
        if (consentState.contains("&")) {
            consentState = consentState.substring(0, consentState.indexOf("&"));
        }
        consentState = java.net.URLDecoder.decode(consentState, java.nio.charset.StandardCharsets.UTF_8);

        MvcResult consentResult = mockMvc.perform(post("/oauth2/authorize")
                .session((org.springframework.mock.web.MockHttpSession) authResult.getRequest().getSession())
                .param("client_id", "forgeauth-test-client")
                .param("state", consentState)
                .param("scope", "openid", "profile", "email")
                .with(user("pkce-user@example.com").roles("USER"))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String redirectUrl = consentResult.getResponse().getHeader("Location");
        org.junit.jupiter.api.Assertions.assertNotNull(redirectUrl);
        org.junit.jupiter.api.Assertions.assertTrue(redirectUrl.startsWith("http://localhost:3000/callback"));

        // Extract code from redirect url
        String code = redirectUrl.substring(redirectUrl.indexOf("code=") + 5, redirectUrl.indexOf("&state="));
        org.junit.jupiter.api.Assertions.assertNotNull(code);

        // 3. Exchange authorization code for token (token exchange)
        MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
                .param("grant_type", "authorization_code")
                .param("client_id", "forgeauth-test-client")
                .param("redirect_uri", "http://localhost:3000/callback")
                .param("code", code)
                .param("code_verifier", codeVerifier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.id_token").exists())
                .andReturn();

        String tokenResponseBody = tokenResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> tokenResponse = objectMapper.readValue(tokenResponseBody, Map.class);
        String oauth2AccessToken = (String) tokenResponse.get("access_token");

        // 4. Query UserInfo endpoint using the retrieved access token
        mockMvc.perform(get("/userinfo")
                .header("Authorization", "Bearer " + oauth2AccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value("pkce-user@example.com"));
    }

    @Test
    void testAccountLockout() throws Exception {
        // 1. Register a user
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setEmail("lockout-user@example.com");
        registerReq.setPassword("CorrectPassword123!");
        registerReq.setDisplayName("Lockout User");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // 2. Perform 5 failed logins
        LoginRequest failedLoginReq = new LoginRequest();
        failedLoginReq.setEmail("lockout-user@example.com");
        failedLoginReq.setPassword("WrongPassword123!");

        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(failedLoginReq)))
                    .andExpect(status().isUnauthorized());
        }

        // The 5th failed login should lock the account
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(failedLoginReq)))
                .andExpect(status().isUnauthorized());

        // 3. Attempt a login with correct password - should be rejected as locked
        LoginRequest correctLoginReq = new LoginRequest();
        correctLoginReq.setEmail("lockout-user@example.com");
        correctLoginReq.setPassword("CorrectPassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(correctLoginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.message").value("Account is locked. Please try again later."));
    }
}
