# Pinned contract source

`iam.yaml` and `_shared.yaml` in this directory are a **verbatim, pinned copy** of the
contracts published by the single source of truth:

- Repository: `code-sena/design-software-shared-contracts`
- Branch: `develop`
- Commit: `65f49be2c66a5564b4d8af7b40d86d0c170e961e`
- Files: `openapi/iam.yaml`, `openapi/_shared.yaml`

Do **not** hand-edit these files. To pick up a contract change:

1. Land the change upstream in `design-software-shared-contracts` (additive changes only;
   a breaking change requires a new ADR first).
2. Re-copy both files here and update the commit reference above.
3. Re-run `mvnw -pl contracts generate-sources` and fix any fallout in the adapters.

The DTOs and API interfaces consumed by `iam-adapters` are generated from these files by
`openapi-generator-maven-plugin` into `target/generated-sources/openapi`; no generated code
is committed.

## Known drift (as of 2026-08-27)

The pinned commit above predates `design-software-shared-contracts@5a41537`
("feat(iam): define IAM OpenAPI contract and domain event schemas"), which is where the
canonical `iam.yaml` actually took its current shape. Fixing issue #94 required backporting
one piece of that drift: `Module.features` here referenced a local-only `FeatureBrief` schema
missing `scope_type`, which the canonical `Feature` schema already carried — HU-IAM-004's
`GET /modules` acceptance test expects `scope_type` on every feature. That one schema was
brought back in line with canonical; the rest of the drift (e.g. `GET /users` returning
`UserSummary` instead of `User`) was left alone because adopting it needs new domain-model
plumbing (`User` has no `createdAt`/`lastLoginAt` yet) unrelated to this fix. A full re-pin
per the steps above is recommended as separate follow-up work.
