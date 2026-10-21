-- No registration endpoint exists (ADR-0003): accounts are seeded here at startup.
-- pgcrypto's crypt()/gen_salt('bs') produces standard bcrypt hashes, verifiable by
-- Spring Security's BCryptPasswordEncoder without any extra conversion.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (username, password_hash) VALUES
    ('alice', crypt('alice-password123', gen_salt('bf'))),
    ('bob', crypt('bob-password123', gen_salt('bf')));
