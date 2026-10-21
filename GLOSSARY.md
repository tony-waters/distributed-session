# Distributed Session Demo

A demo of how session state survives across stateless, horizontally-scaled service instances by living in Redis instead of local memory.

## Language

**User**:
A durable account: a username and a hashed password, persisted in Postgres. The source of truth for identity — not stored in Redis.
_Avoid_: Account

**Session**:
An ephemeral record created when a User logs in, identified by a server-generated session ID (not the username) and backed by Redis via Spring Session. Holds the logged-in username and the Cart. Expires independently of the User it was created for.
_Avoid_: Token (the session is cookie-based, not a bearer token)

**Login**:
The act of authenticating a User's credentials and minting a brand-new Session. Always creates a fresh Session — never looks up or reuses an existing Session for that User.

**Cart**:
A list of item names held inside a Session. No product catalog, prices, or inventory — an item is just a string.
