# Use Spring Session Data Redis for distributed sessions

We considered hand-rolling the session mechanism (explicit session-ID generation, explicit `RedisTemplate` reads/writes, explicit cookie handling) so the distributed-session mechanism would be directly visible in the code, versus using Spring Session Data Redis, which backs `HttpSession` with Redis transparently. We chose Spring Session: it's the idiomatic, production-realistic way Spring Boot services share session state across instances, and the demo's point (state survives instance changes because it lives in Redis) is just as visible with it as without.

A consequence: the session ID is a Spring Session-generated random value delivered via the `SESSION` cookie, not derived from the username — so re-logging in as the same user without presenting that cookie always mints a brand-new session rather than reattaching to an existing one.
