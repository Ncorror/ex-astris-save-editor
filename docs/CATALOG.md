# Catalog reference

Runtime catalog:

```text
app/src/main/assets/items.json
```

Item icons:

```text
app/src/main/res/drawable-nodpi/item_<ID>.png
```

## Catalog totals

```text
160 known item records
160 item image resources
43 bulk-editable records
117 protected records
```

## JSON schema

Each known ID is stored under its decimal string key:

```json
{
  "10000": {
    "name": "Астрит",
    "name_en": "Astrite",
    "cat": "currency",
    "description": "...",
    "description_en": "...",
    "verified": true,
    "bulk_editable": true
  }
}
```

Fields:

| Field | Meaning |
| --- | --- |
| `name` | Russian display name |
| `name_en` | English/alternate display name |
| `cat` | Current UI category key |
| `description` | Russian description |
| `description_en` | English description |
| `verified` | Whether the current name/purpose mapping is considered supported by the available evidence |
| `bulk_editable` | Explicit permission for mass quantity operations |

## Current categories

| Key | English UI | Russian UI | Count |
| --- | --- | --- | ---: |
| `currency` | Resources | Ресурсы | 2 |
| `consumable` | Consumables | Расходники | 23 |
| `material` | Materials | Материалы | 31 |
| `entropite` | Entropites | Энтропиты | 16 |
| `recipe` | Recipes | Рецепты | 25 |
| `laylah_key` | Laylah-Keys | Ключи Лайлы | 34 |
| `quest` | Quest items | Задания | 25 |
| `valuable` | Valuables | Ценности | 4 |

Unknown IDs encountered in a save use the runtime `other` fallback.

## Search

Inventory and add-catalog search use item names only:

- active locale name;
- alternate RU/EN name.

Numeric IDs, descriptions and category text are intentionally not search keys.

## Add catalog

The add-item catalog:

1. starts from all 160 known records;
2. removes IDs already present in the opened save;
3. filters the remainder by name search;
4. displays icon, name, category, description and protection state;
5. asks for quantity before insertion.

A protected item may be added/edited individually; “protected” means it is excluded from bulk operations, not that it is read-only.

## Bulk safety

`bulk_editable` is authoritative. Do not infer safety from the category alone.

Current totals:

```text
43 editable
117 protected
```

Notable protected records that sit inside otherwise familiar categories:

- loot packs (`800000–800011`) are displayed under Consumables but protected;
- Laylah Kernel (`9910007`) is displayed under Materials but protected.

## Special records 500014 / 500015

These records exist in the extracted table but lacked dedicated matching sprites in the supplied `item.ab`. They remain visible but protected and explicitly provisional/internal.

## Data sources

Numeric IDs are never guessed. Sources, strongest first:

1. **Extracted game tables** (`itemTable_decoded_records.csv`, `LocalizationTable_Item.csv`, `LocalizationTable_Magic.csv`, `item_sprite_map.csv` and the 160-entry catalog export): the `ID → localization key → icon key` mapping.
2. **The game's `item.ab` bundle**: the icon atlases (210 sprites). 158 of 160 IDs have exact sprite matches; `500014` and `500015` do not.
3. **Public terminology**, used only to cross-check English names and effects, never to assign IDs: <https://drmone.hatenablog.com/entry/exAstrisJpCn>. The official English localization exists (<https://apps.apple.com/us/app/ex-astris/id6470642337>); the Russian text is the project's own.
4. **Real-device tests**: save discovery, quantity writes, Root (APatch) and Shizuku, backup/restore, categories and bulk editing. An earlier live-save test set covered the older 82-entry catalog. It does not prove the behavior of every ID added later, so uncertain or unique items stay protected.

## Translation policy

- Only Russian and English are shipped; Chinese, Japanese and Korean strings are research input only.
- Official English terminology is preferred where it exists.
- Where no authoritative text was available, descriptions were written conservatively from the item's class and purpose instead of inventing exact effects.
