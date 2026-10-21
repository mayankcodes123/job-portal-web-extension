# Job Application CRM

A Chrome extension and dashboard for organizing and tracking job applications in one place.

## Overview

Job Application CRM helps job seekers save job listings, manage application progress, and keep track of opportunities without relying on spreadsheets.

## Features

### Job Application Dashboard
- Kanban-style application workflow.
- Track jobs across Saved, Applied, Screening, Interview, Offer, and Rejected stages.
- Manage job application information in one place.

### Job Listing Extraction
The beta supports manual, user-triggered extraction from:

- **Naukri:** Job title, company, location, salary, experience, and job URL.
- **Wellfound:** Job title, company, location, compensation, and job URL.
- Duplicate prevention for extracted listings.

LinkedIn extraction is outside the current beta scope.

### Technology Stack
- React and TypeScript
- Vite
- Chrome Extension Manifest V3
- Java and Spring Boot for the planned reminder backend
- MySQL for the reminder backend

## Project Structure

- `src/` — Frontend application source code.
- `public/` — Public assets.
- `backend/` — Backend services and related files.
- `dist/` — Generated frontend build output.
- `dashboard.html` — Dashboard entry HTML.
- `manifest.json` — Extension manifest, if included in the generated build.

## Getting Started

### Prerequisites
- Node.js and npm
- Google Chrome

### Install Dependencies

```bash
npm install
```

### Run the Development Server

```bash
npm run dev
```

### Build the Frontend

```bash
npm run build
```

### Load the Extension in Chrome

1. Run the production build.
2. Open `chrome://extensions`.
3. Enable Developer mode.
4. Select Load unpacked.
5. Select the generated extension directory containing `manifest.json`.

Ensure the generated directory contains all required extension files before loading it.

## Java Reminder Backend

The project includes a Java/Spring Boot reminder service under `backend/`. Its setup and database configuration should be completed and verified separately before relying on reminder functionality.

## Current Beta Scope

- Naukri job extraction.
- Wellfound job extraction.
- Duplicate prevention.
- Job application tracking dashboard.

LinkedIn extraction is not included in the beta.

## Development Status

This project is being developed iteratively. Features described as part of the beta have been tested; backend reminder functionality should be verified independently before being considered production-ready.
