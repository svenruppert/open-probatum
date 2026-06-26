# Release Notes

## Unreleased

## 00.40.00 — Labs & Practical Evidence (2026-06-26)

The credential model grows from *theory-only* to *theory + practice* (concept
§23.4, drafted this cycle). A **Lab** is an authored, versioned, reviewed
practical task; a learner submits practical evidence against a published lab; a
Reviewer acting as assessor verifies or rejects it; and a verified submission
mints a credential carrying **practical-lab evidence** — closing the loop the
V00.30.00 `Evidence` abstraction was built to anticipate. All V00.10–V00.30
invariants carry over.

### Labs

- **Lab as versioned content** (§9.x): a practical task with instructions, a
  learning objective, difficulty, explicit acceptance criteria and tags — a
  versioned content object (`lineageId` + `version`) under the §16.2
  `ContentStatus` lifecycle, authored and reviewed exactly like a bank question
  (reuses `ContentAuthorship` + the review queue).
- **Lab authoring** (`LabBankView`) + the editorial lifecycle (`LabService`):
  create → submit for review → approve → publish; an author cannot approve their
  own lab.

### Practical evidence

- **Submission** (§16.3): a learner submits a write-up + an optional artefact link
  against a *published* lab (`LabView`); the submission pins the lab version
  (§16.4), so a later lab version never falsifies it. A learner sees only their
  own submissions.
- **Assessment**: a Reviewer/assessor (`lab:assess`) verifies or rejects
  submissions with feedback (`AssessmentQueueView`); an author cannot assess
  submissions to a lab they authored (segregation of duties, §3.6). The
  verify edge is serialised so two assessors never double-decide.

### Credentialing from practice

- **Practical-lab credential** (§10.6): a verified submission mints a credential
  carrying `PRACTICAL_LAB_VERIFIED` evidence (lab id + version) + the learner's
  stable recipient id, exactly once on the verify edge — recorded in the §17.3
  audit trail and shown in the wallet and on the validation page unchanged. A new
  `CredentialRule.LAB_VERIFIED` declares what earns it.

### Metrics

- **Lab quality metrics** (§20.2): per-lab submission count + verify/reject rates
  in the `MetricsView`.

### Platform

- Two new persisted stores in the single jSentinel-00.75.20 application store
  (`labs`, `labSubmissions`); a new `lab:assess` permission on the Reviewer role
  (the six-role model is unchanged). All user-facing strings are i18n (EN + DE).
  Exit production-review #2 rated 4/5 — one concurrency double-mint found and
  fixed in-cycle.

## 00.30.00 — Authoring & Credential Governance (2026-06-26)

The usable single-learner academy becomes an *operable, quality-assured* one
(concept §23.3): questions become reusable, reviewed, versioned objects; content
moves through an editorial lifecycle with a reviewer in the loop; and credentials
gain evidence, a durable recipient linkage, re-issue/superseding, a full status
page and an app-side audit trail. All V00.10.00 / V00.20.00 invariants carry over.

### Question bank & assessments

- **Versioned question bank** (§9.3/§9.4): questions are standalone, reusable
  objects with mandatory explanation + learning objective, topic, difficulty,
  tags, a lifecycle `ContentStatus` and a `lineageId` + `version` — a new version
  is a new immutable record, so existing attempts are never falsified (§16.4).
- **Bank operations + authoring UI**: create / tag / submit-for-review in a
  `QuestionBankView`; only APPROVED/PUBLISHED questions are reusable.
- **Assessments reference the bank**: an assessment embeds immutable bank-question
  snapshots carrying their `lineageId` + `version`; a question reused across
  assessments shares its lineage; a new bank version never mutates a captured one.

### Content lifecycle & review

- **Editorial lifecycle** (§16.2): `DRAFT → IN_REVIEW → APPROVED → PUBLISHED →
  DEPRECATED → ARCHIVED` (+ `REPLACED`), validated on every transition.
- **Offering versioning + lifecycle** (§16.2/§16.4): offerings gain
  `lineageId` + `version` + `ContentStatus`; learners browse only PUBLISHED
  offerings.
