# v1.5.3 — Russian/English item descriptions

> **Historical snapshot.** This file describes an earlier project state. For current behavior and instructions, see [`README.md`](../../README.md), [`SETUP.md`](../../SETUP.md), [`TESTING.md`](../../TESTING.md) and [`RELEASE.md`](../../RELEASE.md).

## Goal

Complete the 82-item catalog with useful Russian and English text without inventing unsupported numeric-ID mappings.

## What changed

- Every catalog entry now has `name`, `name_en`, `description`, and `description_en`.
- Only RU/EN is presented to users.
- Public English terminology/effects were translated into Russian for confirmed mappings.
- Unmapped Amber IDs explain Amber-family behavior without assigning a false official name.
- Unmapped Entropith IDs explain Ionix/equipment behavior without assigning a false Entropith effect.
- User-tested edit-safety findings are reflected in conservative descriptions and existing bulk protections.

## Evidence rule

`verified: true` means the catalog's public name/purpose has supporting reference evidence for the working ID mapping. `verified: false` means the editor has a visual/working name or item class but does not claim an official numeric-ID mapping.

## Primary research source

https://drmone.hatenablog.com/entry/exAstrisJpCn
