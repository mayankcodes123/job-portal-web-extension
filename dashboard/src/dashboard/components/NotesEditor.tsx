import { useState } from 'react';
import type { Job } from '../../shared/types';

interface Props {
  job: Job;
  onSave: (patch: Partial<Job>) => void;
  onDelete?: (id: string) => void;
}

export default function NotesEditor({
  job,
  onSave,
  onDelete,
}: Props) {
  const [notes, setNotes] = useState(job.notes);
  const [followUpDate, setFollowUpDate] =
    useState(job.followUpDate ?? '');

  const [dirty, setDirty] = useState(false);

  function save() {
    onSave({
      notes,
      followUpDate: followUpDate || undefined,
    });

    setDirty(false);
  }

  return (
    <div style={{ display: 'grid', gap: 10 }}>
      <label
        style={{
          fontSize: 13,
          fontWeight: 600,
        }}
      >
        Notes

        <textarea
          rows={5}
          value={notes}
          onChange={(e) => {
            setNotes(e.target.value);
            setDirty(true);
          }}
          style={{
            width: '100%',
            marginTop: 4,
          }}
        />
      </label>

      <label
        style={{
          fontSize: 13,
          fontWeight: 600,
        }}
      >
        Follow-up date

        <input
          type="date"
          value={followUpDate}
          onChange={(e) => {
            setFollowUpDate(e.target.value);
            setDirty(true);
          }}
          style={{
            width: '100%',
            marginTop: 4,
          }}
        />
      </label>

      <div style={{ display: 'flex', gap: 8 }}>
        <button
          onClick={save}
          disabled={!dirty}
        >
          Save
        </button>

        {onDelete && (
          <button
            onClick={() => onDelete(job.id)}
            style={{
              color: '#b91c1c',
              marginLeft: 'auto',
            }}
          >
            Delete job
          </button>
        )}
      </div>
    </div>
  );
}