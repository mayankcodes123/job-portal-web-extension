document
  .getElementById('saveJob')
  .addEventListener('click', async () => {

    const [tab] = await chrome.tabs.query({
      active: true,
      currentWindow: true,
    });

    if (!tab.id) {
      return;
    }

    chrome.tabs.sendMessage(tab.id, {
      type: 'EXTRACT_AND_SAVE',
    });

    window.close();
  });


document
  .getElementById('openDashboard')
  .addEventListener('click', () => {

    chrome.tabs.create({
      url: chrome.runtime.getURL('dashboard.html'),
    });

    window.close();
  });
  