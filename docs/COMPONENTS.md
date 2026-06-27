# Components & modules

A reference map of every component in **Open Probatum**. The application is a
**single Maven module** (`open-probatum`, packaged as a WAR) on Java 26 +
Vaadin Flow 25.2 + jSentinel 00.75.20. The base package is
`com.svenruppert.openprobatum`; "module" here means a **Java package** — the
codebase is organised by domain package, not by Maven reactor module.

For *how* these parts interact at runtime see the README section
**"How the components fit together"**; this file is the *what* — a per-package
inventory.

## Package map at a glance

```
com.svenruppert.openprobatum
├── (root)            Application, AppServlet, AppShell   — boot & servlet
├── security          AppClock, AppTenant, SecurityHeadersFilter, login listener
│   ├── bootstrap     layered bootstrap SPI (Default → Persistence → Hardening)
│   ├── model         AppUser, UserDirectory (+ persistence)
│   ├── permissions   AppPermission catalogue
│   ├── roles         AuthorizationRole, @VisibleFor, RoleAccessEvaluator
│   ├── services      authentication, authorization, registration, session
│   └── storage       AppStorage (Eclipse-Store root), AppStoragePaths
├── content           ContentStatus lifecycle + ContentAuthorship registry (shared)
├── catalog           Offering / LearningPath / Module / LearningResource
├── access            Entitlement — who may use which offering
├── progress          LearnerProgress — path completion tracking
├── assessment        Question bank, Assessment, Attempt, grading, metrics
├── lab               practical labs + submissions + assessing
├── bundle            offering bundles + entitlement + completion
├── workshop          scheduled workshops + enrolment + attendance
├── coaching          1:1 coaching offers + slots + sessions
├── credential        Credential, Evidence, issuance, validation, governance, PDF/QR
├── i18n              AppI18NProvider + I18nSupport mixin
└── views             all Vaadin views (+ ui, metrics, analytics, main)
```

Most domain packages follow the **repository quartet** convention:
`XRepository` (interface) + `InMemoryXRepository` + `EclipseStoreXRepository` +
`XRepositoryProvider` (Initialization-on-Demand Holder + `setX`/`reset` test
override). Versioned, reviewable content additionally uses the shared
`content` package (status lifecycle + authorship).

---

## Runtime & boot (root package)

| Class | Role |
|---|---|
| `Application` | Standalone Jetty launcher for the `_shadejar` fat-jar mode (`java -jar application.jar`). |
| `AppServlet` | Custom `VaadinServlet`; configured via `WEB-INF/web.xml` (incl. the `i18n.provider` init-param). |
| `AppShell` | `AppShellConfigurator` — viewport, PWA metadata, the `my-theme` Lumo theme, and `@Push` (WebSocket server push). |

## Security & platform (`security.*`)

The trust foundation supplied by the jSentinel-backed template.

**`security`** — cross-cutting platform services:
`AppClock` (the injectable time source every expiry/effective-status check uses),
`AppTenant` (central tenant id, ADR TR-08), `AppLoginListener`,
`SessionAccess`, `SecurityHeaders` + `SecurityHeadersFilter` (response hardening).

**`security.bootstrap`** — the layered startup SPI. `BootstrapExtension` is the
SPI; `DefaultBootstrapExtension` (order 0), `PersistenceBootstrapExtension`
(order 10) and `HardeningBootstrapExtension` (order 20) each override only their
slice. `BootstrapBuilder` / `BootstrapWiring` assemble them; the
`JSentinelBootstrapInitListener` / `JSentinelVersionInitListener` wire them into
Vaadin startup; `JSentinelStorageProvider` + `PersistentJSentinelVersionStoreProvider`
expose the framework store; `AdministratorAccountStoreImpl` backs the first-admin
flow.

**`security.model`** — the user directory. `AppUser` (id, name, role set),
`Credentials`, `StoredUser`, `UserDirectory` interface with
`PersistentUserDirectory` + `InMemoryUserDirectoryPersistence` /
`EclipseStoreUserDirectoryPersistence` + `UserDirectoryProvider`.

**`security.permissions`** — `AppPermission`: the 12-entry permission catalogue
(`app:view`, `author:content`, `author:review`, `lab:assess`, `workshop:run`,
`coaching:author`, `coaching:provide`, `analytics:read`, `credential:manage`,
`audit:read`, `admin:sessions`, `admin:roles`).