- **Review process** (§16.3): a new **Reviewer** role (`author:review`) and a
  `ReviewView` queue where a reviewer approves / rejects / publishes content an
  author submitted; an author who also reviews **cannot approve their own
  content** (segregation of duties, §3.6).

### Credentialing

- **Credential rules** (§3.7/§10.1): a `CredentialRule` declares what earns a
  credential — an assessment passed (optional minimum score) or a learning path
  completed — and the credential it awards.
- **Evidence + durable recipient** (§10.6/§16.4/§17.2): a credential records its
  issuance `Evidence` (assessment-passed / path-completed / manual award) + the
  source version it was issued from + a **stable `recipientId`**. Wallet and
  dashboard now filter by recipient id, not the free-form display name — closing
  the V00.20.00 cross-user exposure.
- **Re-issue + superseding** (§10.9): a renewal mints a fresh `VALID` successor
  and marks the predecessor `SUPERSEDED` pointing at it; both stay traceable.
- **Full validation page** (§11.7): every effective status renders, and a
  superseded credential links to its successor's validation page.

### Governance, audit & metrics

- **App-side credential audit trail** (§17.3): every issuance and governance
  action appends exactly one persisted `CredentialEvent`; a `CredentialAuditView`
  lists the trail. (jSentinel audit is closed to app-defined event types, so the
  trail is self-contained.)
- **Visibility & own-data hardening** (§17.1/§17.2/§3.6): operational dashboard
  tiles are admin-only; the drawer offers only permitted links; learners see only
  their own wallet / progress.
- **Quality metrics** (§20.2): per-assessment pass rates + average scores and the
  question bank's status/difficulty composition, in a `MetricsView`.

### Platform

- Two new persisted stores in the single jSentinel-00.75.20 application store
  (`credentialEvents`, `contentAuthors`). All user-facing strings are i18n
  (EN + DE). Exit production-review #2 rated 4/5 — one audit-completeness gap on
  re-issue found and fixed in-cycle.

## 00.20.00 — Einzelbenutzer-Academy (2026-06-26)

The platform becomes fully usable for an individual learner (concept §23.2):
register, find an offering, work a learning path, practise, pass a completion
check and manage the earned credential — all on top of the V00.10.00 trust flow,
whose invariants (trust model, id, three-layer status, role-based visibility,
privacy, rate limiting) carry over unchanged.

### Roles & onboarding

- **Five-role model** (§5): `Learner`, `Author`, `CredentialManager`,
  `PlatformAdmin`, `Verifier`, each mapped to a permission set in
  `AppAuthorizationService` (the bootstrap admin holds PlatformAdmin + Learner).
- **Self-registration** (§5.1): a public `/register` flow onboards a visitor as a
  Learner via the shared user directory + Argon2id + password preflight.

### Catalog, access & learning

- **Academy catalog** (§7): offerings with visibility `PUBLIC` / `REGISTERED` /
  `CODE` / `PREREQUISITE`, browsable with their per-learner access state.
- **Entitlements** (§12): `EntitlementService.canAccess` resolves access; code
  redemption + manual/prerequisite grants unlock gated offerings.
- **Learning experience** (§8): learning paths with mandatory/optional modules
  and a completion criterion; typed learning resources (article, video, download,
  link, checklist); per-learner progress with a live progress bar.

### Assessment, credentialing & wallet

- **Practice mode** (§9.5): answer questions with immediate feedback +
  explanations and **no** credential.
- **Completion check** (§9.6): a graded check with a pass threshold + counted
  attempts; the first passing attempt mints a `VALID` credential via the
  V00.10.00 issuance (re-passing does not duplicate).
- **Badge** (§10.8): a standalone, shareable representation referencing the same
  record as the certificate.
- **Credential wallet** (§19): the learner's credentials with effective status,
  expiry, QR code, PDF certificate download and the public validation link; a
  **dashboard** (§18) tile row for credentials earned + paths in progress.

### Authoring & governance

- **Minimal authoring** (§16.1): an Author creates a complete offering (title,
  description, visibility, module) without a developer.
- **Governance** (§10.9/§17.4): a Credential Manager revokes / suspends /
  reinstates issued credentials, effective immediately on the validation page.

### Platform

