import { extractJob } from './extractors/naukri';

chrome.runtime.onMessage.addListener((message) => {
  if (message.type !== 'EXTRACT_AND_SAVE') {
    return;
  }

  const job = extractJob();

  console.log('Naukri job extracted:', job);

  chrome.runtime.sendMessage({
    type: 'EXTRACT_RESULT',
    job,
  });
});