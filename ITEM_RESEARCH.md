# Item metadata and evidence policy

`app/src/main/assets/items.json` is the editor's runtime catalog. The UI ships **Russian and English** item metadata only.

This document explains which sources are trusted for ID mapping, names, icons, categories and edit-safety decisions.

## Evidence priority

### 1. Extracted game tables — numeric ID mapping

The strongest source for `numeric ID -> localization key -> icon key` is the extracted Ex Astris item data used for the 160-entry catalog.

The stage1 extraction contained:

- `itemTable_decoded_records.csv`
- `ex_astris_catalog_160_game_ru.csv`
- `LocalizationTable_Item.csv`
- `LocalizationTable_Magic.csv`
- `item_sprite_map.csv`

These data are preferred over visual guessing because they preserve the game's internal record relationships.

### 2. `item.ab` — icon/sprite evidence

The supplied Unity `item.ab` bundle contains the item icon atlases used for the current icon pack. The atlas/map extraction produced 210 sprite records and two texture atlases.

For the 160 catalog IDs:

- `158` have exact extracted sprite matches;
- `500014` and `500015` do not have dedicated matching sprites in the supplied bundle and remain special/provisional internal records.

### 3. Public/global terminology references

Public terminology sources are used to cross-check readable English names/effects, but they are **not** used alone to invent numeric save-ID mappings.

The main supplementary terminology reference used during the catalog polish is listed in [`RESEARCH_SOURCES.md`](RESEARCH_SOURCES.md).

## Current catalog state

Current catalog size:

```text
160 entries
160 item image resources
43 bulk-editable entries
117 protected entries
```

Current source categories after the v1.6.4 audit:

```text
2  currency
23 consumable
31 material
16 entropite
25 recipe
34 laylah_key
25 quest
4  valuable
```

Unknown IDs present in a save use the `other` fallback at runtime.

See [`docs/CATALOG.md`](docs/CATALOG.md) for schema/category details.

## Bulk-edit safety

Bulk editing is intentionally more conservative than individual editing.

`bulk_editable` in `items.json` is the authority for known catalog entries. Current totals are 43 editable and 117 protected.

Important examples:

- Entropiths are protected from bulk operations.
- Recipes, Laylah-Keys, Quest items and Valuables are protected.
- Loot packs are displayed under Consumables but remain explicitly protected.
- Laylah Kernel (`9910007`) is displayed under Materials but remains explicitly protected.
- Internal hybrid records `500014` and `500015` remain protected.

A category label must never be treated as proof that mass-editing every item in that category is safe; explicit per-ID metadata wins.

## Historical live-save safety evidence

An earlier `safety.md` test set covered the older 82-entry catalog. Those results were useful for establishing the conservative safety model, but they do not automatically prove the behavior of every later-discovered ID.

Later UI/catalog work therefore kept uncertain/unique items protected unless stronger evidence was available.

## Special internal records: 500014 and 500015

The extracted item table contains IDs `500014` and `500015`, but the supplied icon bundle does not expose dedicated matching sprites for the expected hybrid keys, and the normal public/global Entropith list covers 14 ordinary obtainable variants.

For this reason the editor:

- keeps both records visible in the known catalog;
- marks their metadata as provisional/internal;
- protects them from bulk editing;
- does not present the temporary icon/name handling as fully verified normal gameplay data.

## Translation policy

- Russian and English are the only catalog display languages shipped by the editor.
- Chinese/Japanese/Korean source strings may be used only as research input.
- Existing official/global English terminology is preferred where available.
- When no authoritative English prose was available, descriptions were written conservatively from known item class/purpose rather than inventing exact effects.
