# Open Probatum — Developer Academy & Credential Platform

Open Probatum is an instance-based developer academy and credential
platform with central, online verification. Learners follow structured
learning paths, pass knowledge checks, and receive a verifiable
credential record that third parties can validate through a single
public verification page — the credential record is the sole source of
truth, with no offline trust and no cryptographic signatures. Each
installation is its own independently branded instance with its own
issuer identity and data store.

The platform is built on a hardened Vaadin Flow 25 + jSentinel
foundation: authentication, role-based access, persistent storage,
audit log, a mutation-tested core and a design system. The domain layer
— learning paths, assessments, credentials and the verification portal
— is delivered release by release, starting with **V00.10.00 — Trust
Core** (see the platform concept and version roadmap).

## Foundation in the box

| Concern | What you get | Where it lives |
|---|---|---|
| Authentication | Username/password, role + permission catalog, drift detection | `security/{services,roles}` |
| First-admin bootstrap | One-time-token flow, persistent token file, `/setup` view | `security/bootstrap`, `views/SetupView` |
| Persistence | Eclipse-Store for users, sessions, audit events | `security/model/PersistentUserDirectory` |
| Audit log | Ring buffer + persistent sink, live `/audit` grid | `views/AuditView` |
| Session admin | Active-session inventory, revoke-on-click | `views/SessionsView` |
| Role admin | Add/remove users + roles, version-bump-on-mutation | `views/AdminRolesView` |
| Mutation tests | PIT + Browserless, per-package coverage floors enforced in CI | `tools/`, `src/test/.../*BrowserlessTest.java` |
| Design system | `TemplateBrand` + theme tokens + reusable components | `views/ui/`, `frontend/themes/my-theme/styles.css` |
| Push demo | Vaadin `@Push` example view | `views/main/PushDemoView` |

## Quick start

```bash
./mvnw                              # default goal = jetty:run on :8080
```

The first start prints a bootstrap token to stdout and writes it to
`./data/jsentinel/bootstrap.token`. Open `http://localhost:8080/login`
— you'll be redirected to `/setup` until you've used the token to
create the first admin.

## Users & roles

Every account holds a **set of roles**; each role grants a **set of
permissions**; views and navigation are gated by permission (a subject's
permissions are the union across its roles, so one person can wear several
hats). The first account — created from the bootstrap token at `/setup` — is a
**Platform Admin**; from there you assign all other users and roles in
**Role administration** (`/roles`, needs `admin:roles`).

### The six roles

| Role | Holds permissions | What this user does |
|---|---|---|
| **Learner** | `app:view` | Registers, browses the **Catalog**, follows learning paths, **practises** and sits assessments, joins **Bundles** / **Workshops**, books **Coaching**, and owns a **credential wallet** (`/wallet`) + in-app credential check. |
| **Author** | `app:view`, `author:content` | Creates and edits content: offerings, paths, modules, resources, **questions**, **labs**, **bundles**, **workshops** and **coaching offers** (the author is recorded as the coach). Sees the per-content **Quality metrics**. |
| **Reviewer** | `app:view`, `author:review`, `lab:assess`, `workshop:run`, `coaching:provide` | The **teaching/quality staff**: reviews + approves authored content in the **Review queue**, verifies/rejects **lab submissions** (Assessment queue), runs **workshops** + records **attendance**, opens **coaching slots** and completes **1:1 sessions** — the acts that mint practical credentials. |
| **Credential Manager** | `app:view`, `credential:manage` | Governs **issued** credentials — revoke / reissue in **Credential governance**, with the **Credential audit** trail. |
| **Platform Admin** | *every* permission (incl. `audit:read`, `admin:sessions`, `admin:roles`, `analytics:read`) | Operates the instance: **Audit log**, **Active sessions**, **Role administration**, the academy-wide **Operator dashboard** — and can perform any of the above. |
| **Verifier** | `app:view` | An *authenticated* verifier. (The **public** credential-validation page needs no account at all — this role is only for signed-in verification workflows.) |

