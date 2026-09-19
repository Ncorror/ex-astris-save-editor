# Research sources for item descriptions

The app UI/catalog is Russian + English only. Other languages below are research inputs only.

## Public game terminology/effect reference

- https://drmone.hatenablog.com/entry/exAstrisJpCn

Used for English terminology and effects for Astrite, Doron, Amber consumables, Entropith Triode, Laylah Kernel, crafting materials, cooking ingredients and the public Entropith list.

Important limitation: the page does **not** expose the save editor's numeric item IDs, so it cannot by itself prove that a public item name belongs to a particular internal ID.

## Official product / language confirmation

- https://apps.apple.com/us/app/ex-astris/id6470642337

Confirms Ex Astris ships an official English localization. Russian text in this editor is our own translation/localization layer.

## Live-save editing evidence

The project also uses the user's `safety.md` test notes as empirical editing-safety evidence for the 82 currently discovered save IDs. These tests are separate from public name/effect research: they tell us what quantities were safe to edit, not the official public name of an unknown numeric ID.

## v1.6.4 category audit

The same terminology table was used to correct top-level placement:

- packs are listed under consumables;
- Laylah Kernel is listed under materials as an Entropith upgrade item;
- Fossil Chip, Fossil, Dense Doronite and Potbug Shell are sell-items for Doron;
- Laylah-Key diagrams and passage keys belong to the key/other item group rather than a standalone public "Tritris" category.

The extracted `itemTable` remains the authoritative source for the numeric ID -> localization/icon mapping.
