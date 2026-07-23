# Storybook test-runner → addon-vitest Migration: Outcome Report

`packages/ui`'s Storybook workbench gate was migrated from `@storybook/test-runner` (static build → local HTTP
serve → `test-storybook` CLI) to `@storybook/addon-vitest` (runs directly against the Vite module graph via
Vitest browser mode), per the plan at `docs/superpowers/plans/2026-07-24-storybook-addon-vitest-migration.md`.
All 5 tasks are complete and individually reviewed (see `.superpowers/sdd/task-{1..4}-report.md` and
`.superpowers/sdd/progress.md` for full detail); this file is the permanent summary. Final state (after the
post-review fixes below): `pnpm run test-stories` runs both Vitest projects (`storybook` + `browser-commands`)
and passes 5 files / 23 tests; the root `pnpm run verify` (the actual CI gate — see Post-review fixes) passes
end to end; `pnpm run build-storybook` still succeeds unchanged.

**Task 1 — scaffold.** Ran the official `pnpm dlx storybook@latest add @storybook/addon-vitest --package-manager
pnpm --yes` installer. It added `@storybook/addon-vitest` to `.storybook/main.ts`'s `addons` array, added
`vitest`/`@vitest/browser-playwright`/`@vitest/coverage-v8` as devDependencies, and generated
`vitest.config.ts` with a single `test.projects` entry (`name: 'storybook'`) wiring `storybookTest({ configDir
})` as a Vite plugin plus a Playwright-provider browser config (`chromium`, `headless: true`). It did **not**
generate a `.storybook/vitest.setup.ts` — that file was created by hand in Task 3. The installer's only touch to
`preview.tsx` (`parameters.a11y.test: 'todo'`) was reverted, since the toolbar-vs-gate dual-theme design in that
file is out of scope and the key wasn't required for the harness to boot.

**Task 2 — `setColorScheme` provider command.** `@vitest/browser-playwright`'s provider augments Vitest's
`BrowserCommandContext` (module augmentation on `vitest/node`) with a raw Playwright `Page` — not the
`vitest/browser` `BrowserPage` wrapper, which has no `emulateMedia`/`evaluate`. `setColorScheme` is a
`BrowserCommand<[scheme: 'light'|'dark'|'no-preference']>` registered under `test.browser.commands` in
`vitest.config.ts`, calling `page.emulateMedia({ colorScheme })` server-side; test code calls it via
`commands.setColorScheme(scheme)` from `vitest/browser`. **Deviation from the plan:** `storybookTest`'s plugin
hard-overrides `test.include` on any project it's attached to (Storybook 8.5+ indexing behavior), so a plain
`.test.ts` regression file cannot be discovered inside the `storybook` project. A second, minimal Vitest project
(`browser-commands`, same `projects` array, same browser/provider/commands config, no `storybookTest` plugin)
was added solely to host `vitest.setColorScheme.test.ts`; it does not affect the `storybook` project's story
tests. A second command, `matchesDarkColorScheme`, was added purely as test-support infrastructure (wraps
`page.evaluate` to read `window.matchMedia` from inside the browser, since `vitest/browser`'s own `page` has no
`evaluate`).

