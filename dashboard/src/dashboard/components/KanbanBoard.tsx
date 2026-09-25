import {
  DndContext,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
} from '@dnd-kit/core';

import type { Job, JobStatus } from '../../shared/types';
import { STATUS_ORDER } from '../../shared/types';
import KanbanColumn from './KanbanColumn';

interface Props {
  jobs: Job[];
  onMove: (jobId: string, newStatus: JobStatus) => void;
  onCardClick?: (job: Job) => void;
}

export default function KanbanBoard({
  jobs,
  onMove,
  onCardClick,
}: Props) {
  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 5,
      },
    })
  );

  function handleDragEnd(event: DragEndEvent) {
    const { active, over } = event;

    if (!over) return;

    const jobId = String(active.id);
    const newStatus = over.id as JobStatus;

    const job = jobs.find((j) => j.id === jobId);

    if (!job || job.status === newStatus) {
      return;
    }

    onMove(jobId, newStatus);
  }

  return (
    <DndContext
      sensors={sensors}
      onDragEnd={handleDragEnd}
    >
      <div
        style={{
          display: 'flex',
          gap: 12,
          overflowX: 'auto',
          paddingBottom: 8,
          alignItems: 'flex-start',
        }}
      >
        {STATUS_ORDER.map((status) => (
          <KanbanColumn
            key={status}
            status={status}
            jobs={jobs.filter(
              (j) => j.status === status
            )}
            onCardClick={onCardClick}
          />
        ))}
      </div>
    </DndContext>
  );
}