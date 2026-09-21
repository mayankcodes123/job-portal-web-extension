import { useDraggable } from '@dnd-kit/core';
import type { Job } from '../../shared/types';
import { isPast, isToday } from 'date-fns';

interface Props {
  job: Job;
  onClick?: (job: Job) => void;
}

export default function JobCard({ job, onClick }: Props) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    isDragging,
  } = useDraggable({
    id: job.id,
  });

  const style: React.CSSProperties = {
    transform: transform
      ? `translate3d(${transform.x}px, ${transform.y}px, 0)`
      : undefined,

    opacity: isDragging ? 0.5 : 1,
    background: 'white',
    border: '1px solid #e5e7eb',
    borderRadius: 6,
    padding: 10,
    marginBottom: 8,
    cursor: 'grab',
    boxShadow: '0 1px 2px rgba(0,0,0,0.04)',
  };

  const dueSoon =
    job.followUpDate &&
    (isToday(new Date(job.followUpDate)) ||
      isPast(new Date(job.followUpDate)));

  return (
    <div
      ref={setNodeRef}
      style={style}
      {...listeners}
      {...attributes}
      onClick={() => onClick?.(job)}
    >
      <div style={{ fontWeight: 600, fontSize: 14 }}>
        {job.title}
      </div>

      <div style={{ color: '#6b7280', fontSize: 13 }}>
        {job.company}
      </div>

      {job.location && (
        <div style={{ color: '#9ca3af', fontSize: 12 }}>
          {job.location}
        </div>
      )}

      {dueSoon && (
        <div
          style={{
            marginTop: 6,
            fontSize: 11,
            color: '#b91c1c',
            fontWeight: 600,
          }}
        >
          Follow-up: {job.followUpDate}
        </div>
      )}
    </div>
  );
}