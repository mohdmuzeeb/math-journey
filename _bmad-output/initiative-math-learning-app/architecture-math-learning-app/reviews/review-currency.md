# Review: Currency and reality-check of the Math Journey architecture spine

- Reviewer lens: Was each committed decision checked against current reality (versions, existence and fit, starter defaults), or asserted from training data?
- Spine: `../architecture-math-learning-app.md` (not modified)
- Evidence base: `.memlog.md` `(version)` entries plus live checks on 2026-10-03 (npm registry, Maven Central, start.spring.io metadata API, official docs, web search)

## Verdict

Most version pins were genuinely web-verified and still hold: Boot 4.1, Java 25, springdoc 3.x, Vite 8 and openapi-typescript 7. Three claims were not checked and are wrong or stale. The Initializr default build tool is Gradle, not Maven. React 19.3 is out, and the Vite template already resolves to it. @dnd-kit/core is officially "legacy". One toolchain conflict was never checked: openapi-typescript 7.13.0 declares a peer dependency of `typescript ^5.x`, but the Vite react-ts template pins TypeScript ~6.0.2, so the documented starter path will hit an npm peer-dependency error.

## Findings

### F1 - HIGH - openapi-typescript vs template TypeScript (peer-dependency conflict)
- **Spine says:** TypeScript is "version from the Vite React-TS template" and openapi-typescript is 7.x.
- **Current reality:** `create-vite` 9.2.1's `template-react-ts/package.json` pins `"typescript": "~6.0.2"`. `openapi-typescript@7.13.0` is still `latest`, last published 2026-02-11, and declares `peerDependencies: { "typescript": "^5.x" }`. With npm 7+ strict peer resolution, `npm i -D openapi-typescript` in the scaffolded project fails with ERESOLVE unless you force it. Also, npm `latest` for `typescript` is now 7.0.2 (2026-07-08, the native port). openapi-typescript does not declare support for it, and its programmatic-API compatibility with TS 7 is unverified.
- **Fix:** Add a Stack note that TypeScript is pinned at the template's 6.0.x and must not be bumped to 7.x. Add `"overrides": { "openapi-typescript": { "typescript": "$typescript" } }` to `frontend/package.json` and verify that `openapi-typescript` generates correctly under TS 6. Do not use `--legacy-peer-deps`. Make "type generation runs under the pinned TS" a scaffold acceptance check. Record this in the memlog as a verified constraint.

### F2 - MEDIUM - "Maven wrapper (Spring Initializr default)" is false
- **Spine says:** Build (backend): "Maven wrapper (Spring Initializr default)". The memlog lists "Maven wrapper" under the `(assumption)` entry, so nobody verified it.
- **Current reality:** start.spring.io metadata (`Accept: application/json`) returns `type.default = gradle-project`. Its default `javaVersion` is `17`, and the default `bootVersion` is `4.1.1.RELEASE`.
- **Fix:** Keep Maven. It fits the builder's skills, the `./mvnw verify` gate and frontend-maven-plugin. Change the rationale to "Maven wrapper (select **Maven** and **Java 25** explicitly in Initializr; its defaults are Gradle and Java 17)".

### F3 - MEDIUM - dnd-kit: @dnd-kit/core is officially legacy
- **Spine says:** dnd-kit is `@dnd-kit/core 6.3.x`. The memlog says "choose at scaffold, prefer stable", but the spine commits without recording the trade-off.
- **Current reality:** `@dnd-kit/core` 6.3.1 was published 2024-12-05 and nothing has been published since. Its peer range `react >=16.8.0` works with React 19. The official docs file it under **Legacy** with the banner "There's a new version of @dnd-kit available. We recommend you use the latest version instead." `@dnd-kit/react` is at 0.5.0 (2026-06-11), with a 0.5.1 beta in September 2026, peers `react ^18 || ^19`, and is still pre-1.0. The spine also omits `@dnd-kit/sortable` (10.0.0, peers `@dnd-kit/core ^6.3.0`), which `sort-match` would likely need.
- **Fix:** Pick one deliberately and say why. Option (a): `@dnd-kit/core 6.3.1` + `@dnd-kit/sortable 10.0.0`, marked "legacy, frozen API, accepted", using its proven `KeyboardSensor`, which the UX's keyboard-alternative requirement needs. Option (b): `@dnd-kit/react 0.5.x`, pinned exactly because it is 0.x. Either way, make the activity renderers the only importers so a later swap stays local.

