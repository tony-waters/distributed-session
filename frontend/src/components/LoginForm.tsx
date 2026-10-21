import { useState } from 'react';
import styles from './LoginForm.module.css';

// The seeded users (login-service has no registration, see ADR-0003), prefilled so the demo
// needs no setup and the credentials aren't something a reader has to go hunting for.
const SEEDED = [
  { username: 'alice', password: 'alice-password123' },
  { username: 'bob', password: 'bob-password123' },
];

type Props = {
  onSubmit: (username: string, password: string) => Promise<void>;
  busy: boolean;
  error: string | null;
};

export default function LoginForm({ onSubmit, busy, error }: Props) {
  const [username, setUsername] = useState(SEEDED[0].username);
  const [password, setPassword] = useState(SEEDED[0].password);

  return (
    <form
      className={styles.form}
      onSubmit={(event) => {
        event.preventDefault();
        void onSubmit(username, password);
      }}
    >
      <h2 className={styles.heading}>Log in</h2>
      <p className={styles.hint}>
        Logging in mints a brand-new Session in Redis and returns it as the{' '}
        <code>SESSION</code> cookie.
      </p>

      <label className={styles.field}>
        Username
        <input value={username} onChange={(event) => setUsername(event.target.value)} />
      </label>

      <label className={styles.field}>
        Password
        <input
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />
      </label>

      <div className={styles.actions}>
        <button type="submit" disabled={busy} className={styles.primary}>
          {busy ? 'Logging in…' : 'Log in'}
        </button>
        {SEEDED.map((user) => (
          <button
            key={user.username}
            type="button"
            disabled={busy}
            onClick={() => {
              setUsername(user.username);
              setPassword(user.password);
            }}
          >
            use {user.username}
          </button>
        ))}
      </div>

      {error !== null && <p className={styles.error}>{error}</p>}
    </form>
  );
}
