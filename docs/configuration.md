# Configuration

AirBrush reads its settings from `config.yml` in the plugin's data folder
(`plugins/AirBrush/config.yml`). Keys are nested — `pencil.default-radius` means:

```yaml
pencil:
  default-radius: 0.05
```

Run [`/airbrush reload`](commands.md) to apply changes without restarting the server.

## Raycasting & drawing

| Key | Default | Description |
| --- | --- | --- |
| `max-raycast-length` | `10.0` | Maximum distance (blocks) a stroke can be drawn from the player. |
| `segment-depth` | `0.03` | Spacing between stroke segments. |
| `smooth-iterations` | `2` | How many smoothing passes are applied to a finished stroke. |

## Pencil

| Key | Default | Description |
| --- | --- | --- |
| `pencil.default-radius` | `0.05` | Starting brush radius. |
| `pencil.min-radius` | `0.01` | Smallest selectable brush radius. |
| `pencil.max-radius` | `0.1` | Largest selectable brush radius. |
| `pencil.radius-step` | `0.01` | Increment per sneak-scroll step. |

## Eraser

| Key | Default | Description |
| --- | --- | --- |
| `eraser.default-radius` | `0.5` | Starting eraser radius. |
| `eraser.min-radius` | `0.125` | Smallest selectable eraser radius. |
| `eraser.max-radius` | `4.0` | Largest selectable eraser radius. |
| `eraser.radius-step` | `0.125` | Increment per sneak-scroll step. |

## Marker

| Key | Default | Description |
| --- | --- | --- |
| `marker.ttl-seconds` | `60` | How long a non-permanent marker stroke lives. |
| `marker.fade-seconds` | `10` | How long the stroke spends thinning out before it vanishes. |
| `marker.width` | `0.02` | Marker line width. |
| `marker.blocks-per-durability` | `0.25` | Blocks of line drawn per point of ink. |

## Cauldron & cloth

| Key | Default | Description |
| --- | --- | --- |
| `cauldron.charge-per-dip` | `50` | Ink restored when a quill/marker is dipped. |
| `cauldron.cloth-charge-per-dip` | `50` | Charge restored when a cloth is soaked. |
| `cloth.blocks-per-durability` | `0.15` | Volume a cloth can erase per point of charge. |

## Glyphs

| Key | Default | Description |
| --- | --- | --- |
| `glyph.min-radius` | `0.6` | Smallest glyph the analyzer accepts (blocks). |
| `glyph.max-radius` | `4.0` | Largest glyph the analyzer accepts (blocks). |
| `glyph.analysis-radius` | `8.0` | Search radius for nearby strokes when crafting or running `/glyphtest`. |

## Resource pack

| Key | Default | Description |
| --- | --- | --- |
| `resource-pack.enabled` | `true` | Whether the plugin serves and sends its resource pack. |
| `resource-pack.ip` | `127.0.0.1` | Address the built-in HTTP server advertises to clients. |
| `resource-pack.port` | `8080` | Port the built-in HTTP server listens on. |

!!! warning
    `resource-pack.ip` must be an address your players can actually reach. The
    default `127.0.0.1` only works for a client on the same machine — set it to
    your server's public IP or hostname for remote players.
