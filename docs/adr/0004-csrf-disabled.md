# CSRF protection disabled

**Status: amended.** The decision stands; the reasoning below does not. Its "no browser frontend"
premise was overtaken by [ADR-0005](0005-same-origin-react-frontend.md) — read the amendment at the
foot of this record, and ADR-0005 itself, for the grounds CSRF is actually disabled on today. The
original reasoning is kept unedited as the record of what was decided and why at the time.

Spring Security enables CSRF protection by default, which matters when cookie-based session auth is paired with a browser-based frontend capable of making cross-origin form submissions. This demo has no such frontend — only `curl` and k6 drive the services — so the attack vector CSRF protection exists for doesn't apply here.

We explicitly disabled CSRF in the Spring Security config with a comment stating this rationale, rather than leaving it on by default inertia or disabling it silently. If a browser-based frontend is ever added, this decision should be revisited.

**Superseded in part by [ADR-0005](0005-same-origin-react-frontend.md):** a React frontend now exists, so the "no browser frontend" premise above no longer holds. The decision itself stands, on the narrower grounds that the frontend is served same-origin and the `SESSION` cookie's `SameSite=Lax` blocks the cross-site POSTs CSRF tokens guard against. See ADR-0005 for the reasoning and its limits.
