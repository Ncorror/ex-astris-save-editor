# v1.6.0 — full catalog + new icon set

> **Historical snapshot.** This file describes an earlier project state. For current behavior and instructions, see [`README.md`](../../README.md), [`SETUP.md`](../../SETUP.md), [`TESTING.md`](../../TESTING.md) and [`RELEASE.md`](../../RELEASE.md).

## Included in this update

- Full 160-entry item catalog generated from the extracted stage1 tables.
- Full replacement of the old mixed icon pack with the new icons extracted from `item.ab`.
- 158 IDs have exact extracted PNG icons.
- IDs `500014` and `500015` temporarily reuse the `500013` meteor-style icon so the UI stays visually consistent until a later dump reveals the exact sprites.

## Category behavior

The editor UI still keeps the simpler five filter groups used by the current build:

- `currency`
- `consumable`
- `material`
- `entropite`
- `other`

The new catalog internally covers extra source categories such as quest items, recipes, packs, curiosities and tritris pieces; these are currently grouped under `other` in the UI so no extra UI rewrite is required in this pass.
