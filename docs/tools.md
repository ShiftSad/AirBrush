# Tools & controls

Every AirBrush tool is driven by clicks, sneaking and scrolling — no GUIs. This
page lists what each tool does in your hand.

!!! note "Creative tools vs. witchcraft items"
    The **pencil**, **eraser** and **palette** are creative/admin tools: using
    them requires the `airbrush.tools.use` permission (op by default), and
    `/color` sets the pencil's color. Everything earned through the witchcraft
    loop — the **hammer**, **amethyst dye**, **quills** and the **cloth** —
    needs no permission at all.

## Pencil & quills

The pencil draws for free; [quills](craftings.md#quills) draw with
[ink](inks.md) and spend it per block of line. Both share the same controls:

- **Right-click** to start drawing, **right-click again** to finish.
- **Left-click** while drawing to cancel the stroke.
- **Sneak + right-click** for straight lines — each click drops a point,
  left-click ends the chain.
- **Sneak + scroll** to change the brush thickness.
- Switching hotbar slots finishes the active stroke.

!!! info "Strokes live on surfaces"
    Drawing needs a block under the crosshair — there is no drawing in mid-air.
    Fast swipes hug the terrain: the line is subdivided and every point falls
    back onto the surface, so it follows steps and corners instead of cutting
    straight through the air. Swiping across a void splits the line — it ends
    at one edge and continues on the other side, never floating in between.
    And since every bit of a stroke is glued to the block it was drawn on,
    **breaking that block erases the strokes on it**.

!!! note "Quill perks"
    Higher quill tiers unlock a thicker maximum brush and finer minimum lines,
    and stretch each point of ink further. The glyph **quality** you crafted the
    quill with sharpens both bonuses. A quill at 1 durability is dry — dip it in
    an [ink cauldron](inks.md) to refill.

## Amethyst Dye (marker)

A non-permanent marker, made by [hammer transmutation](craftings.md#hammer-transmutation).

- **Right-click** to start a glowing amethyst line, **right-click** to finish.
- **Left-click** to cancel — the ink is refunded.
- Ink is durability: it drains with line length and the marker stops (but never
  breaks) at 1 point. Recharge it in an [Amethyst Ink cauldron](inks.md).
- Strokes live for 60 seconds by default, thinning out before they vanish —
  long enough to draw [glyphs](craftings.md#glyph-crafting).

## Eraser & cloth

The eraser removes pencil strokes for free. The **cloth** works the same way but
must be [soaked in solvent](inks.md#solvent), and spends charge for the volume
it wipes.

- **Right-click** to start or stop erasing.
- **Sneak + right-click** to switch between **Area** (everything in the sphere)
  and **Whole stroke** (any touched stroke is removed entirely) modes.
- **Sneak + scroll** to change the eraser size.

## Palette

- **Right-click** to open the floating color picker.
- **Click** to move the selector, **click again** to confirm.
- **Left-click** to close it without picking.

Prefer typing? [`/color`](commands.md#color) accepts names and hex codes.

## Hammer

Crafted at a [crafting table](craftings.md#hammer), the hammer is the engine of
the witchcraft loop:

- **Punch a dropped amethyst shard** to transmute it into an Amethyst Dye
  (1 durability). **Sneak-punch** to convert the whole stack at once.
- **Punch near a finished glyph** with the ingredients dropped inside it to
  activate the craft — see [Glyph crafting](craftings.md#glyph-crafting).
- It also mines at iron tier with increased speed and hits for 9 damage.

## Undoing mistakes

Admins can revert any change with [`/undo`](commands.md#undo) (`airbrush.undo`,
op by default). In survival, mistakes are fixed the survival way: wipe them
with the [cloth](inks.md#solvent), or wait for non-permanent ink to fade.
