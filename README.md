# distributed-session

A demo proving that session state survives across stateless, horizontally-scaled service instances by living in Redis instead of local memory.

Two services, no sticky sessions:

- **login-service** — owns `User` accounts in Postgres (seeded, no registration), authenticates via Spring Security, and mints a Spring Session-backed `Session` in Redis.
- **cart-service** — holds no data of its own; trusts only the shared Redis session to read/append the `Cart`.

Run N instances of each behind a load balancer with no session affinity, and a session created on one instance is usable from any other — proven under concurrent load with a k6 script.

See `GLOSSARY.md` for domain vocabulary, `docs/adr/` for the architecture decisions, and [issue #1](https://github.com/tony-waters/distributed-session/issues/1) for the full spec. Work is tracked as sub-issues of #1 in GitHub Issues.
