import react from '@vitejs/plugin-react'
import { playwright } from '@vitest/browser-playwright'
import { defineConfig } from 'vitest/config'
import type { BrowserCommand } from 'vitest/node'

// The real-browser smoke tests (src/browser/**): headless Chromium through Playwright, real layout,
// real CSS and real mouse and keyboard input. Kept apart from vite.config.ts so the jsdom suite is
// unchanged. Run with `npm run test:browser`; Maven runs it in integration-test after `npm test`.

type Point = { x: number; y: number }

/** Where the test iframe sits in the page, so iframe coordinates become Playwright mouse coordinates. */
async function frameOrigin(context: Parameters<BrowserCommand<[]>>[0]): Promise<Point> {
  const frame = await context.frame()
  const box = await (await frame.frameElement()).boundingBox()
  if (!box) throw new Error('the test iframe has no layout box')
  return { x: box.x, y: box.y }
}

/**
 * A real mouse drag: press at `from`, move through each of `path` in small steps, release at the
 * last one. Coordinates are the test document's client coordinates.
 */
const mouseDrag: BrowserCommand<[from: Point, path: Point[]]> = async (context, from, path) => {
  const origin = await frameOrigin(context)
  const { mouse } = context.page
  await mouse.move(origin.x + from.x, origin.y + from.y)
  await mouse.down()
  for (const to of path) await mouse.move(origin.x + to.x, origin.y + to.y, { steps: 8 })
  await mouse.up()
}

/** A real key press (Playwright key names, e.g. "Space", "ArrowRight", "Enter", "Escape"). */
const pressKey: BrowserCommand<[key: string]> = async (context, key) => {
  await context.page.keyboard.press(key)
}

/** Emulates the `prefers-reduced-motion` media feature for the page. */
const emulateReducedMotion: BrowserCommand<[reduce: boolean]> = async (context, reduce) => {
  await context.page.emulateMedia({ reducedMotion: reduce ? 'reduce' : 'no-preference' })
}

export default defineConfig({
  plugins: [react()],
  test: {
    include: ['src/browser/**/*.browser.test.tsx'],
    browser: {
      enabled: true,
      headless: true,
      provider: playwright(),
      instances: [{ browser: 'chromium' }],
      // a failure is reported in the console; no screenshot files are left in the tree
      screenshotFailures: false,
      commands: { mouseDrag, pressKey, emulateReducedMotion },
    },
  },
})