- Persistence consolidated in the single jSentinel-00.75.20 application store
  (users, credentials, offerings, entitlements, progress, assessments, attempts).
  All user-facing strings are i18n (EN + DE); the new domain mutation-tests
  cleanly (access 100 %, progress 95 %).

## 00.10.00 — Trust Core (2026-06-25)

The first domain-level vertical slice (concept §23.1): the trust flow proven
end-to-end on the grounding gear. The credential record is the sole source of
truth — online-only verification, no offline trust, no cryptographic signatures.
This cut also renames the base package from the inherited template identity and
hardens the inherited security stack.

### Trust Core — credential flow

- **Credential domain** (`com.svenruppert.openprobatum.credential`): the
  `Credential` record with a random, non-enumerable UUIDv4 id (§10.4), the
  three-layer status model (§10.5) — stored `CredentialStatus`
  (Valid/Revoked/Suspended/Superseded), computed `EffectiveStatus` adding
  `Expired` at read time (never stored), and the `ValidationResult` (the seven
  verification outcomes, §11.7). Optional expiry; immutable transitions.
- **Persistence**: `CredentialRepository` + Eclipse-Store backend in the single
  shared application store (record only — never a PDF, §3.2/§10.7).
- **Catalog / assessment**: minimal `Offering`/`LearningPath`/`Module` (§7.2/§8),
  `Assessment` with a fixed versioned question set + pass threshold and grading
  (§9.6), `Attempt` as the issuance evidence (§9.7).
- **Issuance**: on a passed attempt, `IssuanceService` mints and persists a
  `VALID` credential with this instance's `IssuerIdentity` (§4.3).
- **PDF + QR**: `CredentialPdf` (Apache PDFBox) renders the certificate with
  separated issued/generated dates, the match-rule and validation link, and **no
  printed status** (§10.7); `CredentialQr` (ZXing) encodes the validation link.
- **Public validation page** (`/validate/<id>`): the sole source of truth (§11.1)
  — reachable without login, computes the status on read, renders the seven
  results and only the match fields (§11.3) plus the match-rule and privacy hint
  (§11.4/§11.6). Dates render as `yyyy-MM-dd` in UTC, identical to the PDF, so a
  verifier comparing the two never sees a timezone-induced mismatch. Per-IP
  **rate limiting** via jSentinel `ratelimiting` (§11.5).
- **Governance**: `CredentialGovernance` revoke/suspend/supersede, each taking
  effect immediately on the validation page (§10.9/§17.4).

### Platform / ADR

- Base package renamed `com.svenruppert.flow` → `com.svenruppert.openprobatum`
  (TR-09); multi-tenancy held behind a single central `AppTenant.ID`
  (TR-08); development on the `00.10.00-SNAPSHOT` line, tagged `v00.10.00` at
  finalize (TR-10). PDFBox + ZXing pinned (Apache-2.0, TR-05).
- Bumped to **jSentinel 00.75.20** and consolidated the three parallel
  Eclipse-Stores (framework + user directory + credential repository) into one
  `JSentinelStoragePair` owned by `AppStorage`: a single storage tree, one
  shutdown hook and one two-phase idempotent close. No JDK serialisation anywhere
  — records are mapped reflectively. The standalone `application.jar` launcher's
  `Main-Class` was corrected to the renamed package.

### Hardening (entry-review remediation, 29 findings)

- Fixed the inherited security findings: session-version drift baseline on login,
  owner-only on-disk permissions for the token + credential/user stores, session
  fixation rotation, CSP/security-header filter, concurrent-rehash lock,
  login-timing decoy, `SessionInvalidated` audit on revoke, scrubbed setup logs,
  lazy bootstrap wiring, injectable `AppClock`, masked session ids, and a
  row-level own-sessions rule (§3.6/§5.4), among others.

### Notes

- Single instance / single offering for the slice. The learner assessment view,
  PDF download wiring and a Credential-Manager admin UI are intentionally
  deferred to a later version; the domain + the public validation UI ship here.
- No live host yet: the deploy marker is the built + tagged production WAR.

## 00.10.00 — grounding gear (2026-06-14)

