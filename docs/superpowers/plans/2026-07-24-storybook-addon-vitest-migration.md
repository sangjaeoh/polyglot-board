# Storybook test-runner → addon-vitest Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `packages/ui`'s Storybook workbench gate (`@storybook/test-runner` + `axe-playwright` + a static-build-and-serve orchestration script) with `@storybook/addon-vitest`, matching the current guide (`apps/frontend/docs/code-quality.md`), while preserving the exact dual light/dark `prefers-color-scheme` accessibility check semantics.

**Architecture:** Use Storybook's official `npx storybook@latest add @storybook/addon-vitest` installer to scaffold version-correct Vitest browser-mode config (avoids hand-guessing config shape against a specific Storybook/Vitest version pairing). Layer a custom Vitest browser command (`setColorScheme`) on top — Vitest's browser-mode test code cannot reach Playwright's raw `page.emulateMedia` directly, only through a server-side custom command — and a project-level `afterEach` hook that runs the accessibility check twice per story (once per color scheme), reproducing the old `test-runner.ts` `postVisit` behavior exactly. Remove the now-unnecessary static-build-serve orchestration script, since addon-vitest runs directly against the Vite module graph.

**Tech Stack:** Storybook 10.5.0, Vitest (browser mode, Playwright provider), `@storybook/addon-vitest`, `@storybook/addon-a11y`, pnpm workspace.

## Global Constraints

- Scope is `polyglot-board/apps/frontend/packages/ui` only — no other package or app.
- The dual-theme check (`light` then `dark`, both via real `prefers-color-scheme` media emulation, not the preview toolbar override) must run for every story, matching `test-runner.ts`'s current `postVisit` behavior exactly.
- The `preview.tsx` toolbar-vs-gate dual-validation design (documented in its own comments) stays untouched — do not modify `preview.tsx`.
- "오프라인·비대화형" (offline, non-interactive) gate requirement stays: no network calls beyond one-time dependency/browser-binary installation, no interactive prompts during `test-stories`/`verify`.
- Exact dependency versions are resolved by pnpm at install time — do not hand-pin versions where the installer or `pnpm add` already resolves one compatible with `storybook@10.5.0`.
- One commit per task, Conventional Commits style (`feat:`/`chore:`/`refactor:` as appropriate — this is a real code repo, not the docs-only guide repo).
- Working directory for all commands below: `/Users/sangjaeoh/Desktop/gitspace/polyglot-board/apps/frontend/packages/ui`, unless stated otherwise.

---

### Task 1: Scaffold addon-vitest via the official installer

**Files:**
- Modify: `package.json` (installer adds deps/scripts)
- Create: `vitest.config.ts` (installer-generated, exact shape TBD by installer)
- Possibly modify: `.storybook/main.ts`, `.storybook/preview.tsx` (installer may add annotations — review, and revert any preview.tsx touch per Global Constraints)
- Create (if installer generates it): `.storybook/vitest.setup.ts`

**Interfaces:**
- Produces: a working `vitest --project=storybook` (or whatever project name the installer assigns — confirm and record the exact name for later tasks) invocation that at minimum boots the browser and renders stories without crashing.

- [ ] **Step 1: Run the installer**

```bash
cd /Users/sangjaeoh/Desktop/gitspace/polyglot-board/apps/frontend/packages/ui
pnpm dlx storybook@latest add @storybook/addon-vitest
```

Answer any interactive prompts using pnpm as the package manager and accepting Playwright browser binary installation if asked.

- [ ] **Step 2: Inspect what changed**

```bash
git status --short
git diff -- package.json .storybook/main.ts .storybook/preview.tsx
cat vitest.config.ts
find . -maxdepth 2 -iname "vitest.setup.*" -not -path "*/node_modules/*"
```

Record in your report: the exact project `name` set in `vitest.config.ts`'s `test` block (needed for Task 2's `--project` flag), and whether the installer created `.storybook/vitest.setup.ts` or modified `.storybook/preview.tsx`.

- [ ] **Step 3: Revert any preview.tsx change**

If the installer modified `.storybook/preview.tsx`, run:

```bash
git checkout -- .storybook/preview.tsx
```

Per Global Constraints, this file's toolbar-vs-gate design is out of scope. If the installer's change to this file was required for addon-vitest to function (not just optional annotation registration), STOP and report BLOCKED — do not silently keep or silently drop a required change.

- [ ] **Step 4: Verify the harness boots**

```bash
pnpm exec vitest run --project=<name-from-step-2>
```

