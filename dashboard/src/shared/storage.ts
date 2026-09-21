import type { Job } from './types';

const KEY = 'jobs';

export async function getAllJobs(): Promise<Job[]> {
  const result = await chrome.storage.local.get(KEY);
  return (result[KEY] as Job[]) ?? [];
}

export async function addJob(
  input: Omit<Job, 'id' | 'createdAt' | 'updatedAt'>
): Promise<Job> {
  const now = new Date().toISOString();

  const job: Job = {
    ...input,
    id: crypto.randomUUID(),
    createdAt: now,
    updatedAt: now,
  };

  const jobs = await getAllJobs();

  await chrome.storage.local.set({
    [KEY]: [job, ...jobs],
  });

  return job;
}

export async function updateJob(
  id: string,
  patch: Partial<Job>
): Promise<void> {
  const jobs = await getAllJobs();

  const next = jobs.map((j) =>
    j.id === id
      ? {
          ...j,
          ...patch,
          updatedAt: new Date().toISOString(),
        }
      : j
  );

  await chrome.storage.local.set({
    [KEY]: next,
  });
}

export async function deleteJob(id: string): Promise<void> {
  const jobs = await getAllJobs();

  await chrome.storage.local.set({
    [KEY]: jobs.filter((j) => j.id !== id),
  });
}

/**
 * Subscribe to storage changes. Returns an unsubscribe function.
 */
export function subscribeJobs(
  callback: (jobs: Job[]) => void
): () => void {
  const listener = (
    changes: { [key: string]: chrome.storage.StorageChange },
    area: string
  ) => {
    if (area === 'local' && changes[KEY]) {
      callback((changes[KEY].newValue as Job[]) ?? []);
    }
  };

  chrome.storage.onChanged.addListener(listener);

  return () => chrome.storage.onChanged.removeListener(listener);
}