### F4 - LOW - React "19.2.x" is already stale, and the template won't honor it
- **Spine says:** React 19.2.x. The memlog says "19.2.7 latest as of 2026-06", using June data on 2026-10-03.
- **Current reality:** React 19.3.0 was released 2026-09-09 and is npm `latest`. 19.2.8 came out 2026-07-21. The react-ts template declares `"react": "^19.2.8"`, so a fresh scaffold installs **19.3.0**, which contradicts the spine's pin.
- **Fix:** Change the Stack row to "19.x (latest minor at scaffold; 19.3 as of 2026-10)" and refresh the memlog entry.

### F5 - MEDIUM - "ProblemDetail (RFC 9457) for every non-2xx" is correct but not automatic
- **Spine says:** Errors use Spring `ProblemDetail` (RFC 9457) for every non-2xx response.
- **Current reality:** The RFC is correct. The Spring Framework 7.0.9 reference cites RFC 9457, which supersedes 7807. However, `spring.mvc.problemdetails.enabled` still defaults to `false` in Boot, and app exceptions only become ProblemDetail through an `@RestControllerAdvice` (typically one extending `ResponseEntityExceptionHandler`). Without both, some error paths, such as Boot's `/error` fallback, will not produce ProblemDetail.
- **Fix:** Extend the Errors/Config conventions to `spring.mvc.problemdetails.enabled: true` in `application.yml` plus a single global `@RestControllerAdvice` that maps domain exceptions to `ProblemDetail`. Make sure the SPA fallback (AD-1/web module) doesn't turn `/api/**` 404s into `index.html`.

### F6 - LOW - Flyway/H2 under Boot 4 modularization: confirmed, with one forward note
- **Spine says:** H2 and Flyway are "version managed by Spring Boot 4.1". Starters: Web, Data JPA, Flyway, H2, Validation.
- **Current reality:** Confirmed. An Initializr-generated Maven pom (Boot 4.1.1, Java 25) emits `spring-boot-starter-flyway`, `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-h2console`, `h2` (runtime) and matching `*-test` starters. `spring-boot-dependencies` 4.1.1 manages Flyway **12.4.0**, H2 **2.4.240** and Hibernate 7.4.5.Final. Boot 4 needs the Flyway **starter** to auto-configure, because `flyway-core` alone is no longer enough. The Initializr "Flyway" checkbox already adds the starter. **No `flyway-database-h2` artifact exists** on Maven Central: H2 support lives inside `flyway-core`, as the `org/flywaydb/core/internal/database/h2/` classes in 12.4.0 show. A later Postgres move (AD-11/Deferred) **will** need `flyway-database-postgresql` plus the driver. Flyway OSS is at 13.9.0 upstream, while Boot pins 12.4.0. Keep the Boot-managed version.
- **Fix:** Optionally add to Deferred/AD-11: "Postgres move = add `flyway-database-postgresql` + `postgresql` driver; config only otherwise." Name the starters in Boot 4 terms (`starter-webmvc`, `starter-flyway`).

### F7 - LOW - Frontend-into-jar build glue is described but not named or pinned
- **Spine says:** "The Maven build runs the frontend build and copies `frontend/dist` into the jar's static resources." No plugin is named.
- **Current reality:** The usual pattern is `com.github.eirslett:frontend-maven-plugin`. Maven Central lists **2.0.2** as the latest (2.0.0 was a major dependency/minimum-version bump). It installs a pinned Node/npm under the build directory and runs `npm ci` and `npm run build`. Then `maven-resources-plugin`, or Vite `build.outDir`, places the output in `target/classes/static`. Vite 8.3.2 requires Node `^20.19.0 || >=22.12.0`.
- **Fix:** Add a Stack row: "frontend-maven-plugin 2.0.x, Node 22 LTS pinned in the pom (`nodeVersion`)". This makes `./mvnw verify` reproducible without a system Node.

