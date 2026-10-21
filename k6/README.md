# k6 cross-instance proof

`cross-instance-proof.js` is the automated, repeatable, concurrent-load proof
that the distributed-session property holds under real traffic. It is a
separate, on-demand acceptance step against the full `docker-compose` stack -
it does **not** run as part of `./gradlew test` (the Testcontainers-based
integration tests for `login-service` and `cart-service`), and the stack must
already be up.

## Running it

```sh
docker-compose up -d --build
k6 run k6/cross-instance-proof.js
```

Or, without a local k6 install:

```sh
docker run --rm -i --network host grafana/k6:latest run - < k6/cross-instance-proof.js
```

Override the entrypoints or virtual-user count with environment variables:

```sh
k6 run -e VUS=50 -e LOGIN_BASE_URL=http://localhost:8080 -e CART_BASE_URL=http://localhost:8081 k6/cross-instance-proof.js
```

## What it proves

Per virtual user, through the nginx load-balancer entrypoints only (the script
never inspects or pins which backing instance answers a call):

1. Login creates a fresh, empty-cart session.
2. Several rapid `cart/add` calls accumulate correctly, proving cart state
   propagates via the shared Redis session rather than living in any one
   instance's memory.
3. `GET /cart` reflects the same accumulated items.
4. Logout invalidates the session.
5. A `cart/add` retry after logout returns 401 immediately, proving
   invalidation is visible cluster-wide regardless of which instance answers.