Expected: process exits 0 (or fails only on pre-existing story content, not on config/harness errors — read the output carefully; a config error looks like a thrown exception before any test names print, a story failure looks like a normal Vitest test failure with a story name). If it's a harness/config error, fix the config before proceeding — this is not a story problem yet, there's no custom logic in place.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "chore(ui): scaffold @storybook/addon-vitest via official installer"
```

---

### Task 2: Add the `setColorScheme` custom browser command

**Files:**
- Modify: `vitest.config.ts`
- Create: `vitest.setColorScheme.test.ts` (temporary-but-kept regression test for the command itself, colocated at package root or under `src/` per existing test placement conventions — check `coding-conventions.md`'s test placement rule and match it)

**Interfaces:**
- Produces: a Vitest browser command `setColorScheme(scheme: 'light' | 'dark' | 'no-preference')` callable in any browser-mode test via `import { commands } from 'vitest/browser'`. This is what Task 3's `afterEach` hook consumes.

- [ ] **Step 1: Read the generated `vitest.config.ts` from Task 1**

Confirm the exact shape the installer used for `test.browser` (e.g. `browser.instances` + `browser.provider`, or the older `browser.name` + `browser.provider: 'playwright'` string form — Vitest's own current docs show `provider: playwright()` imported from `@vitest/browser-playwright`, but the installer may pin an older Vitest where the shape differs). Match the command definition below to whatever provider API the installer actually wired — the provider object needs to be the Playwright provider for `page`/`context` to be available in the command context.

- [ ] **Step 2: Add the custom command**

Add to `vitest.config.ts`, inside the existing `test.browser` block (adapt the surrounding object shape to match what Task 1 generated — this is the delta, not a full file replacement):

```typescript
import type { BrowserCommand } from 'vitest/node';

type ColorScheme = 'light' | 'dark' | 'no-preference';

const setColorScheme: BrowserCommand<[scheme: ColorScheme]> = async ({ page }, scheme) => {
  await page.emulateMedia({ colorScheme: scheme });
};

// inside test.browser.commands:
// commands: { setColorScheme }
```

Also add the TypeScript module augmentation (put it in `vitest.config.ts` or a `.d.ts` alongside it, matching whatever the installer already generates for other type augmentation, if any):

```typescript
declare module 'vitest/browser' {
  interface BrowserCommands {
    setColorScheme: (scheme: ColorScheme) => Promise<void>;
  }
}
```

- [ ] **Step 3: Write a regression test for the command**

```typescript
import { commands } from 'vitest/browser';
import { page } from 'vitest/browser';
import { expect, test } from 'vitest';

test('setColorScheme emulates prefers-color-scheme: dark', async () => {
  await commands.setColorScheme('dark');
  const matches = await page.evaluate(() => window.matchMedia('(prefers-color-scheme: dark)').matches);
  expect(matches).toBe(true);
});

test('setColorScheme emulates prefers-color-scheme: light', async () => {
  await commands.setColorScheme('light');
  const matches = await page.evaluate(() => window.matchMedia('(prefers-color-scheme: dark)').matches);
  expect(matches).toBe(false);
});
```

If `vitest/browser`'s `page` export doesn't expose a raw `.evaluate()` (check what Task 1's installed version actually exports — `BrowserPage` is a Vitest-specific wrapper, not Playwright's `Page`), use whatever equivalent in-browser-JS-execution primitive that version exposes (e.g. a second custom command that reads `window.matchMedia(...)` server-side isn't possible — it must run in-browser — so prefer `page.evaluate` if present, otherwise render a tiny probe component that reads `window.matchMedia` into the DOM and assert on the rendered text). Report which path you used.

- [ ] **Step 4: Run it**

```bash
pnpm exec vitest run --project=<name> vitest.setColorScheme.test.ts
```

Expected: 2/2 passing.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(ui): add setColorScheme Vitest browser command"
```

---

### Task 3: Wire the dual-theme accessibility check

**Files:**
- Modify or create: `.storybook/vitest.setup.ts`

**Interfaces:**
- Consumes: `commands.setColorScheme` from Task 2.
- Produces: every story test now performs the same accessibility check twice — once under `light`, once under `dark` — matching `test-runner.ts`'s current `postVisit`.

- [ ] **Step 1: Read the current `.storybook/test-runner.ts` for the exact behavior to reproduce**

The file (do not delete it yet — Task 4 does that) currently:
1. Gets story context (`storyContext.parameters?.a11y?.disable` — skip if set).
2. Applies story-level axe rule overrides (`storyContext.parameters?.a11y?.config?.rules`).
3. For each of `['light', 'dark']`: emulates that color scheme, waits for page ready, runs the accessibility check against `#storybook-root`.
4. Resets to `no-preference` at the end.

- [ ] **Step 2: Confirm how addon-a11y's Vitest integration is already wired**

Check whether Task 1's installer already registered `@storybook/addon-a11y/preview` project annotations in `.storybook/vitest.setup.ts` (or wherever it put setup). If yes, the file already has a `setProjectAnnotations([...])` call — add to it rather than replacing it. If no, add:

```typescript
import { setProjectAnnotations } from '@storybook/react-vite';
import * as previewAnnotations from './preview';
import * as a11yAddonAnnotations from '@storybook/addon-a11y/preview';
import { beforeAll } from 'vitest';

const annotations = setProjectAnnotations([previewAnnotations, a11yAddonAnnotations]);

beforeAll(annotations.beforeAll);
```

(Adjust the `setProjectAnnotations` import source if Task 1's installer used a different one — some Storybook versions export it from `@storybook/your-framework` vs a shared testing package; match whatever Task 1's generated file already imports it from.)

