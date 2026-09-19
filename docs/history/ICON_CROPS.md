# Item icon crop pass — v1.4.1

> **Historical snapshot.** This file describes an earlier project state. For current behavior and instructions, see [`README.md`](../../README.md), [`SETUP.md`](../../SETUP.md), [`TESTING.md`](../../TESTING.md) and [`RELEASE.md`](../../RELEASE.md).

> Historical first crop-pass note. The final crop set is described by `V1_4_3_ICON_POLISH.md`; v1.4.4 adds currency icons.


The supplied icon archive was keyed by internal item ID. The five in-game inventory screenshots were used as the higher-quality source for a new centered crop pass.

- Output resource naming: `item_<ID>.png`
- Output size: 128 × 128 px
- The ID mapping is preserved from the user's original archive.
- Crops are centered to the inventory slot grid and shifted slightly upward so quantity badges are excluded.
- Missing IDs continue to use the app placeholder automatically.

The original game UI background/circle may remain in the crop; this pass prioritizes preserving the item art without reconstructing or inventing pixels.
