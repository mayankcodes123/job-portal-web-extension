export interface JobData {
  title: string;
  company: string;
  location: string;
  salary?: string;
  experienceRequired?: string;
  jobUrl: string;
  source: 'wellfound';
}

export function extractJob(): JobData | null {
  const title =
    document
      .querySelector('h1')
      ?.textContent
      ?.trim() || '';

  let company =
    document
      .querySelector('.inline.text-md.font-semibold')
      ?.textContent
      ?.trim() || '';

  // Fallback: extract company from job title
  // Example: "... at Parsewave" → "Parsewave"
  if (!company) {
    const match = title.match(/\bat\s+(.+)$/i);

    if (match) {
      company = match[1].trim();
    }
  }

  const pageText =
    document.body.innerText || '';

  let location = '';

  if (pageText.includes('Everywhere')) {
    location = 'Everywhere';
  }

  const compensation =
    document
      .querySelector('h1')
      ?.parentElement
      ?.parentElement
      ?.innerText
      ?.split('\n')[1]
      ?.trim() || '';

  const bodyText =
    document.body.innerText || '';

  let experienceRequired = '';

  if (
    bodyText.includes(
      'No experience required'
    )
  ) {
    experienceRequired =
      'No experience required';
  }

  console.log('TITLE:', title);
  console.log('COMPANY:', company);
  console.log('LOCATION:', location);
  console.log('COMPENSATION:', compensation);

  if (!title || !company || !location) {
    return null;
  }

  return {
    title,
    company,
    location,
    salary:
      compensation || undefined,
    experienceRequired:
      experienceRequired || undefined,
    jobUrl: window.location.href,
    source: 'wellfound',
  };
}