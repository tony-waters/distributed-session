import { useEffect, useRef, useState } from 'react';
import styles from './App.module.css';
import { api, onRequest, type ErrorBody, type SessionBody } from './api';
import CartPanel from './components/CartPanel';
import LoginForm from './components/LoginForm';
import RequestLog from './components/RequestLog';
import type { Cart, LogEntry } from './types';

const HAMMER_ITEMS = ['apple', 'banana', 'cherry', 'date', 'apple', 'elderberry'];

function errorMessage(body: unknown, fallback: string): string {
  if (typeof body === 'object' && body !== null && 'error' in body) {
    return String((body as ErrorBody).error);
  }
  return fallback;
}

export default function App() {
  const [username, setUsername] = useState<string | null>(null);
  const [cart, setCart] = useState<Cart>([]);
  const [log, setLog] = useState<LogEntry[]>([]);
  const [busy, setBusy] = useState(false);
  const [restoring, setRestoring] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const nextLogId = useRef(0);

  useEffect(() => {
    // Newest first, so the instance that answered the most recent call is the top row.
    onRequest((entry) =>
      setLog((previous) => [{ ...entry, id: nextLogId.current++ }, ...previous]),
    );

    // The SESSION cookie is HttpOnly, so a reload can't tell from JavaScript whether it is
    // still logged in - GET /session asks a login-service instance, which answers from the
    // shared Redis session rather than from any local state.
    void (async () => {
      const result = await api.session();
      if (result.status === 200 && result.body !== null) {
        setUsername((result.body as SessionBody).username);
        const cartResult = await api.viewCart();
        if (cartResult.status === 200 && cartResult.body !== null) {
          setCart(cartResult.body as Cart);
        }
      }
      setRestoring(false);
    })();
  }, []);

  // Any 401 from a cart call means the Session is gone cluster-wide (logout elsewhere, or
  // Redis TTL expiry), so drop back to the login form rather than showing a stale cart.
  function handleUnauthorized(): void {
    setUsername(null);
    setCart([]);
    setError('Session is no longer valid — every instance rejected it. Log in again.');
  }

  async function withBusy(work: () => Promise<void>): Promise<void> {
    setBusy(true);
    setError(null);
    try {
      await work();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Request failed');
    } finally {
      setBusy(false);
    }
  }

  const handleLogin = (user: string, password: string) =>
    withBusy(async () => {
      const result = await api.login(user, password);
      if (result.status !== 200) {
        setError(errorMessage(result.body, 'Login failed'));
        return;
      }
      setUsername(user);
      const cartResult = await api.viewCart();
      setCart(cartResult.status === 200 ? ((cartResult.body ?? []) as Cart) : []);
    });

  const handleLogout = () =>
    withBusy(async () => {
      await api.logout();
      setUsername(null);
      setCart([]);
    });

  // POST /cart/add already returns the full updated cart, so there is nothing to re-fetch -
  // and that returned array is what proves the append landed in shared state.
  async function addOne(item: string): Promise<void> {
    const result = await api.addItem(item);
    if (result.status === 401) {
      handleUnauthorized();
      return;
    }
    if (result.status !== 200) {
      setError(errorMessage(result.body, `Could not add “${item}”`));
      return;
    }
    setCart((result.body ?? []) as Cart);
  }

  const handleAdd = (item: string) => withBusy(() => addOne(item));

  const handleHammer = () =>
    withBusy(async () => {
      // Sequential, not Promise.all: one request per round-robin turn makes the instance
      // column alternate predictably, and keeps the returned carts in a readable order.
      for (const item of HAMMER_ITEMS) {
        await addOne(item);
      }
    });

  const handleRefresh = () =>
    withBusy(async () => {
      const result = await api.viewCart();
      if (result.status === 401) {
        handleUnauthorized();
        return;
      }
      setCart((result.body ?? []) as Cart);
    });

  return (
    <div className={styles.page}>
      <header className={styles.intro}>
        <h1 className={styles.title}>Distributed session demo</h1>
        <p className={styles.lede}>
          Three <code>login-service</code> instances and three <code>cart-service</code>{' '}
          instances sit behind nginx with <strong>no sticky sessions</strong>. Every request
          below may land on a different instance, yet the session and cart stay intact —
          because they live in Redis, not in any instance&rsquo;s memory.
        </p>
      </header>

      <main className={styles.columns}>
        <div className={styles.card}>
          {restoring ? (
            <p className={styles.restoring}>Checking for an existing session…</p>
          ) : username === null ? (
            <LoginForm onSubmit={handleLogin} busy={busy} error={error} />
          ) : (
            <CartPanel
              username={username}
              cart={cart}
              busy={busy}
              error={error}
              onAdd={handleAdd}
              onHammer={handleHammer}
              onRefresh={handleRefresh}
              onLogout={handleLogout}
            />
          )}
        </div>

        <div className={styles.card}>
          <RequestLog entries={log} />
        </div>
      </main>
    </div>
  );
}