> **Segregation of duties:** an author cannot approve their **own** content —
> review/approval (`author:review`) is a Reviewer act, distinct from authoring
> (`author:content`). For a clean editorial workflow, the Author and the
> Reviewer should be **different people**.

### Permission catalogue (11)

| Permission | Gates |
|---|---|
| `app:view` | Sign in and see the application (held by every role). |
| `author:content` | Authoring surfaces + Quality metrics. |
| `author:review` | Review queue — approve content before publication. |
| `lab:assess` | Verify / reject practical lab submissions. |
| `workshop:run` | Run workshops, record attendance. |
| `coaching:provide` | Open coaching slots, complete 1:1 sessions. |
| `analytics:read` | Academy-wide **Operator dashboard**. |
| `credential:manage` | Credential governance (revoke/reissue) + credential audit. |
| `audit:read` | Security audit log. |
| `admin:sessions` | Active-session inventory + revoke. |
| `admin:roles` | Assign users and roles. |

### The minimum set of users to run the academy end-to-end

| You need | Role(s) | Why |
|---|---|---|
| **1 operator** | Platform Admin | Bootstrap account; assigns all other users, operates the instance. |
| **1 author** | Author | Produces the catalog content. |
| **1 reviewer** | Reviewer | Approves content **and** delivers labs/workshops/coaching. Make this a *different* person from the author (segregation of duties). |
| **1 credential manager** | Credential Manager | Only if you need to revoke/reissue issued credentials (a Platform Admin can also do this). |
| **≥1 learner** | Learner | The end user who learns and earns credentials. |
| *(optional)* verifier | Verifier | Only for signed-in verification flows — public credential checks need no account. |

A single person may combine roles (e.g. a small instance can run with one
Platform Admin doing admin + authoring + review, plus learners) — but keep
**Author ≠ Reviewer** wherever content approval must be independent.

## Build commands

```bash
./mvnw                                              # dev server
./mvnw test                                         # unit + browserless tests
./mvnw -Pproduction package                         # production WAR
./mvnw -P_shadejar -DskipTests package              # standalone Jetty fat-jar
./mvnw -P_mutation-gate \
       org.pitest:pitest-maven:mutationCoverage \
       verify                                       # PIT + enforce coverage floors
./mvnw versions:display-dependency-updates          # dependency audit
```

## Branding an instance (30 minutes)

1. **`src/main/java/com/svenruppert/flow/views/ui/TemplateBrand.java`**
   — change `NAME`, `TAGLINE`, `LANDING_INTRO`, `ICON`. The wordmark,
   navbar, hero copy and document title all pull from here.
2. **`src/main/frontend/themes/my-theme/styles.css`** — change the six
   `--app-brand-*` hex values at the top. Lumo's `--lumo-primary-*` is
   mapped to it, so the entire app retheme follows.
3. **`PublicHomeView.buildFeatureGrid()`** — replace the three
   `FeatureCard`s with what your product actually ships.
4. **`MainLayout.buildDrawer()`** — add or remove drawer sections.
   Role-gated visibility is automatic.

Full design-system docs: [`docs/DESIGN_SYSTEM.md`](docs/DESIGN_SYSTEM.md).

## Architecture at a glance

```
src/main/java/com/svenruppert/flow/
├── Application.java          ← standalone Jetty launcher (fat-jar mode)
├── AppShell.java             ← @Push, theme, viewport
├── AppServlet.java           ← VaadinServlet, error handling
├── security/
│   ├── bootstrap/            ← BootstrapExtension SPI, layered wiring
│   ├── model/                ← AppUser, persistent directory
│   ├── roles/                ← role enum, @VisibleFor evaluator
│   ├── permissions/          ← permission name catalog
│   └── services/             ← auth, version-bump, password preflight
└── views/
    ├── ui/                   ← design system: BrandMark, PageHeader,
    │                            MetricTile, FeatureCard, EmptyState
    ├── MainLayout.java       ← AppLayout shell, role-gated drawer
    ├── PublicHomeView.java   ← landing page (hero + features)
    ├── DashboardView.java    ← post-login metric tiles + activity
    ├── AppLoginView.java     ← jSentinel-backed login form
    ├── SetupView.java        ← first-admin bootstrap form
    ├── AdminRolesView.java   ← user/role admin
    ├── AuditView.java        ← persistent audit grid
    ├── SessionsView.java     ← session inventory
    ├── AboutView.java        ← about / author profile
    └── YoutubeView.java      ← embed example
```

