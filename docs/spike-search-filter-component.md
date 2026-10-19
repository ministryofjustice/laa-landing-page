# SPIKE: Reusable Search & Filter Component for SiLAS — Findings

**Status:** Spike complete
**Scope:** Assessment of the existing Reactivation Requests filter component, audit of other search/filter
implementations across SiLAS, GDS/accessibility review, and a recommendation on building a shared,
reusable Search & Filter component.

Related design: [SiLAS Components (Figma)](https://www.figma.com/design/28rTMX8Rd8YMHigyRMFkqB/SiLAS-Components?node-id=28-3585)

---

## 1. Summary / Recommendation

**Can the Reactivation Requests filter component be reused across SiLAS as-is? No — not without further work.**

The component (`shared-components/search-filter.html`) is already written as a generic Thymeleaf fragment and
is explicitly documented in its own code comments as reusable, but in practice it is **consumed by exactly one
page today** (Reactivation Requests). Every other search/filter experience in SiLAS is a separate, hand-built
implementation that duplicates layout, CSS and JS rather than using the shared fragment.

The underlying GOV.UK/MOJ markup, ARIA wiring and JS conventions for checkbox-based filters are sound and a
good foundation — this is **not** a "start again" situation. However, the component:

- Only supports **search text, boolean checkboxes and checkbox groups**. It has no support for `<select>`
  dropdowns, date filters or autocomplete/typeahead fields — all of which are used elsewhere in SiLAS today.
- Has a **progressive-enhancement/accessibility defect**: the filter panel is rendered `hidden` in the
  server-rendered HTML and is only revealed by JavaScript. If JS fails to load, the filter controls are
  invisible and unreachable, unlike GOV.UK's own `<details>` component which degrades to an open state.
- Requires each adopting page to hand-author both the filter markup (`filterContent`) and the active-filter
  "tag" banner (`activeFiltersContent`) using string-based `data-*` attribute conventions, with **no
  compile-time or runtime validation** that the two are kept in sync.
- Has **no automated accessibility test coverage** — the project's existing pa11y script does not scan the one
  page that uses this component.

**Recommendation:** Evolve the existing component rather than replace it. Invest a hardening phase (see
§6 Rollout approach) to fix the no-JS defect, add `select`/date support, and move to a declarative
filter-definition API, then prove reusability with a second, low-risk pilot page before wider rollout.

---

## 2. Existing solution assessment — Reactivation Requests filter

**Files:**
- Component: `src/main/resources/templates/shared-components/search-filter.html`
- Consumer: `src/main/resources/templates/reactivation-requests.html`
- Controller: `UserActivationController#displayReactivationRequests`
- Service: `UserReactivationRequestService#getPage`

**Architecture:**
- The component is a single Thymeleaf fragment, `searchFilter(formAction, searchHint, filterContent,
  activeFiltersContent, searchHeading, formAriaLabel, filterDefaultClosed)`, combining HTML, inline `<style>`
  and inline `<script>` in one file.
- Callers pass two **inline fragment expressions** (`filterContent`, `activeFiltersContent`) containing their
  own page-specific checkbox markup and active-filter tag banner. The shared fragment only renders the
  surrounding chrome (search box, Apply/Show-filters buttons, collapsible panel, generic JS behaviours).
- Filter state round-trips entirely through the query string. The page's controller also persists the last
  applied filters in the `HttpSession` (`REACTIVATION_FILTERS_SESSION_KEY`) so that a bare "Back" navigation
  replays the user's last filters — this session-replay logic is bespoke to this one controller, not part of
  the shared component.
- Filtering and sorting are performed **in memory in Java** (`filterAndSortRequests(buildRequests(...), ...)`)
  rather than at the database/query level, then paginated in memory. This is a page-specific implementation
  detail, not something the shared UI component dictates, but it means "reusability" of the UI does not imply
  reusability of the filtering performance characteristics — a page with a much larger dataset would need
  DB-level filtering.

**Journey-specific logic that limits reuse:**
- The session-based "replay last filters" redirect behaviour in the controller.
- The two-column inline-flex layout of the User Type / Request Status checkbox groups, hard-coded in the
  consuming page with no responsive breakpoint of its own (the shared component's only responsive CSS targets
  the search input row, not the filter panel body).
- The `selectedRequestStatuses` enum-to-label mapping used to build filter tags.

---

## 3. GDS & accessibility review

**Positive findings:**
- Uses current GOV.UK Frontend (`v5.11.1`) and MOJ Frontend (`v5.1.5`) — a good, up-to-date baseline.
- Correct use of `govuk-fieldset`/`govuk-legend`/`govuk-checkboxes` markup for filter groups.
- The "Show/Hide filters" toggle button correctly manages `aria-expanded` and `aria-controls`, and updates its
  visible label text when toggled.
- Sortable table column headers use `aria-sort` correctly, and the "unsorted" icon uses an empty `alt=""`
  (decorative), which is correct.
- Each active-filter "remove" link has a descriptive `aria-label` (e.g. "Remove Provider Admin filter").
- The project already has a pa11y-based accessibility smoke test (`pa11y-auth-test.js`).

**Issues / risks found:**
1. **No-JS failure (High):** `<div id="filter-panel" hidden ...>` is rendered hidden by the server; only the
   inline `<script>` removes the `hidden` attribute. Users without JavaScript (or if the script fails to load)
   cannot see or reach any filter controls at all. This differs from, and is less resilient than, GOV.UK's own
   `<details>`/progressive-disclosure pattern, which fails open rather than fails closed.
2. **No accessibility test coverage (Medium):** `pa11y-auth-test.js` only scans `/`, `/home`, `/users` and
   `/users/manage/{id}`. It does not include `/users/reactivation-requests`, so the one real-world consumer of
   this component has never been automatically scanned for accessibility issues.
3. **Auto-submit-on-change inconsistency (Medium):** The shared component always requires an explicit "Apply
   filters" button click. Two other SiLAS pages (User Access Audit Table, SiLAS Administration) auto-submit
   the form on a `<select>`'s `change` event with no warning. Mixing both interaction models across the app is
   inconsistent, and unannounced auto-submission on input risks WCAG 3.2.2 (On Input) if rolled into a shared
   pattern without a decision on which model to standardise on.
4. **Responsive layout is caller-owned (Low/Medium):** The shared CSS only adds a mobile breakpoint for the
   search input row. The two-column checkbox layout used in Reactivation Requests is hard-coded inline by the
   consuming page with no equivalent small-screen behaviour, so every adopting page must re-solve
   responsiveness itself.
5. **Minor redundancy (Low):** Sort icons carry `alt="Sorted ascending"/"Sorted descending"` text in addition
   to the `aria-sort` attribute already present on the parent `<th>`, which may cause duplicate announcements
   for some screen readers — not blocking, but worth tidying if the sortable-table pattern is folded into a
   shared component later.

---

## 4. Use case analysis — existing search/filter implementations across SiLAS

| # | Page | Implementation | Search text | Boolean checkbox | Multi-select checkbox group | Select dropdown | Date filter | Autocomplete/typeahead | Active-filter tags | Submit model |
|---|------|------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|---|
| 1 | Reactivation Requests (`reactivation-requests.html`) | **Shared component** | Yes | Yes | Yes | – | – | – | Yes | Apply button |
| 2 | Manage Users (`users.html`) | Bespoke | Yes (firm name/code) | Yes | – | – | – | Yes (firm) | – | Apply button |
| 3 | User Access Audit Table (`user-audit/users.html`) | Bespoke | Yes | – | – | Yes (app, role, user type) | Yes (inactive since) | – | – | **Auto-submit on change** |
| 4 | Deleted Users (`user-audit/deleted-users.html`) | Bespoke (minimal) | Yes | – | – | – | – | – | – | Apply button |
| 5 | Firm Directory search (`firm-directory/search-page.html`) | Bespoke | Yes (firm name/code) | – | – | Yes (firm type) | – | Yes (firm) | – | Apply button |
| 6 | SiLAS Administration (Roles / App access / Role-assignment restrictions tabs) (`silas-administration/administration.html`) | Bespoke | – | – | – | Yes (app, user type, per tab) | – | – | – | **Auto-submit on change** |

**Observations:**
- Free-text search appears in 4 of 6 implementations and is the most consistent pattern.
- Only Reactivation Requests has a multi-select checkbox group and an active-filter tag banner — both are
  currently unique to the shared component's one real consumer.
- `<select>` dropdown filtering is used in 3 of 6 pages, and date filtering and autocomplete in others — none
  of these are supported by the shared component today.
- Two pages (#3 and #6) auto-submit on every filter change rather than requiring an explicit action, an
  interaction pattern not present in the shared component at all.
- Every non-Reactivation-Requests page re-implements its own search-card CSS, form wiring and (where present)
  sort/direction hidden fields from scratch.

---

## 5. Reusability & configuration assessment

| Capability | Supported today? | Notes |
|---|---|---|
| Search text | Yes | Built into the shared fragment. |
| Single-select filters (dropdown/radio) | No | No markup, styling or active-filter-tag convention exists for this filter type. |
| Multi-select filters (checkbox group) | Yes | Proven via `data-remove-array-param`/`data-remove-array-value` convention (`selectedRequestStatuses`). |
| Checkbox filters (boolean) | Yes | Proven via `data-remove-filter` convention (`showProviderUsers`, etc.). |
| Date-based filters | No | Not implemented; the one real date filter in SiLAS (`inactiveSinceDate`) was built entirely outside this component. |
| Combined filtering scenarios | Partially | Reactivation Requests combines free text + boolean + multi-select checkboxes, with sorting and pagination, successfully. |

**Can it be extracted into a shared component?** It already is, physically (a Thymeleaf fragment any page can
`th:replace`), but it has only been validated against one filter shape. Extraction is not the open question —
**generalising it to the other filter types already in production use is.**

**Can it be made configurable through props/configuration?** Only partially. Today "configuration" means the
caller supplies two hand-written HTML fragments plus a handful of named parameters; it is not a declarative,
data-driven API (e.g. a list of filter definitions). This is workable for one or two adopters but increases
the risk of drift and silent breakage (e.g. a developer renames a checkbox `name` without updating the
matching `data-remove-filter`/`data-remove-array-param` value, which fails silently at runtime with no
compiler or test to catch it).

**Can it be used without feature-specific code changes?** No. Every adopting page still has to write its own
filter markup, active-filter banner markup, controller `@RequestParam` bindings, and filtering/sorting/pagination
logic (whether in-memory or DB-level). Only the panel "chrome" (search box, toggle button, generic
removal/clear-filters JS, and query-string builder) is genuinely shared today.

---

## 6. Proposed technical architecture & rollout approach

### Proposed direction
1. **Fix the no-JS accessibility defect first** — render the filter panel open by default and use JS only to
   add the collapse/toggle affordance (mirroring GOV.UK's own `<details>` resilience pattern), rather than
   hiding it server-side pending JS.
2. **Move the inline `<script>` into a versioned static JS asset** (alongside the existing `url-utils.js`)
   with its own unit tests, instead of embedding behaviour logic inside the Thymeleaf fragment.
3. **Introduce a declarative filter-definition API** — a list of filter descriptors (`id`, `label`, `type:
   text | checkbox | checkbox-group | select | date`, param name(s), options) passed into the fragment, from
   which both the filter inputs *and* the active-filter tag banner are generated automatically. This removes
   the current duplication/drift risk between `filterContent` and `activeFiltersContent`.
4. **Add `select` and `date` filter types** with matching active-filter-tag and removal conventions, since
   these are already in live use elsewhere in SiLAS and are blocking adoption by the Audit Table, Firm
   Directory and SiLAS Administration pages.
5. **Keep data access out of the component.** The component should only need "current filter state" + "filter
   option metadata" to render; each page's controller/service continues to own whether filtering/sorting is
   done in memory or via a DB-level query (e.g. JPA `Specification`), so the UI layer doesn't assume a
   performance model that won't scale to larger datasets such as the full Users list or Audit log.
6. **Resolve the auto-submit vs. explicit "Apply" button inconsistency** as a UX/design decision (in
   conjunction with the Figma spec, since it is expected to iterate against a working build), rather than
   silently supporting both.
7. **Extend pa11y/automated accessibility coverage** to every page that adopts the shared component, starting
   by adding `/users/reactivation-requests` to the existing `pa11y-auth-test.js` URL list.
8. **Publish developer guidance** — a short README alongside `shared-components/search-filter.html` (or a docs
   page) describing the filter-definition config shape, caller responsibilities, and current known limitations.

### High-level rollout phases
| Phase | Activity |
|---|---|
| 0 | This spike (assessment, inventory, recommendation) — **complete** |
| 1 | Harden the component: fix no-JS panel defect, extract/test JS, add `select` + `date` filter types, add pa11y coverage for Reactivation Requests |
| 2 | Pilot a second, low-complexity adoption — **Deleted Users** (search-only today) is the lowest-risk real test of the generalised contract |
| 3 | Migrate medium-complexity pages needing `select`/autocomplete support (Manage Users, Firm Directory search) |
| 4 | Migrate auto-submit-driven pages (User Access Audit Table, SiLAS Administration) once the submit-model UX decision is made |

Effort for phases 2–4 is indicative only at spike stage and should be re-estimated with the delivery team once
Phase 1 scope is agreed, since each migration also requires reworking that page's controller/service
filtering logic, not just swapping in new markup.

---

## 7. Answers to the spike's questions

1. **Can the Reactivation Requests filter component be reused across SiLAS?** Not as-is. The chrome
   (search box, toggle, checkbox/boolean active-filter conventions) can be, but it has only ever been proven
   against one page and is missing support for filter types (`select`, date, autocomplete) already used
   elsewhere in the app.
2. **What changes are required to make it reusable?** Fix the no-JS accessibility defect, add `select`/date
   filter type support, move to a declarative filter-definition API, extract/test the JS, and decide on a
   single submit-interaction model (button vs. auto-submit).
3. **Does the implementation meet GDS and accessibility standards?** Mostly, for the parts it supports
   (correct GOV.UK checkbox/fieldset markup, ARIA on the toggle and sortable headers, current GOV.UK/MOJ
   Frontend versions). The no-JS panel-visibility defect and lack of pa11y coverage on its one consuming page
   are the two standards gaps that should be fixed before wider rollout.
4. **What filter capabilities should be supported?** Search text, boolean checkbox, multi-select checkbox
   group (all proven today), plus `select` dropdown and date filters (needed to cover the other five SiLAS
   use cases found in this audit) and, longer-term, autocomplete/typeahead (used by two pages today).
5. **How should developers configure and implement the component?** Currently: supply two Thymeleaf fragment
   expressions plus a few named parameters. Recommended future state: supply a single list of filter
   definitions and let the component render both the inputs and the active-filter banner consistently.
6. **What effort would be required to roll the solution out across SiLAS?** Not quantified at spike level;
   recommend a phased rollout (see §6) starting with a hardening phase, then a single low-risk pilot (Deleted
   Users) before committing effort to the remaining four pages.
7. **Are there any technical, UX or accessibility risks?** Yes — see §3 (no-JS defect, missing pa11y coverage,
   auto-submit vs. button inconsistency) and §5/§6 (in-memory filtering not proven at scale, string-based
   `data-*` conventions with no validation, and the Figma design still iterating, meaning engineering
   investment ahead of design stabilisation risks rework).

---

## 8. Acceptance criteria check

- [x] Existing Reactivation Requests filter implementation has been reviewed (§2).
- [x] Existing search and filter use cases across SiLAS have been identified (§4).
- [x] GDS and accessibility considerations have been assessed (§3).
- [x] Reusability constraints and required changes have been documented (§5, §6).
- [x] Recommendation provided on whether to progress with a reusable component (§1).
- [x] Proposed architecture and configuration approach documented (§6).
- [x] Estimated next steps and implementation effort identified (§6 phased rollout; detailed effort estimation
      deferred to delivery planning, as noted).