First named cut of the template. Bundles the security stack
migration to jSentinel V00.74.00, the design-system + theming
overhaul, the i18n rewrite, the storage-paths/test-isolation pass,
the fail-fast bootstrap, the HIBP password leak check and the
persistent drift-detection store.

### Security stack

- Bumped to **jSentinel V00.74.00** (`com.svenruppert.jsentinel:*`),
  up from V00.73.00. Surfaces the new Token-Propagation API
  (`TokenCredentialStore`, `BearerToken/OidcAccessToken/RefreshToken/ApiKey`,
  `OutboundTokenStrategy`, `@PropagateToken`) — the
  `VaadinSessionTokenCredentialStore` is wired by default and the
  `SecurityFeaturesView` card panel documents the surface.
- `BootstrapExtension` SPI splits the wiring into three additive
  layers — `DefaultBootstrapExtension` (order 0),
  `PersistenceBootstrapExtension` (order 10) and
  `HardeningBootstrapExtension` (order 20). Each layer contributes
  to `.audit(...) / .sessions(...) / .credentials(...) /
  .policies(...)` independently, picked up via `ServiceLoader` at
  service init.
- Restructured the `com.svenruppert.flow.security` package into focused
  sub-packages: `model`, `roles`, `permissions`, `services`, `bootstrap`,
  `storage`.
- Typed permission catalog: `app:view`, `audit:read`,
  `admin:sessions`, `admin:roles` (`AppPermission`).
- Hybrid SPI registration: `@JSentinelAutoService` (annotation processor)
  for `AppAuthenticationService` and `AppAuthorizationService`;
  hand-written `META-INF/services` files for `AccessEvaluator`,
  `LoginListener`, `BootstrapExtension`, `JSentinelVersionStore`,
  `SubjectIdResolver`, and `VaadinServiceInitListener`.
- Eclipse-Store persistence: `EclipseStoreJSentinelStorage` opens at
  `app.storage.dir/jsentinel` (default `./data/jsentinel`). User
  directory, session store, audit ring buffer and the
  drift-detection version counter all share that backend.
  `JSentinelStorageProvider` exposes a lazy singleton + a
  `setStorage(...)` test seam; a shutdown hook closes the storage
  cleanly on JVM exit.
- First-admin bootstrap via `BootstrapWiring` + `SetupView` — a
  one-time token (`bootstrap.token` in the storage dir, validity
  `PT24H`) gates the initial admin creation. Username pattern
  `[A-Za-z0-9._-]` (1–64 chars); password policy minimum 12 chars.
- **Argon2id password hashing** (BouncyCastle modern profile)
  replaces PBKDF2. Legacy PBKDF2 hashes still verify and auto-rehash
  on next successful login via the `RehashDecisionEngine`.
- **HIBP password leak check** (`PasswordPreflight`) — k-anonymity
  range API call against `api.pwnedpasswords.com`. Only the first
  5 SHA-1 hex chars leave the JVM. Fail-open on network errors
  (CWE-359), opt-in via `app.hibp.enabled` (default `true` in
  production; Surefire sets it to `false` so tests skip the network
  call).
- **Phase-4c drift detection** — role mutations in `AdminRolesView`
  call `VersionBumper.bump(user)` to increment the per-subject
  `JSentinelVersion`; `JSentinelVersionEnforcerListener` reroutes
  the affected session to `/login` on the next request.
  `PersistentJSentinelVersionStoreProvider` adapts the
  Eclipse-Store-backed `JSentinelVersionStore` for `ServiceLoader`
  so the version counter survives JVM restarts.
- **Fail-fast bootstrap** — `JSentinelBootstrapInitListener` throws
  `IllegalStateException` with a diagnosis message when
  `AuthenticationService` or `AuthorizationService` is absent,
  instead of silently no-opping. Misconfigured wiring surfaces at
  startup, not at the first login attempt.
- **Lazy `UserDirectoryProvider`** — Initialization-on-Demand Holder
  refactor: the persistent directory opens on first access, not at
  classload time. Tests that swap the directory before any access
  still win; SpotBugs `MS_EXPOSE_REP` suppression dropped because
  the field is no longer eagerly exposed.