## How the components fit together

The base package is `com.svenruppert.openprobatum`. The platform is one Vaadin
WAR whose parts are wired through three recurring seams: **one persistent store**,
**one authorization gate on every route**, and **one issuance edge** that turns
learner activity into credentials. Understanding those three explains the whole.

### 1. Persistence — two stores, reached through `*Provider`s

All state lives in Eclipse-Store via a single `JSentinelStoragePair`
(`security/storage/AppStorage`): the **framework store** (jSentinel: users,
sessions, audit, version stamps) and the **app store** whose `AppRoot` holds one
`ConcurrentHashMap` per domain type — `credentials`, `offerings`, `entitlements`,
`progress`, `assessments`, `attempts`, `questions`, `credentialEvents`,
`contentAuthors`, `labs`, `labSubmissions`, `bundles`, `workshops`,
`workshopEnrolments`, `coachingOffers`, `coachingSlots`.

No component touches a store directly. Each domain exposes a **repository quartet**
— `interface` + `InMemory*` + `EclipseStore*` + `*Provider` — and everything
resolves the repository through the `*Provider` (Initialization-on-Demand Holder +
a `setX`/`reset` test override). That one seam is why every service is testable
against in-memory repositories with **no mocks**.

### 2. The request & authorization pipeline

```
HTTP → SecurityHeadersFilter → AppServlet (VaadinServlet)
     → jSentinel auth (SubjectStores.currentSubject → AppUser)
     → AppAuthorizationService  (roles ─► permissions table)
     → RoleAccessEvaluator      (enforces @VisibleFor on the target route)
            • no subject        → reroute to /login
            • lacks every role  → reroute to MainView
     → MainLayout               (builds the drawer; each item shown only if the
                                  subject holds that item's permission)
```