**Task 3 — dual-theme accessibility check.** `@storybook/addon-a11y@10.5.0` was investigated as the "native"
integration path (Path A in the plan): its `./preview` export ships only `{ afterEach, decorators,
initialGlobals, parameters }` — the actual `axe.run`-wrapping function is module-private, and the exported
`afterEach` is a parameterless, theme-unaware hook meant to be attached to project annotations and invoked once
per story automatically. No callable, theme-aware "run axe now" function is exposed, so Path A was not
available. **Path B** (the plan's documented fallback) was used instead: a third custom command, `runAxeCheck`,
drives `axe-playwright`'s `injectAxe`/`configureAxe`/`checkA11y` directly against the raw Playwright frame — the
same package and calls the old `test-runner.ts` used — invoked twice per story (light, then dark) from a
project-level `afterEach` hook in the hand-written `.storybook/vitest.setup.ts`, registered via `setupFiles` as
a bare string (an array is silently dropped by the `storybookTest` plugin's own `setupFiles` handling). The
check was empirically verified to catch real violations: a missing-`alt` story and a dark-mode-only
`color-contrast` story (hardcoded light-token color that fails contrast only against the dark canvas) were each
introduced, confirmed to fail the gate, then reverted. An initial fixed 200ms `setTimeout` (added to absorb a
race between `page.emulateMedia` resolving and CSS custom-property/transition recompute) was replaced after
review with a deterministic wait: poll `matchMedia` for the expected scheme, then await every active
`CSSTransition`'s `.finished` promise (bounded by a 5s safety-net timeout) — verified stable across 8
consecutive full-suite runs.

**Task 4 — cleanup, with one deviation.** `.storybook/test-runner.ts`, `scripts/test-stories.mjs`, and the
`@storybook/test-runner` devDependency (plus its Jest/nyc-based transitive tree) were removed; `test-stories`
was repointed at `vitest run --project=storybook`. The plan's Step 3 also called for removing `axe-playwright`,
on the original assumption that the a11y check would land entirely inside `@storybook/addon-a11y` (Path A).
Since Task 3's investigation (above) found Path A unavailable and implemented `runAxeCheck` directly against
`axe-playwright`, removing it breaks `vitest.config.ts`'s module resolution outright (verified: `pnpm run
test-stories` fails with `ERR_MODULE_NOT_FOUND` when removed). `axe-playwright` was kept as a devDependency —
this is a necessary amendment to the plan, not an oversight, and is load-bearing for the a11y gate.

**Task 5 — final sanity checks.** `pnpm run build-storybook` exits 0 (only an informational Vite chunk-size
warning, not an error) — the addon swap doesn't affect the production build. The telemetry env vars
(`STORYBOOK_DISABLE_TELEMETRY`, `STORYBOOK_TELEMETRY_DISABLED`) were investigated rather than assumed moot:
reading the installed `@storybook/addon-vitest@10.5.3` and `storybook@10.5.0` source confirmed that
`storybookTest`'s `configureVitest` hook loads Storybook's full preset chain (`experimental_loadStorybook` →
`presets.apply("core")`, the same `common-preset.js` used by `storybook build`/`dev`) and unconditionally fires
a `telemetry("test-run", …)` call on every `vitest run`. That preset's `core.disableTelemetry` reads
`process.env.STORYBOOK_DISABLE_TELEMETRY` — the identical env var the old script set — so the concern was real,
not obsolete. Confirmed empirically with `STORYBOOK_TELEMETRY_DEBUG=1`: without the disable var, a real
`test-run` telemetry payload (package versions, project metadata) was logged as about-to-send on every
invocation; with `STORYBOOK_DISABLE_TELEMETRY=1` set, no telemetry log appears at all. (`STORYBOOK_TELEMETRY_DISABLED`
is not read anywhere in the currently installed Storybook version — it's dead in practice, kept only for parity
with the old script and as a defensive no-op.) The telemetry call itself is fire-and-forget (not awaited), so it
could never have caused a hang — but it was a genuine, previously-unguarded outbound network call. Both env vars
were added directly to the `test-stories` script string in `package.json`
(`STORYBOOK_DISABLE_TELEMETRY=1 STORYBOOK_TELEMETRY_DISABLED=1 vitest run --project=storybook`), and re-verified
telemetry-free under `env -i PATH="$PATH" pnpm run test-stories`. This inline `VAR=1 command` syntax is
POSIX-shell-only; a `cross-env` dependency was deliberately not added because every environment this script
actually runs in — local dev (macOS/Linux) and the CI gate (`.github/workflows/verify.yml`, `runs-on:
ubuntu-latest`) — is a POSIX shell, not because the repo has no CI (it does: a real workflow runs `pnpm verify`
on every push/PR). `pnpm run verify` exits 0 as the final clean gate run: 4 test files / 21 tests passing,
matching the pre-migration baseline exactly.

**Post-review fixes.** The final review found the package-scoped `test-stories`/`verify` numbers above were
green while the actual CI gate — root `pnpm run verify`, which additionally runs `depcruise:frontend` — was
not: `apps/frontend/.dependency-cruiser.cjs`'s `no-orphans` rule had no exception for
`vitest.setColorScheme.test.ts` (Task 2's regression test, loaded only via the `browser-commands` Vitest project
because `storybookTest` clears `test.include` on any project it's attached to), so `depcruise:frontend` failed;
separately, `test-stories` only ran `--project=storybook`, so that same regression test never executed as part
of `verify` at all, leaving a coverage gap open across Tasks 2–4. Both were fixed: a `pathNot` exception
(`(^|/)vitest\.[^/]+\.test\.ts$`, matching the existing exception style) was added to `no-orphans`, and
`test-stories` now runs `vitest run --project=storybook --project=browser-commands`, bringing the count to 5
files / 23 tests. Root `pnpm run verify` (`turbo run verify && pnpm run depcruise:frontend && pnpm run
drift:check`) now passes end to end.
