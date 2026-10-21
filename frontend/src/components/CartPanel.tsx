import { useState } from 'react';
import styles from './CartPanel.module.css';

// Same items k6/cross-instance-proof.js uses. An item is just a string - there is no
// catalog, no price, no inventory (GLOSSARY.md: Cart).
const QUICK_ITEMS = ['apple', 'banana', 'cherry', 'date'];

type Props = {
  username: string;
  cart: string[];
  busy: boolean;
  error: string | null;
  onAdd: (item: string) => Promise<void>;
  onHammer: () => Promise<void>;
  onRefresh: () => Promise<void>;
  onLogout: () => Promise<void>;
};

export default function CartPanel({
  username,
  cart,
  busy,
  error,
  onAdd,
  onHammer,
  onRefresh,
  onLogout,
}: Props) {
  const [item, setItem] = useState('');

  return (
    <section className={styles.panel}>
      <header className={styles.header}>
        <h2 className={styles.heading}>
          Cart for <strong>{username}</strong>
        </h2>
        <button type="button" onClick={() => void onLogout()} disabled={busy}>
          Log out
        </button>
      </header>

      {cart.length === 0 ? (
        <p className={styles.empty}>Cart is empty.</p>
      ) : (
        <ol className={styles.items}>
          {cart.map((cartItem, index) => (
            // Index-keyed deliberately: the cart is an ordered Redis list that allows
            // duplicates, so the item name is not a unique key.
            <li key={`${index}-${cartItem}`}>{cartItem}</li>
          ))}
        </ol>
      )}

      <div className={styles.quick}>
        {QUICK_ITEMS.map((quickItem) => (
          <button
            key={quickItem}
            type="button"
            disabled={busy}
            onClick={() => void onAdd(quickItem)}
          >
            + {quickItem}
          </button>
        ))}
      </div>

      <form
        className={styles.custom}
        onSubmit={(event) => {
          event.preventDefault();
          if (item.trim().length === 0) {
            return;
          }
          void onAdd(item.trim()).then(() => setItem(''));
        }}
      >
        <input
          value={item}
          placeholder="anything else…"
          onChange={(event) => setItem(event.target.value)}
        />
        <button type="submit" disabled={busy || item.trim().length === 0}>
          Add
        </button>
      </form>

      <div className={styles.actions}>
        <button type="button" className={styles.primary} disabled={busy} onClick={() => void onHammer()}>
          Add 6 items in a row
        </button>
        <button type="button" disabled={busy} onClick={() => void onRefresh()}>
          Re-read cart
        </button>
      </div>
      <p className={styles.hint}>
        “Add 6 items in a row” fires six separate requests. nginx round-robins them across
        cart instances — watch the log.
      </p>

      {error !== null && <p className={styles.error}>{error}</p>}
    </section>
  );
}
