package com.forgeauth.server.config;

import com.forgeauth.domain.user.model.User;
import com.forgeauth.domain.user.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;

@Configuration
public class JwtTokenCustomizerConfig {

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer(UserRepository userRepository) {
        return context -> {
            if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue()) ||
                "access_token".equals(context.getTokenType().getValue())) {
                
                Authentication principal = context.getPrincipal();
                
                // Fetch user from database using the principal name (which should be the email or ID)
                // Assuming principal.getName() is the user's email or ID. We'll use email for now.
                userRepository.findByEmail(principal.getName()).ifPresent(user -> {
                    context.getClaims().claim("email", user.getEmail());
                    context.getClaims().claim("email_verified", user.isEmailVerified());
                    context.getClaims().claim("name", user.getDisplayName());
                    if (user.getAvatarUrl() != null) {
                        context.getClaims().claim("picture", user.getAvatarUrl());
                    }
                });
            }
        };
    }
}