- [ ] **Step 3: Add the dual-theme afterEach hook**

Add to the same file, importing whatever addon-a11y exposes for running an a11y check programmatically outside its automatic per-story hook (check `@storybook/addon-a11y`'s exports — if it does not expose a standalone "run a11y check now" function callable from a custom hook, keep `axe-playwright`'s `checkA11y`-equivalent approach is not available in browser-mode Vitest tests since there's no raw `page` in test code — in that case, implement the check via a second custom browser command in `vitest.config.ts`, `runAxeCheck`, that uses `@axe-core/playwright` or `axe-playwright`'s server-side APIs against the raw `page`/`context`, mirroring `test-runner.ts`'s `injectAxe`/`checkA11y` calls exactly. Report which path was available and used.):

```typescript
import { afterEach } from 'vitest';
import { commands } from 'vitest/browser';

afterEach(async () => {
  for (const scheme of ['light', 'dark'] as const) {
    await commands.setColorScheme(scheme);
    // run the a11y check chosen in the step above (addon-a11y hook call or runAxeCheck command)
  }
});
```

- [ ] **Step 4: Verify against a real story**

```bash
pnpm exec vitest run --project=<name>
```

Expected: existing stories pass (they passed the old test-runner gate already, so no real violations expected).

- [ ] **Step 5: Prove the check actually catches violations**

Temporarily add a deliberately inaccessible story (e.g. an `<img>` with no `alt` in a scratch `.stories.tsx` file, or edit an existing story's render to omit a required label) and re-run. Expected: FAIL, with an axe violation in the output. Record the exact failing output in your report as evidence, then revert the temporary change.

```bash
pnpm exec vitest run --project=<name>
```

Expected after revert: back to passing.

- [ ] **Step 6: Prove the dark-mode check actually engages**

Temporarily hardcode a story's markup to use a raw light-only color (bypassing tokens) so it only fails contrast under the dark palette, run, confirm FAIL, then revert and confirm PASS. Record both outputs in your report.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat(ui): reproduce dual light/dark a11y check in addon-vitest setup"
```

---

### Task 4: Remove the obsolete test-runner path

**Files:**
- Delete: `.storybook/test-runner.ts`
- Delete: `packages/ui/scripts/test-stories.mjs`
- Modify: `package.json` (`test-stories` script, dependencies)

**Interfaces:** None — this is cleanup only, Tasks 2–3 already built the replacement.

- [ ] **Step 1: Delete the obsolete files**

```bash
git rm .storybook/test-runner.ts scripts/test-stories.mjs
```

- [ ] **Step 2: Update `package.json` scripts**

Replace the `test-stories` script. Current:
```json
"test-stories": "node ./scripts/test-stories.mjs",
```
New (use the exact project name recorded in Task 1):
```json
"test-stories": "vitest run --project=<name>",
```
`verify` already calls `test-stories` — leave it unchanged.

- [ ] **Step 3: Remove obsolete dependencies**

```bash
pnpm remove @storybook/test-runner axe-playwright
```

- [ ] **Step 4: Full clean run**

```bash
pnpm install
pnpm run test-stories
```

Expected: exits 0, same story count exercised as Task 3's Step 4 run.

- [ ] **Step 5: Grep for leftovers**

```bash
git grep -n "test-runner\|axe-playwright" -- . ':!pnpm-lock.yaml'
```

Expected: no output.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "chore(ui): remove @storybook/test-runner and axe-playwright"
```

---

### Task 5: Final sanity checks

**Files:** None modified — verification only.

- [ ] **Step 1: Confirm the production build still works**

```bash
pnpm run build-storybook
```

Expected: exits 0. Addon swap should not affect the production build output, but confirm rather than assume.

- [ ] **Step 2: Confirm STORYBOOK_DISABLE_TELEMETRY still applies**

Check whether `vitest run` respects the same env vars the old `test-stories.mjs` set (`STORYBOOK_DISABLE_TELEMETRY=1`, `STORYBOOK_TELEMETRY_DISABLED=1`), or whether they need to be added to the new `test-stories` script / a `.env` the vitest config reads. Run once with `env -i PATH="$PATH" pnpm run test-stories` (strips inherited env) and confirm no network/telemetry prompt hangs the process. If telemetry calls are observed, add the env vars to the `test-stories` script string.

- [ ] **Step 3: Re-run the full verify gate one more time clean**

```bash
pnpm run verify
```

Expected: exits 0.

- [ ] **Step 4: Update the report**

Summarize in a final report file (`docs/superpowers/plans/2026-07-24-storybook-addon-vitest-migration-report.md`, one paragraph): what the installer generated (Task 1), the exact provider/command API shape used (Task 2), which a11y integration path was used (Task 3, since two were possible), and confirmation all verification commands in this plan passed. This is the artifact a future reader checks instead of re-deriving the migration from commit messages.

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/plans/2026-07-24-storybook-addon-vitest-migration-report.md
git commit -m "docs: record Storybook addon-vitest migration outcome"
```
