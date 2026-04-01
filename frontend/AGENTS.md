<!-- BEGIN:nextjs-agent-rules -->
# This is NOT the Next.js you know

This version has breaking changes. APIs, conventions, and file structure may differ from older Next.js releases. Read the relevant guide in `node_modules/next/dist/docs/` before writing framework-level code, and pay attention to deprecation notices.
<!-- END:nextjs-agent-rules -->

# Frontend Guidelines

## Stack

- Next.js 16.2.2
- React 19.2.4
- TypeScript 5
- Tailwind CSS 4
- App Router under `src/app`

## Project Layout

- Routes and layouts: `src/app`
- Global styles: `src/app/globals.css`
- Static assets: `public`

## Commands

Run commands from `frontend/`.

- `npm run dev`
- `npm run build`
- `npm run lint`

## Coding Rules

- Use TypeScript for all application code.
- Keep route components and layouts aligned with the App Router conventions already in `src/app`.
- Prefer server-first patterns unless client interactivity is required.
- Add `"use client"` only where it is actually needed.
- Reuse existing styling patterns before introducing new utility combinations or component wrappers.

## Styling

- Keep global CSS minimal and push page-specific styling into the relevant route or component.
- Use Tailwind utilities consistently and avoid mixing multiple styling systems.
- Preserve the existing visual direction unless the task explicitly asks for a redesign.

## Validation

- Run `npm run lint` after meaningful frontend changes when feasible.
- For behavior changes, verify both desktop and mobile layouts if the task affects UI structure.
