CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    oidc_subject TEXT NOT NULL UNIQUE
);
