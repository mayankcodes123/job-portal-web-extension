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
      .querySelector('.styles_title_xpQDw')
      ?.textContent?.trim() || '';

  const company =
    document
      .querySelector('.inline.text-md.font-semibold')
      ?.textContent?.trim() || '';

  const location =
    document
      .querySelector('.styles_location__09Z62')
      ?.textContent?.trim() || '';

  const compensation =
    document
      .querySelector('[class*="styles_compensation"]')
      ?.textContent?.trim() || '';

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

  if (!title || !company || !location) {
    return null;
  }

  return {
    title,
    company,
    location,
    salary: compensation || undefined,
    experienceRequired:
      experienceRequired || undefined,
    jobUrl: window.location.href,
    source: 'wellfound',
  };
}