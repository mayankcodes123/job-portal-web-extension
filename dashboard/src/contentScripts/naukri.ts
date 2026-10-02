import { extractJob } from './extractors/naukri';

chrome.runtime.onMessage.addListener(
  (message, sender, sendResponse) => {
    if (message.type !== 'EXTRACT_AND_SAVE') {
      return false;
    }

    function tryExtract(attempt = 1) {
      const job = extractJob();

      console.log(
        `Naukri extraction attempt ${attempt}:`,
        job
      );

      if (job || attempt >= 5) {
        chrome.runtime.sendMessage(
          {
            type: 'EXTRACT_RESULT',
            job,
          },
          (response) => {
            if (chrome.runtime.lastError) {
              sendResponse({
                success: false,
                error:
                  chrome.runtime.lastError.message,
              });

              return;
            }

            sendResponse(response);
          }
        );

        return;
      }

      setTimeout(() => {
        tryExtract(attempt + 1);
      }, 1000);
    }

    tryExtract();

    return true;
  }
);