-- Login accounts for LineBook.
--
-- One row per person who can sign in. A row with role = 'AGENT' is linked to an agents row, so the
-- app knows which agent is collecting; a row with role = 'ADMIN' has agent_id NULL and can manage
-- everything (agents, customers, billing).
--
-- Passwords are never stored in plain text: password_hash holds "iterations:salt:hash" produced by
-- PBKDF2WithHmacSHA256 (see PasswordHasher.java).

CREATE TABLE app_users (
    id             VARCHAR(64)  NOT NULL,                 -- us-0001, us-0002, ...
    email          VARCHAR(190) NOT NULL,                 -- the login id, always stored lower-case
    full_name      VARCHAR(150) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    role           VARCHAR(16)  NOT NULL,                 -- ADMIN | AGENT
    agent_id       VARCHAR(64)  NULL,                     -- set for role = AGENT
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at  DATETIME     NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_users_email (email),
    UNIQUE KEY uk_app_users_agent (agent_id),
    KEY idx_app_users_role (role),
    CONSTRAINT fk_app_users_agent FOREIGN KEY (agent_id) REFERENCES agents (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
