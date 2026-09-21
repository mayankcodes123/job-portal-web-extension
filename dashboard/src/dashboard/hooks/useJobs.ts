import { useEffect, useState, useCallback } from 'react';
import type { Job } from '../../shared/types';

import {
  getAllJobs,
  addJob,
  updateJob,
  deleteJob,
  subscribeJobs,
} from '../../shared/storage';

export function useJobs() {
  const [jobs, setJobs] = useState<Job[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getAllJobs().then((j) => {
      setJobs(j);
      setLoading(false);
    });

    const unsub = subscribeJobs(setJobs);

    return unsub;
  }, []);

  const add = useCallback(
    (input: Parameters<typeof addJob>[0]) => addJob(input),
    []
  );

  const update = useCallback(
    (id: string, patch: Partial<Job>) => updateJob(id, patch),
    []
  );

  const remove = useCallback(
    (id: string) => deleteJob(id),
    []
  );

  return {
    jobs,
    loading,
    add,
    update,
    remove,
  };
}