// Acceptance-level proof for ticket 6 (issue #6): drives the full docker-compose
// stack through the nginx load-balancer entrypoints only (localhost:8080 for
// login-service, localhost:8081 for cart-service) and asserts that session state
// propagates correctly across instances under concurrent, instance-agnostic
// traffic. The script never inspects or pins which instance answers a call - that
// is the entire point of the proof.
//
// This is a separate, on-demand acceptance step against the full compose stack.
// It is not part of the Testcontainers-based test suites in login-service/cart-service
// (tickets 2/3) and does not run as part of `./gradlew test`. See k6/README.md.
import http from 'k6/http';
import { check } from 'k6';

const LOGIN_BASE_URL = __ENV.LOGIN_BASE_URL || 'http://localhost:8080';
const CART_BASE_URL = __ENV.CART_BASE_URL || 'http://localhost:8081';
const VUS = __ENV.VUS !== undefined ? Number(__ENV.VUS) : 10;

// Only two Users are seeded (ADR-0003, V2__seed_users.sql); VUs share them, which
// is fine because Login always mints a brand-new Session regardless of how many
// other Sessions already exist for that username.
const USERS = [
  { username: 'alice', password: 'alice-password123' },
  { username: 'bob', password: 'bob-password123' },
];

const ITEMS = ['apple', 'banana', 'cherry', 'date'];

const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } };

export const options = {
  scenarios: {
    cross_instance_proof: {
      executor: 'per-vu-iterations',
      vus: VUS,
      iterations: 1,
      maxDuration: '30s',
    },
  },
  thresholds: {
    checks: ['rate==1.0'],
  },
};

export default function () {
  const user = USERS[__VU % USERS.length];

  const loginRes = http.post(
    `${LOGIN_BASE_URL}/login`,
    JSON.stringify({ username: user.username, password: user.password }),
    JSON_HEADERS,
  );
  check(loginRes, { 'login succeeds': (r) => r.status === 200 });

  const freshCartRes = http.get(`${CART_BASE_URL}/cart`);
  check(freshCartRes, {
    'fresh session cart is empty': (r) => JSON.stringify(r.json()) === '[]',
  });

  // Several rapid cart/add calls, each landing on whichever cart-service instance
  // nginx round-robins to - the cart must still accumulate correctly because it
  // lives in the shared Redis session, not in any one instance's memory.
  let expectedCart = [];
  for (const item of ITEMS) {
    expectedCart = expectedCart.concat(item);
    const addRes = http.post(`${CART_BASE_URL}/cart/add`, JSON.stringify({ item }), JSON_HEADERS);
    check(addRes, {
      'cart/add succeeds': (r) => r.status === 200,
      'cart/add reflects accumulated items': (r) => JSON.stringify(r.json()) === JSON.stringify(expectedCart),
    });
  }

  const viewRes = http.get(`${CART_BASE_URL}/cart`);
  check(viewRes, {
    'cart view matches accumulated items': (r) => JSON.stringify(r.json()) === JSON.stringify(expectedCart),
  });

  const logoutRes = http.post(`${LOGIN_BASE_URL}/logout`, null);
  check(logoutRes, { 'logout succeeds': (r) => r.status === 200 });

  // Logout invalidation must be visible instantly cluster-wide, regardless of
  // which cart-service instance answers this retry.
  const retryRes = http.post(`${CART_BASE_URL}/cart/add`, JSON.stringify({ item: 'post-logout-retry' }), JSON_HEADERS);
  check(retryRes, { 'cart/add after logout is rejected with 401': (r) => r.status === 401 });
}
