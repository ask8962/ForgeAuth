-- ============================================
-- APPLICATIONS (OAuth Clients)
-- ============================================
CREATE TABLE applications (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    organization_id     UUID, -- nullable for now until Phase 4 (Orgs) is implemented
    name                VARCHAR(100) NOT NULL,
    description         VARCHAR(500),
    client_id           VARCHAR(64) NOT NULL UNIQUE,
    client_secret_hash  VARCHAR(255) NOT NULL,
    application_type    VARCHAR(20) NOT NULL DEFAULT 'WEB',  -- WEB, SPA, NATIVE, M2M
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    homepage_url        VARCHAR(500),
    logo_url            VARCHAR(500),
    privacy_policy_url  VARCHAR(500),
    terms_url           VARCHAR(500),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_app_client_id ON applications(client_id);

CREATE TABLE application_redirect_uris (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    application_id  UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    uri             VARCHAR(500) NOT NULL
);
CREATE INDEX idx_redirect_app ON application_redirect_uris(application_id);

CREATE TABLE application_grant_types (
    application_id  UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    grant_type      VARCHAR(50) NOT NULL,
    PRIMARY KEY (application_id, grant_type)
);

CREATE TABLE application_scopes (
    application_id  UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    scope           VARCHAR(50) NOT NULL,
    PRIMARY KEY (application_id, scope)
);

-- ============================================
-- OAuth AUTHORIZATION CODES (for Spring Authorization Server)
-- ============================================
-- Spring Authorization Server has a specific schema for its authorization service if we use JdbcOAuth2AuthorizationService.
-- We will use the standard Spring Security schema for interoperability.
CREATE TABLE oauth2_authorization (
    id varchar(100) NOT NULL,
    registered_client_id varchar(100) NOT NULL,
    principal_name varchar(200) NOT NULL,
    authorization_grant_type varchar(100) NOT NULL,
    authorized_scopes varchar(1000) DEFAULT NULL,
    attributes VARBINARY(10000) DEFAULT NULL,
    state varchar(500) DEFAULT NULL,
    authorization_code_value VARBINARY(10000) DEFAULT NULL,
    authorization_code_issued_at timestamp DEFAULT NULL,
    authorization_code_expires_at timestamp DEFAULT NULL,
    authorization_code_metadata VARBINARY(10000) DEFAULT NULL,
    access_token_value VARBINARY(10000) DEFAULT NULL,
    access_token_issued_at timestamp DEFAULT NULL,
    access_token_expires_at timestamp DEFAULT NULL,
    access_token_metadata VARBINARY(10000) DEFAULT NULL,
    access_token_type varchar(100) DEFAULT NULL,
    access_token_scopes varchar(1000) DEFAULT NULL,
    oidc_id_token_value VARBINARY(10000) DEFAULT NULL,
    oidc_id_token_issued_at timestamp DEFAULT NULL,
    oidc_id_token_expires_at timestamp DEFAULT NULL,
    oidc_id_token_metadata VARBINARY(10000) DEFAULT NULL,
    refresh_token_value VARBINARY(10000) DEFAULT NULL,
    refresh_token_issued_at timestamp DEFAULT NULL,
    refresh_token_expires_at timestamp DEFAULT NULL,
    refresh_token_metadata VARBINARY(10000) DEFAULT NULL,
    user_code_value VARBINARY(10000) DEFAULT NULL,
    user_code_issued_at timestamp DEFAULT NULL,
    user_code_expires_at timestamp DEFAULT NULL,
    user_code_metadata VARBINARY(10000) DEFAULT NULL,
    device_code_value VARBINARY(10000) DEFAULT NULL,
    device_code_issued_at timestamp DEFAULT NULL,
    device_code_expires_at timestamp DEFAULT NULL,
    device_code_metadata VARBINARY(10000) DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE oauth2_authorization_consent (
    registered_client_id varchar(100) NOT NULL,
    principal_name varchar(200) NOT NULL,
    authorities varchar(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);
