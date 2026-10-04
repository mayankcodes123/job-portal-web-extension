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
      .querySelector('h1.styles_header__ZLR7s')
      ?.textContent?.trim() || '';

  const company =
    document
      .querySelector('.inline.text-md.font-semibold')
      ?.textContent?.trim() || '';

  const pageText = document.body.innerText || '';

let location = '';

if (pageText.includes('Everywhere')) {
  location = 'Everywhere';
}

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