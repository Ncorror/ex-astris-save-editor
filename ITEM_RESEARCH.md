# Ex Astris item research notes

This file documents the item-name/purpose data used by the editor. It is deliberately conservative: public references describe many Ex Astris items, but they do **not** publish the internal numeric save IDs. Numeric-ID mappings in `app/src/main/assets/items.json` therefore come from the editor's existing catalog/visual identification and are only given richer descriptions when the match is reasonably clear.

## Main public reference

Community terminology/reference table (JP / CN / EN), based on the global v1.1.0 game data:

- https://drmone.hatenablog.com/entry/exAstrisJpCn

It documents, among other things:

- **Astrite** — character-upgrade resource used on the Orbitals screen; generally obtained through combat, quests and exploration.
- **Doron** — general in-game currency used for purchases from merchants.
- **Vitality Amber** — restores 60% Vitality to one ally, with reduced effectiveness from the same healing effect for three turns afterward.
- **Vitaflow Amber** — restores 25% Vitality to the whole party, with the same three-turn repeat-healing penalty.
- **Entropith Triode** — Entropith battery that restores one Ionix gauge.
- **Laylah Kernel** — Entropith upgrade material; the reference notes a finite total obtainable in normal progression.
- Crafting materials such as Bio-debris, Bio-deposit, Jel, Molding Jel, Carboid Shell, Alloid Shell, Fibrous Fur, Matted Fur, Fluorite and Solid Doronite.
- Cooking ingredients such as Fibrous Seed, Fibrous Fruit, Shellwort, Shellwort Mass, Silver/Gold Mirrormoon, Carbonized Fascia/Hyaline, Galactic Masala, Floweed, Craver's Puff, Smoothcap, Stacker Caps, Licosphere, Titami Meat, Honeypot, Sourpot and Bitterpot.

## IDs currently matched with useful descriptions

The current editor catalog includes confident working matches for:

- `10000` — Astrite
- `10001` — Doron
- `11001` — Vitality Amber
- `11002` — Vitaflow Amber
- `12999` — Entropith Triode
- `200000`–`200005`, `200008`, `200009`, `200101`, `200102` — crafting-material entries listed above
- `210000`–`212002` (with gaps) — cooking ingredients listed above
- `9910007` — Laylah Kernel

These are marked `verified: true` in the catalog only to mean that the **name/purpose text has a public reference**. It does not mean the game publisher has published a table pairing those names with the save IDs.

## Known official/community item names still awaiting reliable ID mapping

The same reference lists additional combat consumables:

- Attack Amber
- Burst Amber
- Flux Amber
- Overload Amber
- Hardening Amber
- Stimulant Amber
- Frenzy Amber

It also lists fourteen Entropiths, including Impact, Mender, Lightning, Razor Gale, Triple Bolt, Blaze, Tempest, Barrier, Concussion, Arc Field and Igneous. Several IDs in the editor clearly belong to Entropiths, but their exact name-to-ID mapping is intentionally left preliminary until icons or stronger game-data evidence can distinguish them.

## Current icon coverage

The current project now contains resource images for all 69 pre-v1.5.2 catalog entries, including the Astrite (`10000`) and Doron (`10001`) HUD/resource symbols added in v1.4.4. Icon presence does not by itself prove an item-name mapping; descriptions should still follow the evidence rules above.

## How to improve the mapping with item icons

When real item icons are added, name them:

```text
app/src/main/res/drawable-nodpi/item_<ID>.png
```

For example:

```text
item_10000.png
item_11001.png
item_500000.png
```

Comparing those icons with public screenshots/reference lists is the safest next step for resolving the remaining unknown IDs without inventing names.


## v1.5.2 additions

The user supplied an updated catalog snapshot and icon archive containing 13 IDs not present in v1.5.1. They are added as **unverified** catalog entries using the supplied Russian names/categories. No official-name claim is made for these new IDs until independently verified.

IDs: `12005`, `12007`, `12008`, `12009`, `210012`, `500001`, `710002`, `710004`, `710005`, `720005`, `730002`, `800006`, `9710017`.

The project now contains 82 catalog entries and matching item images for all 82 IDs.
