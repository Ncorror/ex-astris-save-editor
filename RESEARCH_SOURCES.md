# Item research sources

This file records the sources used to build and audit the 160-entry item catalog.

## Extracted game data (primary for numeric IDs)

The current catalog was built from the user-supplied Ex Astris game-data extraction, including:

```text
itemTable_decoded_records.csv
ex_astris_catalog_160_game_ru.csv
LocalizationTable_Item.csv
LocalizationTable_Magic.csv
item_sprite_map.csv
```

These files provide the strongest available evidence for numeric ID relationships, localization keys, internal categories and icon keys.

## Game icon bundle (primary for icons)

The user supplied `item.ab`, a UnityFS bundle from the game resources. It was decoded to recover the item icon atlases and individual sprites.

The resulting catalog mapping has exact sprite matches for 158/160 known IDs. IDs `500014` and `500015` remain special cases because dedicated expected hybrid sprites were not present in the supplied bundle.

## Global terminology / effect cross-check

Supplementary terminology reference:

- https://drmone.hatenablog.com/entry/exAstrisJpCn

Used to cross-check readable English terminology/effects for currencies, Ambers, Entropiths, Laylah Kernel, materials, ingredients and other named items.

Limitation: this page does not itself provide the editor's numeric save-ID table, so public terminology is paired to IDs only when the extracted game data supplies the mapping evidence.

## Official English-language availability

- https://apps.apple.com/us/app/ex-astris/id6470642337

Used only to confirm that Ex Astris has an official English localization. Russian editor text is maintained as the project's Russian localization layer.

## Live-device evidence

Real-device testing has been used for:

- opening/discovering the live Ex Astris save;
- successful item quantity writes;
- Root/APatch access;
- Shizuku access;
- backup/restore behavior;
- UI/category checks;
- conservative bulk-edit behavior.

The earlier `safety.md` test set applied to the older 82-entry catalog. It remains historical evidence, not blanket proof for every ID added later.

## Category audit (v1.6.4)

The extracted tables plus terminology cross-check were used to correct the UI taxonomy:

- loot packs are displayed under Consumables;
- Laylah Kernel is displayed under Materials;
- Fossil Chip, Fossil, Dense Doronite and Potbug Shell are Valuables/sell-items;
- internal Tritris records are presented as Laylah-Keys / passage-key diagrams rather than a public-facing Tritris category.

The extracted game table remains the authority for numeric ID mapping.
