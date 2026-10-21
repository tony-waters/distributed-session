import type { LogEntry } from '../types';
import InstanceBadge from './InstanceBadge';
import styles from './RequestLog.module.css';

// The point of the whole demo: every call, newest first, labelled with the instance that
// answered it. Distinct instances down the "served by" column while the cart stays correct
// is the proof that the Session lives in Redis and not in any instance's memory.
export default function RequestLog({ entries }: { entries: LogEntry[] }) {
  const distinctInstances = new Set(
    entries.map((entry) => entry.instanceId).filter((id): id is string => id !== null),
  );

  return (
    <section className={styles.panel}>
      <header className={styles.header}>
        <h2 className={styles.heading}>Request log</h2>
        <span className={styles.count}>
          {entries.length} request{entries.length === 1 ? '' : 's'} ·{' '}
          {distinctInstances.size} instance{distinctInstances.size === 1 ? '' : 's'}
        </span>
      </header>

      {entries.length === 0 ? (
        <p className={styles.empty}>No requests yet.</p>
      ) : (
        <div className={styles.scroll}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>Time</th>
                <th>Request</th>
                <th>Status</th>
                <th>Served by</th>
              </tr>
            </thead>
            <tbody>
              {entries.map((entry) => (
                <tr key={entry.id}>
                  <td className={styles.time}>{entry.at}</td>
                  <td>
                    <code>
                      {entry.method} /api{entry.path}
                    </code>
                  </td>
                  <td
                    className={
                      entry.status !== null && entry.status < 400 ? styles.ok : styles.bad
                    }
                  >
                    {entry.status ?? 'failed'}
                  </td>
                  <td>
                    <InstanceBadge instanceId={entry.instanceId} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