**`security.roles`** — `AuthorizationRole` (the seven roles), `@VisibleFor`
(route-level any-of-role annotation) and `RoleAccessEvaluator` (enforces it on
`BeforeEnter`: no subject → login, lacks every role → MainView).

**`security.services`** — `AppAuthenticationService`, `AppAuthorizationService`
(the roles→permissions table — single source for both route and nav gating),
`RegistrationService` (+ `RegistrationResult`), `PasswordPreflight` (HIBP
k-anonymity, fail-open), `SessionStoreProvider`, `SessionVersionResolver`,
`SubjectIdResolverImpl`, `VersionBumper` (bumps a subject so stale sessions
re-evaluate after a role change).

**`security.storage`** — `AppStorage`: the single `JSentinelStoragePair`
(framework store + app store). Its `AppRoot` holds one `ConcurrentHashMap` per
domain type (users, credentials, offerings, entitlements, progress, assessments,
attempts, questions, credentialEvents, contentAuthors, labs, labSubmissions,
bundles, workshops, workshopEnrolments, coachingOffers, coachingSlots).
`AppStoragePaths` is the single source for storage locations.

## Shared content model (`content`)

The spine of every authored, reviewed, versioned artefact.

| Type | Role |
|---|---|
| `ContentStatus` | The lifecycle enum: DRAFT → IN_REVIEW → APPROVED → PUBLISHED → DEPRECATED → ARCHIVED (+ REPLACED), with `canTransitionTo` / `isTerminal`. |
| `ContentAuthorship` | Registry keyed by `lineageId → authorId`, enforcing segregation of duties (an author can't approve their own content). `InMemory*` / `EclipseStore*` + `ContentAuthorshipProvider`. |

Questions, offerings, labs, bundles, workshops and coaching offers all carry a
`ContentStatus` and register authorship here, so a single **Review queue** serves
them all.

## Domain modules

### `catalog` — courses & learning paths
`Offering` (the versioned catalog item) with `OfferingType` and
`OfferingVisibility` (public / registered / code-gated); a `LearningPath` of
`Module`s, each holding `LearningResource`s (`ResourceType`).
`CatalogLifecycleService` drives submit-for-review / approve / publish.
Quartet: `CatalogRepository` + In-Memory/Eclipse-Store + `CatalogRepositoryProvider`.

### `access` — entitlements
`Entitlement` records that a learner may use an offering; `EntitlementReason`
explains why (direct grant, bundle, access code…); `AccessDecision` is the
resolved verdict. `EntitlementService` grants/checks. Quartet present.

### `progress` — learner progress
`LearnerProgress` tracks a learner's advance through a path; `ProgressService`
exposes completion queries (e.g. `isPathComplete`, the trigger for path/bundle
credentials). Quartet present.

### `assessment` — questions & grading
`Question` (versioned content) with `QuestionType` and `Difficulty`;
`QuestionBankService` (authoring), `QuestionFeedback`. `Assessment` is built from
the bank; an `Attempt` records a sitting with its `AssessmentResult`;
`CheckService` runs/grades a sitting. `QualityMetricsService` aggregates pass
rates and bank composition. Three quartets (question / assessment / attempt).

### `lab` — practical evidence
`Lab` (versioned content, with `Difficulty`); `LabService` (authoring).
`LabSubmission` (+ `SubmissionStatus`) is a learner's practical submission;
`LabSubmissionService` holds the **assess edge** (verify/reject under a shared
static lock, minting at most one credential). Two quartets.

### `bundle` — offering bundles
`Bundle` (versioned content grouping member offerings); `BundleService`
(authoring); `BundleAccessService` (granting a bundle entitles every member);
`BundleCompletionService` (the **claim edge** — one `BUNDLE_COMPLETED` credential
when all members are complete). Quartet present.

### `workshop` — scheduled sessions
`Workshop` (versioned content; schedule + capacity + instructor);
`WorkshopService` (authoring). `WorkshopEnrolment` (+ `EnrolmentStatus`:
ENROLLED / ATTENDED / NO_SHOW / CANCELLED); `WorkshopEnrolmentService` holds the
**seat edge** (capacity-safe enrol) and **attendance edge** (attendance mints a
credential). Two quartets.

### `coaching` — 1:1 coaching
`CoachingOffer` (versioned content; the author is recorded as the coach);
`CoachingOfferService`. `CoachingSlot` (+ `BookingStatus`:
OPEN / BOOKED / COMPLETED / CANCELLED); `CoachingSlotService` holds the **booking
edge** (one learner per slot) and **completion edge** (completing mints a
`COACHING_COMPLETED` credential). Two quartets.

### `credential` — the trust output
The richest domain package — what the academy ultimately produces.

| Group | Types |
|---|---|
| Core record | `Credential` (recipient, evidence, status, validity), `CredentialType`, `CredentialStatus`, `EffectiveStatus` (VALID / EXPIRED / REVOKED / SUSPENDED / SUPERSEDED, via `effectiveStatusAt`). |
| Evidence & rules | `Evidence` (typed proof + source id/version), `CredentialRule` (what evidence satisfies a credential). |
| Issuance & audit | `IssuanceService` (mints per evidence type on the atomic edge), `CredentialEvent` (+ quartet) — the app-side audit trail (one ISSUED per issuance). |
| Governance | `CredentialGovernance` (revoke / reissue), `IssuerIdentity`, `Badge`. |
| Validation | `CredentialValidator`, `ValidationResult`, `ValidationOutcome`, `ValidationRateLimiter` (+ provider) — the public verification path. |
| Rendering | `CredentialPdf` (PDFBox certificate), `CredentialQr` (verification QR). |

Quartets: `CredentialRepository` and `CredentialEventRepository`.

## Internationalisation (`i18n`)

`AppI18NProvider` (registered via the `i18n.provider` init-param in `web.xml`,
because Vaadin 25 ignores `META-INF/services` for `I18NProvider`); `I18n` (the
lookup facade); `I18nSupport` (the view mixin exposing `tr(key, fallback)`).
Bundles live in `src/main/resources/vaadin-i18n/translations*.properties`
(EN + DE, British English).

## Presentation (`views.*`)

**`views`** — ~38 Vaadin views, all using `MainLayout` (the `AppLayout` shell
with the permission-gated drawer + locale/theme switchers). Grouped by audience:

| Audience | Views |
|---|---|
| Public / auth | `PublicHomeView`, `AppLoginView`, `RegistrationView`, `SetupView` (first-admin), `ValidationView` (public credential check), `AboutView`, `YoutubeView` |
| Learner | `DashboardView`, `CatalogView`, `OfferingView`, `LearnPathView`, `PracticeView`, `CheckView`, `WalletView`, `LabView`, `BundleView`, `WorkshopView`, `CoachingView` |
| Author | `AuthorView`, `QuestionBankView`, `LabBankView`, `BundleAuthorView`, `WorkshopAuthorView`, `CoachingAuthorView`, `MetricsView` |
| Reviewer / staff | `ReviewView` (review queue), `AssessmentQueueView` (lab assess), `WorkshopAttendanceView`, `CoachingSlotsView`, `CoachingSessionView` |
| Credential manager | `GovernanceView`, `CredentialAuditView` |
| Platform admin | `AuditView`, `SessionsView`, `AdminRolesView`, `OperatorDashboardView` |
| Shared | `MainLayout`, `HomeButton` |

**`views.ui`** — the design system: `TemplateBrand`, `BrandMark`, `PageHeader`,
`MetricTile`, `FeatureCard`, `EmptyState`, `FilterBar`, `GridSupport`,
`LocaleSwitcher`, `ThemeSwitcher`, `SessionPreferencesInitListener`.

**`views.metrics`** — `PackagingMetricsService` (read-only bundle / workshop /
coaching aggregation feeding `MetricsView`).

**`views.analytics`** — `OperatorAnalyticsService` (academy-wide read-only
aggregation) + `OperatorDashboardView` (the operator surface, `analytics:read`).

**`views.main`** — `PushDemoView` (a `@Push` demonstration).

## Build, resources & tooling (outside `src/main/java`)

| Area | What |
|---|---|
| `pom.xml` | The authoritative version reference. Profiles: `production` (optimised frontend), `_java` (Java 26 / ASM), `_shadejar` (standalone fat-jar), `_mutation-gate` (PIT + per-package floors). |
| `src/main/webapp/WEB-INF/web.xml` | Servlet mapping + the critical `i18n.provider` init-param. |
| `src/main/frontend/themes/my-theme/` | Custom Lumo theme + view CSS. |
| `src/main/resources/vaadin-i18n/` | EN + DE translation bundles. |
| `tools/` | `pit-gate.sh` + `pit-baselines.txt` (per-package mutation floors; **currently paused** — see the project notes). |
| `src/test/java/junit/com/svenruppert/...` | No-mocks unit + `BrowserlessTest` view tests, mirroring the production packages. |
