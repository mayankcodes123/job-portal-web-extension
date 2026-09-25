import { useState } from 'react';

import type {
  Job,
  JobStatus,
} from '../shared/types';

import { useJobs } from './hooks/useJobs';

import AddJobForm from './components/AddJobForm';
import KanbanBoard from './components/KanbanBoard';
import AnalyticsBar from './components/AnalyticsBar';
import FollowUpsPanel from './components/FollowUpsPanel';
import NotesEditor from './components/NotesEditor';

export default function Dashboard() {
  const {
    jobs,
    loading,
    add,
    update,
    remove,
  } = useJobs();

  const [selected, setSelected] =
    useState<Job | null>(null);

  function handleMove(
    jobId: string,
    newStatus: JobStatus
  ) {
    const patch: Partial<Job> = {
      status: newStatus,
    };

    // Auto-fill applied date the first time
    // a job moves to "applied"
    if (newStatus === 'applied') {
      const job = jobs.find(
        (j) => j.id === jobId
      );

      if (job && !job.appliedDate) {
        patch.appliedDate =
          new Date()
            .toISOString()
            .slice(0, 10);
      }
    }

    update(jobId, patch);
  }

  function handleSaveNotes(
    job: Job,
    patch: Partial<Job>
  ) {
    update(job.id, patch);

    setSelected((s) =>
      s
        ? {
            ...s,
            ...patch,
          }
        : s
    );
  }

  async function handleDelete(id: string) {
    await remove(id);
    setSelected(null);
  }

  if (loading) {
    return (
      <div style={{ padding: 20 }}>
        Loading…
      </div>
    );
  }

  return (
    <div
      style={{
        padding: 20,
        fontFamily:
          'system-ui, sans-serif',
        maxWidth: 1200,
        margin: '0 auto',
        background: '#f3f4f6',
        minHeight: '100vh',
      }}
    >
      <h1 style={{ marginTop: 0 }}>
        Job Application CRM
      </h1>

      <div
        style={{
          display: 'grid',
          gap: 14,
          marginBottom: 16,
        }}
      >
        <AnalyticsBar jobs={jobs} />

        <FollowUpsPanel
          jobs={jobs}
          onCardClick={setSelected}
        />

        <AddJobForm onAdd={add} />
      </div>

      <KanbanBoard
        jobs={jobs}
        onMove={handleMove}
        onCardClick={setSelected}
      />

      {selected && (
        <div
          onClick={() => setSelected(null)}
          style={{
            position: 'fixed',
            inset: 0,
            background:
              'rgba(0,0,0,0.35)',
            display: 'flex',
            justifyContent: 'flex-end',
            zIndex: 50,
          }}
        >
          <div
            onClick={(e) =>
              e.stopPropagation()
            }
            style={{
              background: 'white',
              width: 420,
              height: '100%',
              padding: 20,
              overflowY: 'auto',
            }}
          >
            <button
              onClick={() =>
                setSelected(null)
              }
              style={{ float: 'right' }}
            >
              ✕
            </button>

            <h2 style={{ marginTop: 0 }}>
              {selected.title}
            </h2>

            <p
              style={{
                color: '#6b7280',
                marginTop: 0,
              }}
            >
              {selected.company}

              {selected.location
                ? ` · ${selected.location}`
                : ''}
            </p>

            {selected.jobUrl && (
              <p>
                <a
                  href={selected.jobUrl}
                  target="_blank"
                  rel="noreferrer"
                >
                  Open job posting
                </a>
              </p>
            )}

            <hr />

            <NotesEditor
              job={selected}
              onSave={(patch) =>
                handleSaveNotes(
                  selected,
                  patch
                )
              }
              onDelete={handleDelete}
            />
          </div>
        </div>
      )}
    </div>
  );
}