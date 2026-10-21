# Distributed Session Demo

A demo of how session state survives across stateless, horizontally-scaled service instances by living in Redis instead of local memory.

## Language

**User**:
A durable account: a username and a hashed password, persisted in Postgres. The source of truth for identity — not stored in Redis.
_Avoid_: Account

**Session**:
An ephemeral record created when a User logs in, identified by a server-generated session ID (not the username) and backed by Redis via Spring Session. Holds the logged-in username. Expires independently of the User it was created for.
_Avoid_: Token (the session is cookie-based, not a bearer token)

**Login**:
The act of authenticating a User's credentials and minting a brand-new Session. Always creates a fresh Session — never looks up or reuses an existing Session for that User.

**Cart**:
A list of item names scoped to a Session's ID, stored in its own Redis key (not a Session attribute) so items can be appended atomically. No product catalog, prices, or inventory — an item is just a string.

**Instance**:
One running process of a service. Instances of the same service are interchangeable: any of them can serve any request for any Session, because they hold no state of their own. Each reports its identity in the `X-Instance-Id` response header purely so the demo can show which one answered — never to route to it or to key state by it.
_Avoid_: Node, Server (both read as a machine rather than a replaceable process)
