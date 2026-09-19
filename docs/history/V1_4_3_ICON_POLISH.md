# v1.4.3 — icon crop polish

> **Historical snapshot.** This file describes an earlier project state. For current behavior and instructions, see [`README.md`](../../README.md), [`SETUP.md`](../../SETUP.md), [`TESTING.md`](../../TESTING.md) and [`RELEASE.md`](../../RELEASE.md).

> Historical icon-polish notes. v1.4.4 later added the two currency icons; v1.5.0 keeps this icon set unchanged.


- Re-cropped all 67 item icons directly from the supplied in-game inventory screenshots using the ID mapping from `item_icons.zip`.
- Standardized crop geometry and scale so icons sit more evenly in the editor.
- Recovered full artwork for items that were visibly clipped in the earlier 128x128 crops (notably several materials, masks, relics, scroll/crystal/gift items).
- Reduced icon ImageView padding from 5/6dp to 2dp and switched to `centerCrop` inside the rounded icon container, making the art larger and more consistent at 52dp and 64dp.
- IDs 10000 and 10001 intentionally keep the placeholder because the supplied icon archive contains 67 files for the other 67 known inventory entries.

The original save parsing, Root/Shizuku backends, quantities, bulk editing and item descriptions are unchanged.
