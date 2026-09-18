# v1.5.1 — bulk safety, search and save-bar polish

This patch responds to real-device testing of v1.5.0.

## Bulk safety

Mass operations no longer target every inventory record blindly. The catalog now supports `bulk_editable`. Entropiths and current `other`/unique entries are explicitly protected. `All editable` means currency/resources, consumables and materials that are safe for quantity operations.

The bulk preview reports both editable and protected/skipped counts. This makes it visible before confirmation that unique items will not be touched.

## Search

Search is intentionally human-facing: it matches localized and alternate item names only. Numeric IDs remain available in the item metadata (if the Show IDs setting is enabled) and the Add-by-ID workflow remains available, but IDs no longer influence normal search.

## Unsaved changes

The v1.5.0 persistent guard was functionally correct but visually heavy. v1.5.1 uses a compact one-line card with a warning indicator, Undo and a filled primary Save button. The Save tab uses a dot badge instead of repeating the full count.

The compact access card no longer duplicates the Save action when Root/Shizuku is already connected.
