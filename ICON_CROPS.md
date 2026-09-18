# Item icon crop pass — v1.4.1

The supplied icon archive was keyed by internal item ID. The five in-game inventory screenshots were used as the higher-quality source for a new centered crop pass.

- Output resource naming: `item_<ID>.png`
- Output size: 128 × 128 px
- The ID mapping is preserved from the user's original archive.
- Crops are centered to the inventory slot grid and shifted slightly upward so quantity badges are excluded.
- Missing IDs continue to use the app placeholder automatically.

The original game UI background/circle may remain in the crop; this pass prioritizes preserving the item art without reconstructing or inventing pixels.
