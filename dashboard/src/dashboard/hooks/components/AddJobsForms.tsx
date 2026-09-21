import { useState } from 'react';
import type { Job, JobSource, JobStatus } from '../../shared/types';
import { STATUS_LABELS, STATUS_ORDER } from '../../shared/types';

type NewJob = Omit<Job, 'id' | 'createdAt' | 'updatedAt'>;

const EMPTY: NewJob = {
  title: '',
  company: '',
  location: '',
  jobUrl: '',
  source: 'manual',
  status: 'saved',
  salary: '',
  experienceRequired: '',
  notes: '',
  appliedDate: undefined,
  followUpDate: undefined,
};

interface Props {
  onAdd: (job: NewJob) => void | Promise<unknown>;
}

export default function AddJobForm({ onAdd }: Props) {
  const [form, setForm] = useState<NewJob>(EMPTY);
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);

  function set<K extends keyof NewJob>(
    key: K,
    value: NewJob[K]
  ) {
    setForm((f) => ({
      ...f,
      [key]: value,
    }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    if (!form.title.trim() || !form.company.trim()) {
      return;
    }

    setSaving(true);

    try {
      await onAdd({
        ...form,
        title: form.title.trim(),
        company: form.company.trim(),

        // drop empty optional dates
        appliedDate: form.appliedDate || undefined,
        followUpDate: form.followUpDate || undefined,
      });

      setForm(EMPTY);
      setOpen(false);
    } finally {
      setSaving(false);
    }
  }

  if (!open) {
    return (
      <button
        onClick={() => setOpen(true)}
        style={{
          padding: '8px 16px',
          background: '#2563eb',
          color: 'white',
          border: 'none',
          borderRadius: 6,
          cursor: 'pointer',
          fontWeight: 600,
        }}
      >
        + Add Job
      </button>
    );
  }

  return (
    <form
      onSubmit={handleSubmit}
      style={{
        border: '1px solid #e5e7eb',
        borderRadius: 8,
        padding: 16,
        background: 'white',
        display: 'grid',
        gap: 10,
        maxWidth: 640,
      }}
    >
      <h3 style={{ margin: 0 }}>Add Job</h3>

      <input
        placeholder="Job title *"
        value={form.title}
        onChange={(e) => set('title', e.target.value)}
        required
      />

      <input
        placeholder="Company *"
        value={form.company}
        onChange={(e) => set('company', e.target.value)}
        required
      />

      <input
        placeholder="Location"
        value={form.location}
        onChange={(e) => set('location', e.target.value)}
      />

      <input
        placeholder="Job URL"
        value={form.jobUrl}
        onChange={(e) => set('jobUrl', e.target.value)}
      />

      <div style={{ display: 'flex', gap: 10 }}>
        <label style={{ flex: 1 }}>
          Source

          <select
            value={form.source}
            onChange={(e) =>
              set('source', e.target.value as JobSource)
            }
            style={{ width: '100%' }}
          >
            <option value="manual">Manual</option>
            <option value="naukri">Naukri</option>
            <option value="wellfound">Wellfound</option>
            <option value="linkedin">LinkedIn</option>
          </select>
        </label>

        <label style={{ flex: 1 }}>
          Status

          <select
            value={form.status}
            onChange={(e) =>
              set('status', e.target.value as JobStatus)
            }
            style={{ width: '100%' }}
          >
            {STATUS_ORDER.map((s) => (
              <option key={s} value={s}>
                {STATUS_LABELS[s]}
              </option>
            ))}
          </select>
        </label>
      </div>

      <div style={{ display: 'flex', gap: 10 }}>
        <input
          placeholder="Salary"
          value={form.salary}
          onChange={(e) => set('salary', e.target.value)}
          style={{ flex: 1 }}
        />

        <input
          placeholder="Experience required"
          value={form.experienceRequired}
          onChange={(e) =>
            set('experienceRequired', e.target.value)
          }
          style={{ flex: 1 }}
        />
      </div>

      <div style={{ display: 'flex', gap: 10 }}>
        <label style={{ flex: 1 }}>
          Applied date

          <input
            type="date"
            value={form.appliedDate ?? ''}
            onChange={(e) =>
              set('appliedDate', e.target.value)
            }
            style={{ width: '100%' }}
          />
        </label>

        <label style={{ flex: 1 }}>
          Follow-up date

          <input
            type="date"
            value={form.followUpDate ?? ''}
            onChange={(e) =>
              set('followUpDate', e.target.value)
            }
            style={{ width: '100%' }}
          />
        </label>
      </div>

      <textarea
        placeholder="Notes"
        value={form.notes}
        onChange={(e) => set('notes', e.target.value)}
        rows={3}
      />

      <div style={{ display: 'flex', gap: 8 }}>
        <button type="submit" disabled={saving}>
          {saving ? 'Saving…' : 'Save Job'}
        </button>

        <button
          type="button"
          onClick={() => {
            setForm(EMPTY);
            setOpen(false);
          }}
        >
          Cancel
        </button>
      </div>
    </form>
  );
}