# Collaboration Guide

This project is intended to be edited by multiple contributors (including different models). Keep changes deterministic and easy to review.

## 1) Read Before Editing

Always read these files first:
1. `README.md`
2. `docs/ARCHITECTURE.md`
3. `docs/release/APK_RELEASE_CENTER_SPEC.md` (when touching Android update/download flow)
4. `docs/api/API_V1.md`
5. `docs/data/DB_SCHEMA.md`
6. `docs/data/REDIS_SCHEMA.md`

## 2) Change Scope Rules

- Prefer small, module-focused changes.
- Keep one PR/commit focused on one module or one technical concern.
- If schema changes are needed, add a new SQL migration file instead of rewriting existing scripts.

## 3) Naming Conventions

- SQL migration scripts: `NNN_<short_description>.sql`
- Docs module files: `docs/modules/XX-<module-name>.md`
- Redis keys: prefix `chef:`

## 4) Contract-First Development

- Update API contract docs before implementing handler logic.
- Update schema docs before adding DB fields.
- Keep request/response fields consistent with DB names where practical.

## 5) Delivery Log

- Every meaningful delivery should append an entry to `docs/DELIVERY_LOG.md`.
- Entry format:
  - Date
  - Owner
  - Scope
  - Files changed
  - Risk / follow-up
