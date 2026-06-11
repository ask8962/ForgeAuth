package com.forgeauth.server.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TestClientInitializer {

    @Bean
    public ApplicationRunner registerTestClient(RegisteredClientRepository clientRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String testClientId = "forgeauth-test-client";

            if (clientRepository.findByClientId(testClientId) != null) {
                log.info("Test OAuth2 client '{}' already exists, skipping registration", testClientId);
                return;
            }

            RegisteredClient testClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(testClientId)
                    .clientSecret(passwordEncoder.encode("test-secret"))
                    .clientName("ForgeAuth Test Client")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.NONE) // For public clients (PKCE)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .authorizationGrantType(AuthorizationGrantType.DEVICE_CODE)
                    .redirectUri("http://localhost:3000/callback")
                    .redirectUri("http://127.0.0.1:3000/callback")
                    .redirectUri("https://forgeauth-frontend.vercel.app/callback")
                    .postLogoutRedirectUri("http://localhost:3000")
                    .postLogoutRedirectUri("https://forgeauth-frontend.vercel.app")
                    .scope(OidcScopes.OPENID)
                    .scope(OidcScopes.PROFILE)
                    .scope(OidcScopes.EMAIL)
                    .scope("read")
                    .scope("write")
                    .clientSettings(ClientSettings.builder()
                            .requireAuthorizationConsent(true)
                            .requireProofKey(false) // Allow both PKCE and non-PKCE
                            .build())
                    .tokenSettings(TokenSettings.builder()
                            .accessTokenTimeToLive(Duration.ofMinutes(15))
                            .refreshTokenTimeToLive(Duration.ofDays(7))
                            .reuseRefreshTokens(false)
                            .build())
                    .build();

            clientRepository.save(testClient);
            log.info("Registered test OAuth2 client: clientId='{}', secret='test-secret'", testClientId);
        };
    }
}
