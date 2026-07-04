# Changelog

## 1.1.0 — The Witchcraft Update

The creative pen grows a survival soul: forge a hammer, draw glyphs, brew inks
in cauldrons and earn your own drawing tools — no commands, no permissions.

📖 **New wiki:** https://shiftsad.github.io/AirBrush/

### The witchcraft loop

- **Hammer** — a vanilla crafting-table recipe (2 iron blocks, 1 iron ingot,
  2 sticks). Mines at iron tier with bonus speed, hits for 9 damage, and powers
  every craft below.
- **Amethyst Dye** — punch a dropped amethyst shard with the hammer to
  transmute it into a glowing, non-permanent marker. Sneak-punch converts the
  whole stack.
- **Glyphs** — draw a closed **square** or **triangle** with the marker on any
  flat surface, decorate it with modifiers (**focus dot**, **division line**,
  **rays**), drop the ingredients inside and strike it with the hammer. How
  cleanly you draw is graded (crude / decent / good / perfect) and stamped on
  the crafted item.
- **Quills** — the survival pen, in four tiers (feather → gold → diamond →
  netherite). Higher tiers and better glyph quality mean finer lines, longer
  reach and more efficient ink.
- **Cloth** — the survival eraser, crafted from a square glyph and a white
  wool. Soak it in solvent to erase; it dries out as it works.
- **Inks & cauldrons** — throw ingredients into a water cauldron to brew
  Common, Amethyst, Luminous (glows), Indelible (permanent) or Luminous
  Indelible ink — or a Solvent. Tint any ink brew with vanilla dyes, then dip
  your quill to load it. Each dip drains one water level.

### Drawing rework

- **Strokes hug the terrain.** Fast swipes are subdivided and every point
  falls back onto the surface, so lines follow steps and corners instead of
  cutting straight through the air. Swiping across a void splits the stroke —
  nothing ever floats.
- **Strokes are anchored to blocks.** Break the block a stroke was drawn on
  and the stroke breaks with it — undo included.

### Permissions

- New `airbrush.tools.use` (default: op) gates the creative tools — pencil,
  eraser and palette, which also lost their glyph recipes. The entire
  witchcraft loop needs no permission at all.
- `/color` and `/undo` are now admin tools (`airbrush.color` and
  `airbrush.undo` default to op): `/color` drives the pencil, and `/undo`
  reverts any change — survival mistakes are fixed with the cloth instead.
- `/drawitem` now hands out quills (all tiers) and the cloth as well, and
  `/drawitem kit` gives the three creative tools in one go.

### Fixes

- Undoing an erase now restores strokes exactly as they were.
- Straight-line strokes are no longer orphaned when a player disconnects
  mid-session.
- The color picker no longer leaks memory when players log out.
- All the new mechanics respect region-protection plugins (cauldron dips and
  brews included).

### For server owners

- Remember to set `resource-pack.ip` to your server's public address.
- The full documentation — controls, recipes, inks, commands and every config
  key — now lives at the wiki above.
