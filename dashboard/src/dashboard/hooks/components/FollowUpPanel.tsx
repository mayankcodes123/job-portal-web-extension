import {
  isPast,
  isToday,
  parseISO,
} from 'date-fns';

import type { Job } from '../../shared/types';

interface Props {
  jobs: Job[];
  onCardClick?: (job: Job) => void;
}

export default function FollowUpsPanel({
  jobs,
  onCardClick,
}: Props) {
  const due = jobs
    .filter((j) => j.followUpDate)
    .filter((j) => {
      const d = parseISO(j.followUpDate!);

      return isToday(d) || isPast(d);
    })
    .sort((a, b) =>
      a.followUpDate! < b.followUpDate! ? -1 : 1
    );

  if (due.length === 0) {
    return null;
  }

  return (
    <div
      style={{
        border: '1px solid #fecaca',
        background: '#fef2f2',
        borderRadius: 8,
        padding: 12,
      }}
    >
      <div
        style={{
          fontWeight: 700,
          color: '#b91c1c',
          fontSize: 13,
          marginBottom: 6,
        }}
      >
        Follow-ups due ({due.length})
      </div>

      <ul
        style={{
          margin: 0,
          paddingLeft: 18,
          fontSize: 13,
        }}
      >
        {due.map((j) => (
          <li
            key={j.id}
            style={{
              cursor: onCardClick
                ? 'pointer'
                : 'default',
            }}
            onClick={() => onCardClick?.(j)}
          >
            <strong>{j.title}</strong> — {j.company}{' '}

            <span style={{ color: '#6b7280' }}>
              ({j.followUpDate})
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}