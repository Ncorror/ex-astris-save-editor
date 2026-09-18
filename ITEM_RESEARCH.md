# Ex Astris item research notes

This file documents the evidence policy behind `app/src/main/assets/items.json`. The app itself ships item metadata in **Russian and English only**. Japanese/Chinese text may be consulted as a research source but is translated before it reaches the catalog.

## Public terminology reference

Primary community reference (global v1.1.0 terminology table):

- https://drmone.hatenablog.com/entry/exAstrisJpCn

It provides the game's English terminology and effects for many items, including:

- Astrite — Orbitals/character-upgrade resource.
- Doron — general merchant currency.
- Vitality Amber — restores 60% Vitality to one ally; further Vitality Amber healing is reduced by 70% for 3 turns.
- Vitaflow Amber — restores 25% Vitality to the party; further Vitaflow Amber healing is reduced by 70% for 3 turns.
- Attack Amber, Burst Amber, Flux Amber, Overload Amber, Hardening Amber, Stimulant Amber and Frenzy Amber — additional combat consumables with documented effects.
- Entropith Triode — battery that restores one Ionix gauge.
- Laylah Kernel — Entropith upgrade item.
- Crafting materials: Bio-debris, Bio-deposit, Jel, Molding Jel, Carboid Shell, Alloid Shell, Fibrous Fur, Matted Fur, Fluorite and Solid Doronite.
- Cooking ingredients: Fibrous Seed/Fruit, Shellwort/Mass, Silver/Gold Mirrormoon, Carbonized Fascia/Hyaline, Galactic Masala, Floweed, Craver's Puff, Smoothcap, Stacker Caps, Licosphere, Titami Meat, Honeypot, Sourpot and Bitterpot.
- Fourteen named Entropiths, including EP: Impact, Mender, Lightning, Razor Gale, Triple Bolt, Blaze, Tempest, Barrier, Concussion, Arc Field, Igneous, Gust, Smite and Cluster.

## Important limitation: numeric save IDs

The public reference does **not** publish a table pairing those public names with the numeric IDs used by the save file. Therefore the editor does not guess exact mappings merely because a visual shape or ID range looks plausible.

For example, the catalog contains seven currently-unmapped `120xx` combat-consumable IDs and the public reference lists seven additional Amber types. The counts line up, but that alone is not sufficient evidence to assign Attack/Burst/Flux/etc. to particular numeric IDs. Those entries therefore receive a bilingual Amber-family description while remaining `verified: false`.

The same rule applies to the six observed `500xxx` Entropith IDs: the general Entropith mechanics are known, but their exact public names/effects remain unassigned until stronger icon/game-data evidence is found.

## Live-game edit safety

The supplied `safety.md` records tests on the 82-item catalog. The editor uses those findings conservatively:

- quantities for currencies, `11xxx/12xxx` consumables, `800xxx` masks, common materials/cooking items, `600xxx` entries and Laylah Kernel were tested successfully;
- Entropiths are equipment-like single-copy items and stay protected from bulk editing;
- `971xxxx` relic-like entries and the special `9910001/9910024/9910028` items stay protected;
- bottle-crate/pack IDs (`710xxx/720xxx/730xxx`) were observed as single-copy vendor items with unclear purpose and stay protected;
- zero is not used as a substitute for deletion;
- the game should be fully closed before overwriting its save.

## v1.5.3 bilingual catalog policy

All 82 records now contain:

```text
name             Russian display name
name_en          English display name
description      Russian description
description_en   English description
verified         whether the name/purpose mapping has external support
bulk_editable    optional conservative bulk-edit override
```

When a mapping is unverified, the description still explains the item's known class, observed behavior and edit-safety status without inventing an official name/effect.
