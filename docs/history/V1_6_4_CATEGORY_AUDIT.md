# v1.6.4 — category audit

> **Historical snapshot.** This file describes an earlier project state. For current behavior and instructions, see [`README.md`](../../README.md), [`SETUP.md`](../../SETUP.md), [`TESTING.md`](../../TESTING.md) and [`RELEASE.md`](../../RELEASE.md).

This release corrects catalog placement using the extracted item table and in-game terminology.

## Corrected placement

- `600000`-`600003`: Valuables / Ценности (sellable for Doron), not collectibles.
- `800000`-`800011`: Consumables / Расходники (pack-type consumables), no separate Packs top-level category.
- `9910007`: Materials / Материалы (Laylah Kernel, Entropith upgrade material), not Quest items.
- `970xxxx` and `971xxxx`: Laylah-Keys / Ключи Лайлы instead of the internal development label Tritris.

Bulk-edit safety is intentionally unchanged. Pack items and Laylah Kernel remain explicitly protected.
