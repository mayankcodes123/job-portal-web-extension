import { extractJob } from './extractors/naukri';

const job = extractJob();

chrome.runtime.sendMessage({
  type: 'EXTRACT_RESULT',
  job,
});