- `AppStoragePaths` — single source of truth for all storage
  locations (`frameworkStorageDir`, `userDirectoryDir`,
  `bootstrapTokenFile`). Driven by `-Dapp.storage.dir` so Surefire
  redirects tests to `target/test-data` and never touches the
  repo-rooted `./data/` tree.
- Brute-force protection: `AppAuthenticationService` consults
  `LoginAttemptPolicy` for throttling and records success / failure
  on every login attempt.
- Audit pipeline: `JSentinelAuditService` (Eclipse-Store-backed)
  receives `LoginSucceeded`, `LoginFailed`, `LogoutPerformed`,
  `SessionCreated`, `SessionInvalidated`, `RoleAssigned`,
  `RoleRevoked`, `UserCreated`, `UserDeleted`, `SessionStale` events.
- Logout goes through `VaadinLogoutService` — clears the
  `SubjectStore`, closes the Vaadin session, invalidates the HTTP
  session, redirects to `/login`.

### Views

- `MainLayout` — AppLayout shell carrying the brand mark, role-gated
  drawer entries and the locale + theme + auth-action switchers in
  the navbar. Drawer entries use
  `SecuredUi.link(...).requiresPermission(...).hideWhenDenied()` so
  admin entries vanish for non-privileged subjects.
- **`SecurityFeaturesView`** — public landing page that documents the
  security stack as 11 feature cards (audit, sessions, drift
  detection, Argon2id hashing, HIBP, token propagation V00.74, …)
  plus a three-row table showing the BootstrapExtension chain
  (Default / Persistence / Hardening) and a per-layer status row.
- `PushDemoView` — atmos / observer / `@Push` grid showcasing all
  three Vaadin push styles inside the new layout.
- `AppLoginView` — adds an Enter-key login shortcut via
  `Shortcuts.addShortcutListener` + `ComponentUtil.fireEvent`,
  records a `SessionRecord` on success, and forwards to `/setup`
  when the bootstrap state is `BOOTSTRAP_REQUIRED`.
- `SetupView` (`@Route("setup")`) — initial-admin form; consumes the
  bootstrap token and the chosen username/password through
  `PasswordPreflight` (local blocklist + HIBP) before handing off to
  `InitialAdminBootstrapService.createInitialAdmin(...)`.
- `AuditView` (`@RequiresPermission("audit:read")`) — renders the
  audit feed as a sortable grid.
- `SessionsView` (`@RequiresPermission("admin:sessions")`) — extends
  the framework's `SessionManagementView` over the persistent
  `SessionStore`.
- `AdminRolesView` (`@RequiresPermission("admin:roles")`) — list /
  create / delete users, assign / revoke roles. Every mutation
  publishes an audit event and calls `VersionBumper.bump(user)`.
- `AboutView`, `YoutubeView` rewired to use `MainLayout` as parent
  and the new `roles` package (`@VisibleFor(AuthorizationRole.USER)`).

### Frontend / theming

- Custom `my-theme` Lumo theme with brand tokens, plus **dark** and
  **jSentinel** variants selectable through the `ThemeSwitcher` in
  the navbar (preference persists per Vaadin session via
  `SessionPreferencesInitListener`).
- Design-system primitives under `views/ui/`: `TemplateBrand`,
  `BrandMark`, `PageHeader`, `MetricTile`, `FeatureCard`,
  `EmptyState`, `FilterBar`, `LocaleSwitcher`, `ThemeSwitcher`.
- Lumo badge readability fixes
  (`[theme~="badge"][theme~="contrast"|"success"|"error"]`) for the
  dark and jSentinel themes — the upstream Lumo light-mode colours
  were unreadable on the new backgrounds.
- jSentinel-theme text colour overrides (`--lumo-header-text-color`,
  explicit h1–h6 + form-control colours) so headings and form
  labels are not rendered black on a dark brand background.

### i18n

- Custom `AppI18NProvider` registered via the `i18n.provider`
  init-param in `WEB-INF/web.xml` — Vaadin V25 ignores
  `META-INF/services` for `I18NProvider`, so the SPI mechanism the
  predecessor used was a silent no-op.