A view is gated **twice, by the same model**: `@VisibleFor(roles…)` guards the
route (server-side, enforced by `RoleAccessEvaluator`), and `MainLayout` hides the
nav item unless the subject holds the item's permission. The roles→permissions
table (`AppAuthorizationService`) is the single source for both — see
**[Users & roles](#users--roles)**. The first admin is seeded by the layered
**bootstrap SPI** (next section); roles are then administered live in
`AdminRolesView`, and any role mutation version-bumps the subject so stale
sessions re-evaluate.

### 3. From authoring to a credential (the domain spine)

```
AUTHOR                REVIEWER / staff           LEARNER                CRED. MANAGER
──────                ────────────────           ───────                ─────────────
author content   ─►   review + approve     ─►    entitlement + progress
(ContentStatus:       (SoD: approver ≠           (EntitlementService,
 DRAFT, author         author; → PUBLISHED)       ProgressService)
 recorded in                                          │
 ContentAuthorship)                                   ▼
                      staff act / completion ─►  Evidence  ─►  IssuanceService
                      (lab verified, workshop    (typed,        • one atomic edge
                       attended, coaching         versioned,      (shared static LOCK)
                       completed, assessment      source id)     • saves 1 Credential
                       passed, bundle done)                      • appends 1 ISSUED
                                                                   CredentialEvent
                                                          │
                                                          ▼
                                                   learner Wallet  ──►  public verification
                                                                        (effectiveStatusAt)
                                                                              │
                                                  revoke / reissue  ◄─────────┘
                                                  (GovernanceView) → another CredentialEvent
```

Key interactions on that spine:

- **Versioned reviewed content** is shared by questions, offerings, labs, bundles,
  workshops and coaching offers: the same `ContentStatus` lifecycle
  (DRAFT→IN_REVIEW→APPROVED→PUBLISHED→…) and the `ContentAuthorship` registry that
  enforces **segregation of duties** (an author can't approve their own content)
  drive the single shared **Review queue**.
- **Evidence is the contract** between the academy and a credential: every
  non-manual credential carries a typed `Evidence` (assessment / lab / bundle /
  workshop / coaching) with the **source id + version**, so a credential always
  points back at exactly what earned it.
- **One issuance edge, minted exactly once.** Every mint (lab verify, bundle
  claim, workshop attendance, coaching completion, assessment pass) runs its
  check-then-act inside a **shared static lock** so two concurrent clicks never
  double-mint, and each issuance appends exactly one `ISSUED` `CredentialEvent` —
  the app-side audit trail that `CredentialAuditView` and credential governance
  read back.

### 4. The read side — analytics & audit reuse the same repositories

Nothing on the reporting side keeps its own data: `QualityMetricsService` (per
assessment/lab), `PackagingMetricsService` (bundles/workshops/coaching) and
`OperatorAnalyticsService` (academy-wide) all **aggregate the live repositories
through the same `*Provider`s**, read-only and zero-safe. `AuditView` shows the
security audit log; the **Operator dashboard** shows the credential mix, the
content pipeline and engagement — the same facts, summed.

### 5. Cross-cutting

- **i18n**: `AppI18NProvider` (registered via the `i18n.provider` init-param in
  `web.xml`) + the `I18nSupport` mixin's `tr(key, fallback)`; `MainLayout` carries
  the locale switcher. EN + DE, British English.
- **Theme & push**: `AppShell` sets the Lumo theme + viewport and enables `@Push`;
  the design-system components live in `views/ui`.

## Security layering — three additive layers

The bootstrap pipeline is an SPI (`BootstrapExtension`) that picks up
every layer registered in `META-INF/services`. Each layer overrides
only the slice it cares about; the order is fixed by `order()`.

| Layer | Order | Provides |
|---|---|---|
| Default | 0 | Ring-buffer audit + logging, PBKDF2 hashing |
| Persistence | 10 | Eclipse-Store-backed audit + session store |
| Hardening | 20 | Argon2id hashing, drift-detection wiring |

Adding a fourth layer (MFA, multi-tenant, …) is one new `BootstrapExtension`
implementation + one line in `META-INF/services`. Nothing else changes.

## Mutation-coverage gate

PIT mutation tests have per-package floors that CI enforces — see
[`tools/README.md`](tools/README.md). Current floors:

| Package | Floor |
|---|---|
| `security` | 90 % |
| `security.bootstrap` | 75 % |
| `security.model` | 80 % |
| `security.permissions` | 90 % |
| `security.roles` | 80 % |
| `security.services` | 80 % |
| `views` | 25 % |
| `views.main` | 20 % |
| **overall** | **42 %** |

A failing gate either means real regression (kill the surviving
mutants) or that the team decided to lower a floor — both demand a
written reason in the commit message.

## Hardening you might want to add

Already wired via skills, but not pre-configured here:

- **HIBP password leak check** — flip a flag in the
  hardening skill, `PasswordPreflight` queries `api.pwnedpasswords.com`
  with k-anonymity range.
- **Persistent JSentinelVersionStore** — swap the in-memory drift
  store for the Eclipse-Store-backed one in `META-INF/services`.
- **Multi-tenant** — `TenantId` already threads through audit events
  and sessions; add per-tenant storage partitioning in
  `JSentinelStorageProvider`.

## Issue tracking

* [GitHub Issues](https://github.com/svenruppert/open-probatum/issues)
* [GitHub Projects](https://github.com/svenruppert/open-probatum/projects)

## Vulnerability hunting

Free scanners that integrate as GitHub PR checks — useful even for
personal projects, since they often complement each other:

* [Snyk](https://snyk.io/)
* [OX Security](https://app.ox.security/)
* [FaradaySec](https://faradaysec.com/)

## License

European Union Public Licence 1.2 — see `pom.xml` for the per-file
header that every source must carry.
