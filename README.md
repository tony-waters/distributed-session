# distributed-session

A demo proving that session state survives across stateless, horizontally-scaled service instances by living in Redis instead of local memory.

Two services, no sticky sessions:

- **login-service** — owns `User` accounts in Postgres (seeded, no registration), authenticates via Spring Security, and mints a Spring Session-backed `Session` in Redis.
- **cart-service** — holds no data of its own; trusts the shared Redis session only to authenticate the caller, then reads/appends the `Cart` in its own Redis key (keyed by session ID, expiring alongside it) via atomic `RPUSH` so concurrent adds from different instances can't clobber each other.

`docker compose up` runs 3 instances of each behind an nginx load balancer (no `ip_hash`/sticky-session directive — plain round-robin) on the stable entrypoints `localhost:8080` (login-service) and `localhost:8081` (cart-service). A session created on one instance is usable from any other, regardless of which instance a later request happens to hit — proven under concurrent load with a k6 script.

## Frontend demo

```
docker compose up --build
```

Then open **<http://localhost:3000>** and log in as `alice` / `alice-password123` (prefilled — users are seeded, there is no registration).

Every API call the page makes is listed in a request log alongside the instance that answered it, read from an `X-Instance-Id` response header. Click **Add 6 items in a row** and watch the *served by* column change while the cart accumulates correctly: consecutive requests for the same session are being handled by different `cart-service` instances, which is only possible because the Session and Cart live in Redis. Reload the page and the session is still there; log out and the next add is rejected with a 401 by whichever instance happens to receive it.

The page is a static React build served by nginx on port 3000, which also proxies `/api/*` to the services on the same origin — so the `SESSION` cookie needs no CORS handling (see `docs/adr/0005-same-origin-react-frontend.md`). The bare `8080`/`8081` entrypoints are unchanged, so `curl` and the k6 script still work exactly as before.

For frontend development against a running stack: `cd frontend && npm install && npm run dev`.

## Reference

See `GLOSSARY.md` for domain vocabulary, `docs/adr/` for the architecture decisions, and [issue #1](https://github.com/tony-waters/distributed-session/issues/1) for the full spec. Work is tracked as sub-issues of #1 in GitHub Issues.
