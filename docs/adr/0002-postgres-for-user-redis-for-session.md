# Split persistence: User in Postgres, Session only in Redis

The original idea used Redis for everything, with no persistent `User` entity. To make login "production-like," we added a real `User` (username + hashed password) that needs to survive restarts and isn't naturally a fit for Redis's role as an ephemeral session store. We chose to persist `User` in Postgres, owned exclusively by `login-service`, and kept Redis scoped to what it's actually for in this demo: ephemeral `Session` data shared across instances.

`cart-service` has no Postgres access at all — it only ever reads the Redis-backed session to find the already-authenticated username, reinforcing that each service owns its own data.