- `ResourceBundle.Control.getNoFallbackControl(FORMAT_PROPERTIES)`
  defeats the JVM-default-locale `ResourceBundle` fallback trap: a
  missing translation for `Locale.ENGLISH` no longer cascades to
  the JVM default (German on the dev machine), it falls back to the
  base bundle.
- `I18n` static facade + `I18nSupport` mixin: every translation call
  carries an inline fallback string, so missing keys render the
  fallback rather than the raw key.
- Translation bundles under `src/main/resources/vaadin-i18n/`:
  `translations.properties` (EN ground truth) and
  `translations_de.properties` (~180 keys each).
- Runtime locale switching: `LocaleSwitcher` sets the locale via a
  `?lang=` URL parameter and `SessionPreferencesInitListener`
  applies it before any view renders — survives full reload.

### Tooling

- Maven Wrapper regenerated to **Maven 4.0.0-rc-5** (Apache wrapper
  3.3.4, `only-script` variant). The old Takari `maven-wrapper.jar`
  + `MavenWrapperDownloader.java` are gone.
- Java target **JDK 26** (`maven.compiler.release=26`); Vaadin
  25.1.1, Jetty 12.1.8.
- `_mutation-gate` Maven profile runs PIT mutation coverage and the
  `tools/pit-gate.sh` per-package floor checker.
  `tools/pit-baselines.txt` carries the calibrated floors
  (overall 35 %, security packages 68 %–100 %, view packages
  22 %–35 %); recalibrated after the V00.74 bump and the persistent
  drift store landed.
- Surefire `<systemPropertyVariables>` redirects tests away from
  the repo-rooted `./data/` tree
  (`app.storage.dir=target/test-data`) and disables HIBP network
  egress (`app.hibp.enabled=false`).
- `maven-compiler-plugin` `annotationProcessorPaths` keep
  `*-processor` **before** `*-annotations` (path order matters on
  JDK 21+ — reversed = silent no-emit, no `META-INF/services`
  entries written).
- `_shadejar` profile builds a standalone Jetty fat-jar
  (`application.jar`) via nano-vaadin-jetty 04.00.00.
- Vaadin-Maven-plugin-generated frontend tooling
  (`package.json`, `tsconfig.json`, `types.d.ts`, `vite.config.ts`)
  checked into VCS per Vaadin's own recommendation;
  `vite.generated.ts` stays untracked (auto-regenerated).
- Dropped redundant top-level `tools.jackson.core:jackson-core` /
  `jackson-databind` dependencies — both come transitively via
  `flow-server`.
- `com.fasterxml.jackson.core:jackson-annotations:2.21` retained
  **with a load-bearing comment**: Vaadin 25.1.1's transitive
  `tools.jackson.core:jackson-databind:3.1.x` loads
  `com.fasterxml.jackson.annotation.JsonSerializeAs` in its static
  initializer as a migration bridge. Removing the dep crashes
  `vaadin:prepare-frontend` with `NoClassDefFoundError`. Drop when
  a future Vaadin release ships a databind that no longer triggers
  the Jackson 2 fallback.

### Tests

- 196 unit + Browserless tests across views, listeners, providers,
  i18n, bootstrap and security services. Test classes live under
  `junit.com.svenruppert.*` (the PIT test pattern).
- New `PersistentJSentinelVersionStoreProviderTest` exercises the
  `current / increment / reset` delegation and verifies two
  providers share the same Eclipse-Store-backed state.

### Code quality

- SpotBugs clean. `AppUser` record copies the `roles` set via
  `Set.copyOf` in the compact constructor;
  `UserDirectoryProvider` no longer needs an `MS_EXPOSE_REP`
  suppression after the IODH refactor.

### Removed

- `com.svenruppert:security-for-flow:00.50.00` dependency.
- Default `InMemoryJSentinelVersionStore` SPI binding (replaced by
  `PersistentJSentinelVersionStoreProvider` so role revocations
  survive JVM restarts).
- `views/main/GreetService.java`; the old `MainView` placeholder
  replaced by `SecurityFeaturesView` + the new `MainLayout`.
- Old `META-INF/services` entries for
  `com.svenruppert.vaadin.security.authorization.*`.
- Obsolete docker scripts, `deploy.sh` and superseded views from the
  pre-jSentinel layout.
