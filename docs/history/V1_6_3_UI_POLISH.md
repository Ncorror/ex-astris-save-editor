# v1.6.3 — mobile catalog UI polish

> **Historical snapshot.** This file describes an earlier project state. For current behavior and instructions, see [`README.md`](../../README.md), [`SETUP.md`](../../SETUP.md), [`TESTING.md`](../../TESTING.md) and [`RELEASE.md`](../../RELEASE.md).

## Scope

This release is UI-only. It keeps the v1.6.2 catalog, item icons, save parser/writer, Root/Shizuku access and bulk-edit safety rules intact.

## Changes

- Category filters remain horizontal to preserve vertical space, but the selected chip is now automatically centered and the strip has horizontal edge fading so scrolling is more obvious.
- Search has a one-tap clear button.
- Protected items show a compact lock marker in the inventory list and an explicit “protected from bulk edits” badge in the edit sheet.
- The Bulk button is disabled whenever the currently visible result set contains no eligible items.
- Quick quantity presets use four equal-width buttons and no longer require a horizontal scroll.
- Long item names can shrink slightly instead of clipping, and alternate names can occupy two lines.
- Item descriptions use more comfortable line spacing.
- Residual lightweight markup is stripped before text is shown.

## Safety

No save-format code, write path, backup logic, Root/Shizuku backend, catalog ID mapping, icon mapping or bulk-edit eligibility metadata is changed.
