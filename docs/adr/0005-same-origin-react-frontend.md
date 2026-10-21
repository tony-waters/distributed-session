# React frontend served same-origin behind the load balancer

The demo's proof lived only in `k6/cross-instance-proof.js` and the integration tests: both convincing, neither something a person can look at. We added a React frontend whose job is to make the proof visible — it shows which instance answered each request, so a reader watches calls bounce across instances while the Session and Cart stay intact.

## Served by nginx, not by a separate origin

The frontend is a static Vite build in its own container, and the existing nginx exposes it on `localhost:3000`, proxying `/api/*` from that same port to the `login_service` and `cart_service` upstreams (stripping the `/api` prefix, which the services themselves never see). The alternative — running the frontend on its own origin and adding CORS to both services — was rejected:

- Cookie-based session auth across origins needs `credentials: 'include'`, an explicit CORS allowlist, and `SameSite=None; Secure` on the `SESSION` cookie. That is three changes to production-shaped security config made solely to accommodate a dev-server layout.
- Sharing one origin means the `SESSION` cookie is first-party and simply works, exactly as it already does for `curl` and k6.
- The services stay unaware that a frontend exists. No CORS config, no new Spring Security rules.

`npm run dev` reproduces the same routing with Vite's dev-server proxy, so the app issues identical `/api/*` calls in both modes and neither needs a code path of its own.

The existing bare entrypoints on `8080` and `8081` are untouched, so the k6 proof and the documented `curl` recipes keep working unchanged.

## ADR-0004 (CSRF disabled) still holds

ADR-0004 disabled CSRF protection on the grounds that the demo had no browser frontend, and said the decision should be revisited if one was added. Revisited: it stands, and for a reason beyond "it's a demo".

The `SESSION` cookie is `SameSite=Lax` (Spring Session's default, unchanged here). A cross-site `POST` — the attack CSRF tokens exist to stop — does not carry a `Lax` cookie, so a forged request from an attacker's page arrives unauthenticated and is rejected by the ordinary 401 path. The frontend's own requests are same-origin and unaffected.

This is not a claim that `SameSite` alone is sufficient defence for every application; older browsers and same-site-but-untrusted subdomains are real gaps. It is a claim that this demo's threat model — a locally-run, seeded-user teaching stack with no real data — does not justify adding token plumbing that would obscure what the demo is actually about. A production service with the same auth design should turn CSRF protection back on.

## Instance identity is a label, never an input

Both services stamp `X-Instance-Id` on every response via `InstanceIdFilter` in `session-contract`, fed by a per-instance `INSTANCE_ID` environment variable in `docker-compose.yml`. The frontend reads that header and nothing else to populate its request log.

Deliberately one-directional: nothing routes on the header, no state is keyed by it, and no request ever asks for a particular instance. The instances stay interchangeable — a frontend that could pin one would be demonstrating the opposite of the point. The header exists so a reader can *see* the indifference, not to create a way to depend on it.

Adding `GET /session` to login-service follows from the same constraint: the cookie is `HttpOnly`, so after a page reload the frontend cannot tell who it is logged in as without asking a service, and whichever instance answers reads the username from shared Redis.
