import type { Job } from '../../shared/types';
import {
  STATUS_LABELS,
  STATUS_ORDER,
} from '../../shared/types';

export default function AnalyticsBar({
  jobs,
}: {
  jobs: Job[];
}) {
  const counts = STATUS_ORDER.reduce<
    Record<string, number>
  >((acc, s) => {
    acc[s] = jobs.filter(
      (j) => j.status === s
    ).length;

    return acc;
  }, {});

  return (
    <div
      style={{
        display: 'flex',
        gap: 12,
        flexWrap: 'wrap',
        padding: '10px 12px',
        background: 'white',
        border: '1px solid #e5e7eb',
        borderRadius: 8,
      }}
    >
      {STATUS_ORDER.map((s) => (
        <div key={s} style={{ fontSize: 13 }}>
          <span style={{ color: '#6b7280' }}>
            {STATUS_LABELS[s]}:{' '}
          </span>

          <span style={{ fontWeight: 700 }}>
            {counts[s]}
          </span>
        </div>
      ))}

      <div
        style={{
          marginLeft: 'auto',
          fontSize: 13,
          fontWeight: 700,
        }}
      >
        Total: {jobs.length}
      </div>
    </div>
  );
}