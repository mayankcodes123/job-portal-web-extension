import { useDroppable } from '@dnd-kit/core';
import type { Job, JobStatus } from '../../shared/types';
import { STATUS_LABELS } from '../../shared/types';
import JobCard from './JobCard';

interface Props {
  status: JobStatus;
  jobs: Job[];
  onCardClick?: (job: Job) => void;
}

export default function KanbanColumn({
  status,
  jobs,
  onCardClick,
}: Props) {
  const { setNodeRef, isOver } = useDroppable({
    id: status,
  });

  return (
    <div
      ref={setNodeRef}
      style={{
        background: isOver ? '#eff6ff' : '#f9fafb',
        border: '1px solid #e5e7eb',
        borderRadius: 8,
        padding: 10,
        minWidth: 220,
        flex: '0 0 220px',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          marginBottom: 8,
          fontSize: 13,
          fontWeight: 700,
          color: '#374151',
        }}
      >
        <span>{STATUS_LABELS[status]}</span>

        <span style={{ color: '#6b7280' }}>
          {jobs.length}
        </span>
      </div>

      <div style={{ flex: 1, minHeight: 40 }}>
        {jobs.map((job) => (
          <JobCard
            key={job.id}
            job={job}
            onClick={onCardClick}
          />
        ))}
      </div>
    </div>
  );
}