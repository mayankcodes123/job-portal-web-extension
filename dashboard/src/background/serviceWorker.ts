import { addJob } from '../shared/storage';

chrome.runtime.onMessage.addListener(
  (message, sender, sendResponse) => {
    if (message.type !== 'EXTRACT_RESULT') {
      return false;
    }

    const job = message.job;

    if (!job) {
      sendResponse({
        success: false,
        error: 'Could not extract job',
      });

      return false;
    }

    addJob({
      title: job.title,
      company: job.company,
      location: job.location,
      jobUrl: job.jobUrl,
      source: 'naukri',
      status: 'saved',
      salary: job.salary,
      experienceRequired: job.experienceRequired,
      notes: '',
    })
      .then((savedJob) => {
        console.log('Job saved:', savedJob);

        sendResponse({
          success: true,
          job: savedJob,
        });
      })
      .catch((error) => {
        console.error('Failed to save job:', error);

        sendResponse({
          success: false,
          error: String(error),
        });
      });

    return true;
  }
);