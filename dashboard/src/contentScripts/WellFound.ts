import { extractJob } from './extractors/wellfound';

function tryExtract(attempt = 1) {
  const job = extractJob();

  console.log(
    `Wellfound extraction attempt ${attempt}:`,
    job
  );

  if (job || attempt >= 5) {
    chrome.runtime.sendMessage({
      type: 'EXTRACT_RESULT',
      job,
    });

    return;
  }

  setTimeout(() => {
    tryExtract(attempt + 1);
  }, 1000);
}

tryExtract();