### F8 - INFO - Verified as current (no change needed)
- **Spring Boot 4.1.x:** 4.1.0 was released 2026-06-10 and **4.1.1 (2026-08-20) is the latest GA patch**. 4.2 exists only as milestones (4.2.0-M2 on Maven Central, and on Initializr as a non-default option), so 4.1 is correct. The spine's "latest 4.1 patch at scaffold" wording is right.
- **Java 25 LTS:** The Boot 4.1.1 system requirements give Java 17 to 26 inclusive, Maven 3.6.3+, and Spring Framework 7.0.9+. Java 25 is supported.
- **springdoc-openapi 3.x:** This line exists. **3.1.1** is the latest release (Maven Central, updated 2026-09-06), and its parent pom builds against Spring Boot **4.1.0**. 3.1.0 release notes: "upgrade to Spring Boot 4.1.0". The 2.x line stays on Boot 3. Use `springdoc-openapi-starter-webmvc-ui` (or `-api`).
- **Vite 8.x:** **8.3.2** is the latest (2026-10-01). `npm create vite@latest` (create-vite 9.2.1) still ships `template-react-ts`. Note that the template now uses **oxlint** instead of ESLint, and `@vitejs/plugin-react` 6.x.
- **openapi-typescript 7.x:** 7.13.0 is the latest, and there is no 8.x. The TS peer issue is covered in F1.

## Sources
- Spring Boot 4.1.0 announcement: https://spring.io/blog/2026/06/10/spring-boot-4/
- Spring Boot 4.1.1 announcement: https://spring.io/blog/2026/08/20/spring-boot-4-1-1-available-now/
- Spring Boot system requirements (4.1.1): https://docs.spring.io/spring-boot/system-requirements.html
- Spring Boot versions on Maven Central: https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/maven-metadata.xml
- spring-boot-dependencies 4.1.1 BOM: https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.1/spring-boot-dependencies-4.1.1.pom
- Spring Initializr metadata (defaults): https://start.spring.io (Accept: application/json); generated pom: https://start.spring.io/pom.xml?type=maven-project&bootVersion=4.1.1&javaVersion=25&dependencies=web,data-jpa,flyway,h2,validation
- springdoc-openapi on Maven Central: https://repo1.maven.org/maven2/org/springdoc/springdoc-openapi-starter-webmvc-ui/maven-metadata.xml ; parent 3.1.1: https://repo1.maven.org/maven2/org/springdoc/springdoc-openapi/3.1.1/springdoc-openapi-3.1.1.pom
- springdoc v3.1.0 release notes: https://newreleases.io/project/github/springdoc/springdoc-openapi/release/v3.1.0
- Flyway Boot 4 starter (OpenRewrite recipe): https://docs.openrewrite.org/recipes/java/spring/boot4/addspringbootstarterflyway
- Flyway on Maven Central (no flyway-database-h2; flyway-core and flyway-database-postgresql present): https://repo1.maven.org/maven2/org/flywaydb/
- Spring Framework 7 REST exceptions / RFC 9457: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html
- problemdetails default false: https://github.com/spring-projects/spring-boot/issues/32634
- npm registry: https://registry.npmjs.org/react , https://registry.npmjs.org/vite , https://registry.npmjs.org/create-vite , https://registry.npmjs.org/typescript , https://registry.npmjs.org/openapi-typescript , https://registry.npmjs.org/@dnd-kit/core , https://registry.npmjs.org/@dnd-kit/react , https://registry.npmjs.org/@dnd-kit/sortable
- dnd-kit legacy notice: https://dndkit.com/legacy/introduction/installation
- frontend-maven-plugin: https://repo1.maven.org/maven2/com/github/eirslett/frontend-maven-plugin/maven-metadata.xml ; https://github.com/eirslett/frontend-maven-plugin/blob/master/CHANGELOG.md
