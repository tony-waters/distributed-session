import styles from './InstanceBadge.module.css';

// Distinct hues for the six instances the stack runs, derived from the id so the same
// instance always reads as the same colour down the log - that repetition and change is
// what makes round-robin visible at a glance.
const HUES = [205, 145, 35, 275, 0, 180];

function hueFor(instanceId: string): number {
  let hash = 0;
  for (const char of instanceId) {
    hash = (hash * 31 + char.charCodeAt(0)) | 0;
  }
  return HUES[Math.abs(hash) % HUES.length];
}

export default function InstanceBadge({ instanceId }: { instanceId: string | null }) {
  if (instanceId === null) {
    return <span className={`${styles.badge} ${styles.unknown}`}>no header</span>;
  }

  return (
    <span className={styles.badge} style={{ color: `hsl(${hueFor(instanceId)} 60% 52%)` }}>
      {instanceId}
    </span>
  );
}
