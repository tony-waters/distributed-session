# Authentication scope: Spring Security + seeded Postgres users, no registration

We use full Spring Security (`UserDetailsService` backed by Postgres, `BCryptPasswordEncoder`) for login, matching how a real Spring Boot service would authenticate, even though this is a JSON API with no form login. We deliberately did not add a `POST /register` endpoint or any account-management flow: `User` rows are seeded via migration at startup, and there is no way to create new ones through the API.

This keeps the surface area focused on the thing the demo is actually about — distributed sessions — while still exercising real credential checking (hashed passwords, generic 401 on any failure, no username enumeration) rather than a fake no-auth login.
