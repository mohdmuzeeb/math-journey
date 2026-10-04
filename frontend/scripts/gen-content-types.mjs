// Generates TypeScript types from the content JSON Schemas (AD-7): one .d.ts per
// content/schemas/<name>.schema.json, written to src/content-types/<name>.d.ts (git-ignored).
import { mkdir, readFile, readdir, rm, writeFile } from 'node:fs/promises'
import { basename, dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { compile } from 'json-schema-to-typescript'

const here = dirname(fileURLToPath(import.meta.url))
const schemaDir = resolve(here, '../../content/schemas')
const outDir = resolve(here, '../src/content-types')
const SCHEMA_BASE = 'https://mathjourney.app/content/schemas/'

// Schemas carry absolute https $ids; serve those refs from the local folder, never the network.
const localSchemas = {
  order: 1,
  canRead: (file) => file.url.startsWith(SCHEMA_BASE),
  read: (file) => readFile(join(schemaDir, file.url.slice(SCHEMA_BASE.length).split('#')[0]), 'utf8'),
}

const files = (await readdir(schemaDir)).filter((f) => f.endsWith('.schema.json')).sort()
await rm(outDir, { recursive: true, force: true })
await mkdir(outDir, { recursive: true })

for (const file of files) {
  const schema = JSON.parse(await readFile(join(schemaDir, file), 'utf8'))
  const name = basename(file, '.schema.json')
  const ts = await compile(schema, schema.title ?? name, {
    cwd: schemaDir,
    bannerComment: `/* Generated from content/schemas/${file} by scripts/gen-content-types.mjs. Do not edit. */`,
    // number-line keeps its types under $defs with an empty root; emit them anyway.
    unreachableDefinitions: true,
    additionalProperties: false,
    format: true,
    $refOptions: { resolve: { http: false, local: localSchemas } },
  })
  await writeFile(join(outDir, `${name}.d.ts`), ts)
  console.log(`content-types: ${name}.d.ts`)
}
