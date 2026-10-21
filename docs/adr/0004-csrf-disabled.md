# CSRF protection disabled

Spring Security enables CSRF protection by default, which matters when cookie-based session auth is paired with a browser-based frontend capable of making cross-origin form submissions. This demo has no such frontend — only `curl` and k6 drive the services — so the attack vector CSRF protection exists for doesn't apply here.

We explicitly disabled CSRF in the Spring Security config with a comment stating this rationale, rather than leaving it on by default inertia or disabling it silently. If a browser-based frontend is ever added, this decision should be revisited.
