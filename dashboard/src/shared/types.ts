export type JobStatus =
  | 'saved'
  | 'applied'
  | 'screening'
  | 'interview'
  | 'offer'
  | 'rejected';

export type JobSource = 'naukri' | 'wellfound' | 'linkedin' | 'manual';

export interface Job {
  id: string;
  title: string;
  company: string;
  location: string;
  jobUrl: string;
  source: JobSource;
  status: JobStatus;
  salary?: string;
  experienceRequired?: string;
  notes: string;
  appliedDate?: string;   // ISO date (yyyy-MM-dd)
  followUpDate?: string;  // ISO date (yyyy-MM-dd)
  createdAt: string;      // ISO datetime
  updatedAt: string;      // ISO datetime
}

export const STATUS_ORDER: JobStatus[] = [
  'saved',
  'applied',
  'screening',
  'interview',
  'offer',
  'rejected',
];

export const STATUS_LABELS: Record<JobStatus, string> = {
  saved: 'Saved',
  applied: 'Applied',
  screening: 'Screening',
  interview: 'Interview',
  offer: 'Offer',
  rejected: 'Rejected',
};