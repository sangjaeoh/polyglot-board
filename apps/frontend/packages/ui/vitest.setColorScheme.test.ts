import { commands } from 'vitest/browser';
import { describe, expect, it } from 'vitest';

describe('setColorScheme', () => {
  it("'dark'로 설정하면 prefers-color-scheme: dark 미디어 쿼리가 일치한다", async () => {
    await commands.setColorScheme('dark');
    const matches = await commands.matchesDarkColorScheme();

    expect(matches).toBe(true);
  });

  it("'light'로 설정하면 prefers-color-scheme: dark 미디어 쿼리가 불일치한다", async () => {
    await commands.setColorScheme('light');
    const matches = await commands.matchesDarkColorScheme();

    expect(matches).toBe(false);
  });
});
