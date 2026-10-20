I want to demo how a distributed session would work in a microservices environment

- use Redis to keep track of the sessions
- use Spring / Spring boot

## Domain

Keep the domain model minimal so the demo stays focused on session management, not business logic.

- A session just holds a `username` and a `cart` (`List<String>` of item names) — no product catalog, prices, or inventory.
- **Login service** — `POST /login {username}` → creates a session, stores `username` in Redis.
- **Cart service** — `POST /cart/add {item}` and `GET /cart` → reads the session cookie/token, appends/reads the item list in Redis.

Demo it by logging in via one service instance, then adding/viewing cart items via a different (or restarted) instance — the cart still resolves correctly because state lives in Redis, not local memory.
