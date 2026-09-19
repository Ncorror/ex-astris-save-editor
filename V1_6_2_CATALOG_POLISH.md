# v1.6.2 — catalog terminology polish

## Scope

This release cleans up the bilingual catalog after the 160-ID expansion. The Russian game-derived text remains the primary source for item descriptions, while English names were cross-checked against terminology from the global game where available.

## Key changes

- Removed old visual placeholder names such as `Red crystal in wrapping, variant`, `Bottle crate...`, `Relic...`, and `Scene Tritris...`.
- Corrected the English names of Ambers, recipes, Entropiths, Laylah-Key patterns/passage keys, sellable curiosities and quest items.
- Replaced generic English fallback descriptions for the expanded records with useful English text.
- `500014` and `500015` remain deliberately unverified/protected internal records. The supplied item table contains them, but the public/global terminology lists 14 normal Entropiths and the supplied `item.ab` contains no matching dedicated hybrid sprites.

## Safety

No save parsing, save writing, Shizuku/root access, bulk-edit rules or icon mappings were changed in this release.
