export interface JobData {
  title: string;
  company: string;
  location: string;
  salary?: string;
  experienceRequired?: string;
  jobUrl: string;
  source: 'naukri';
}

export function extractJob(): JobData | null {
  const scripts = Array.from(
    document.querySelectorAll(
      'script[type="application/ld+json"]'
    )
  );

  let jobPosting: any = null;

  for (const script of scripts) {
    try {
      const data = JSON.parse(
        script.textContent || ''
      );

      if (data?.['@type'] === 'JobPosting') {
        jobPosting = data;
        break;
      }
    } catch {
      // Ignore invalid JSON-LD
    }
  }

  if (!jobPosting) {
    return null;
  }

  const title =
    typeof jobPosting.title === 'string'
      ? jobPosting.title.trim()
      : '';

  const company =
    typeof jobPosting.hiringOrganization?.name ===
    'string'
      ? jobPosting.hiringOrganization.name.trim()
      : '';

  const location =
    jobPosting.jobLocation?.address?.addressLocality ||
    '';

  if (!title || !company || !location) {
    return null;
  }

  let salary = '';

  const salaryValue =
    jobPosting.baseSalary?.value;

  if (salaryValue) {
    if (
      typeof salaryValue === 'object'
    ) {
      const min = salaryValue.minValue;
      const max = salaryValue.maxValue;

      if (min && max) {
        salary = `₹${min} - ₹${max}`;
      } else if (min) {
        salary = `₹${min}`;
      } else if (max) {
        salary = `₹${max}`;
      }
    } else {
      salary = String(salaryValue);
    }
  }

  let experienceRequired = '';

  const months =
    jobPosting.experienceRequirements
      ?.monthsOfExperience;

  if (typeof months === 'number') {
    const years = months / 12;

    experienceRequired =
      years >= 1
        ? `${years} years`
        : `${months} months`;
  }

  return {
    title,
    company,
    location,
    salary: salary || undefined,
    experienceRequired:
      experienceRequired || undefined,
    jobUrl: window.location.href,
    source: 'naukri',
  };
}

