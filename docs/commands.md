# Commands

AirBrush adds a handful of commands. The table is a quick reference; each command is
explained with examples below.

| Command | Purpose | Permission | Default |
| --- | --- | --- | --- |
| `/color <color>` | Set the pencil color | `airbrush.color` | op |
| `/undo [amount]` | Undo your last strokes | `airbrush.undo` | op |
| `/drawitem <item>` | Give yourself a tool | `airbrush.drawitem` | op |
| *(pencil, eraser, palette in hand)* | Use the creative tools | `airbrush.tools.use` | op |
| `/airbrush reload` | Reload config & language | `airbrush.reload` | op |
| `/glyphtest [particles]` | Analyze nearby glyphs (debug) | `airbrush.debug` | op |

---

## `/color`

Set the colour your pencil draws with. Accepts a **named colour** or a **`#RRGGBB`
hex** value. Named colours auto-complete as you type.

```
/color red
/color light_purple
/color #1B2A4A
```

The `#` is optional — `/color 1b2a4a` works too. An unknown name or malformed hex
returns an error and leaves your colour unchanged.

!!! note
    This sets the **pencil** colour — an admin tool, hence the op-default
    `airbrush.color` permission. To pick a colour visually, use the **palette**
    instead. Quill colour comes from the ink you dip it in — see
    [Inks & cauldrons](inks.md).

## `/undo`

Undo your most recent strokes. With no argument it undoes a single change; pass a
number to undo several at once.

```
/undo        # undo the last change
/undo 5      # undo the last 5 changes
```

The amount must be `1` or greater. If there's nothing left to undo, you'll be told so.

!!! note "Admin tool"
    `/undo` requires `airbrush.undo` (op by default) and reverts **any** change —
    pencil, quill and marker strokes, and even erases. Survival players fix
    mistakes the survival way: with the [cloth](tools.md), or by waiting for
    non-permanent ink to fade.

## `/drawitem`

Give yourself one of the AirBrush tools. Requires the `airbrush.drawitem` permission
(op by default). Valid items:

`kit` · `pencil` · `eraser` · `palette` · `amethyst_dye` · `hammer` · `cloth` ·
`quill` · `quill_gold` · `quill_diamond` · `quill_netherite`

```
/drawitem kit          # pencil + eraser + palette in one go
/drawitem hammer
/drawitem quill_netherite
```

Quills are handed out at full quality. For how the tools are normally crafted, see
[Craftings](craftings.md).

## `/airbrush reload`

Reload `config.properties` and the language files without restarting the server.
Requires `airbrush.reload` (op by default).

```
/airbrush reload
```

See [Configuration](configuration.md) for the settings this applies.

## `/glyphtest`

A debug command for tuning glyphs. It analyzes the marker/quill strokes around you and
reports the detected **shape**, **modifiers** and a **quality** breakdown (closure,
regularity, smoothness, cleanliness). Requires `airbrush.debug` (op by default).

```
/glyphtest             # print the analysis
/glyphtest particles   # also highlight the detected corners and centre
```

Use it while practising glyphs to see exactly what the plugin recognises before you
strike with the hammer.
