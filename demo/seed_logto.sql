-- Seeds the Logto database with a pre-configured demo application and API resource.
-- Idempotent: uses ON CONFLICT DO NOTHING so re-runs are safe.

-- Demo application (SPA, public client)
INSERT INTO applications (tenant_id, id, name, secret, description, type, is_third_party, oidc_client_metadata, custom_client_metadata, created_at)
VALUES (
    'default',
    'cookmaid-demo',
    'Cookmaid Demo',
    '#internal:demo-placeholder-secret',
    'Demo application',
    'SPA',
    false,
    '{"grantTypes": ["authorization_code", "refresh_token"], "redirectUris": ["http://localhost:8081/callback"], "rotateRefreshToken": true, "postLogoutRedirectUris": ["http://localhost:8081/callback"], "alwaysIssueRefreshToken": true, "backchannelLogoutSessionRequired": false}',
    '{"idTokenTtl": 3600, "allowTokenExchange": false, "corsAllowedOrigins": ["http://localhost:8081"], "rotateRefreshToken": true, "refreshTokenTtlInDays": 30, "alwaysIssueRefreshToken": true}',
    now()
) ON CONFLICT DO NOTHING;

-- API resource
INSERT INTO resources (tenant_id, id, name, indicator, is_default, access_token_ttl)
VALUES (
    'default',
    'cookmaid-api-demo',
    'Cookmaid API',
    'http://localhost:8081/api',
    false,
    3600
) ON CONFLICT DO NOTHING;

-- Permission scope on the API resource
INSERT INTO scopes (tenant_id, id, resource_id, name, description, created_at)
VALUES (
    'default',
    'cookmaid-api-all-demo',
    'cookmaid-api-demo',
    'all',
    'Full access to Cookmaid API',
    now()
) ON CONFLICT DO NOTHING;

-- Grant the scope to the demo application
INSERT INTO application_user_consent_resource_scopes (tenant_id, application_id, scope_id)
VALUES (
    'default',
    'cookmaid-demo',
    'cookmaid-api-all-demo'
) ON CONFLICT DO NOTHING;

-- Demo user for the Cookmaid app
-- Credentials: testuser / CookmaidTest2026!
INSERT INTO users (tenant_id, id, username, password_encrypted, password_encryption_method, name, is_suspended, created_at, updated_at, identities, custom_data, logto_config, mfa_verifications, profile)
VALUES (
    'default',
    'ck-test-demo',
    'testuser',
    '$argon2i$v=19$m=8192,t=8,p=1$FSkRk8nxL1ZxKK2F5eFl0w$B0G/05x8yeCs9bHh/Y6IFxPzMdidIhEpx62+IObamFU',
    'Argon2i',
    'Test User',
    false, now(), now(), '{}', '{}', '{}', '[]', '{}'
) ON CONFLICT DO NOTHING;

-- Enable passkey sign-in, WebAuthn MFA (user-controlled)
UPDATE sign_in_experiences
SET passkey_sign_in = '{"enabled": true, "allowAutofill": true, "showPasskeyButton": true}',
    mfa = '{"policy": "UserControlled", "factors": ["WebAuthn"]}'
WHERE tenant_id = 'default';

-- Enable account center: username read-only, password + MFA (passkeys) editable
UPDATE account_centers
SET enabled = true,
    fields = '{"mfa": "Edit", "name": "Off", "email": "Off", "phone": "Off", "avatar": "Off", "social": "Off", "profile": "Off", "session": "Off", "password": "Edit", "username": "ReadOnly", "customData": "Off"}'
WHERE tenant_id = 'default';
