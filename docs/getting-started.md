# Getting started

## Requirements

- A **Paper** server (built against `1.21.4`)
- **Java 21**
- The client-side resource pack (automatically sent by the plugin)

## Installation

Drop the jar into your server's `plugins/` folder and start it up. The plugin
hosts the resource pack itself over a small built-in HTTP server.

!!! tip "Resource pack delivery"
    The built-in HTTP server is configured under the `resource-pack.*` keys in
    [`config.yml`](configuration.md). Make sure the configured IP and port
    are reachable by your players.

## Grab your tools

Get your tools in-game with `/drawitem` (requires the `airbrush.drawitem`
permission, op by default):

```
/drawitem pencil
/drawitem eraser
/drawitem palette
```

Once you've got them, head to [Tools & controls](tools.md) to learn the clicks,
see [Craftings](craftings.md) for how the tools are normally made in survival,
and [Inks & cauldrons](inks.md) for filling your quill.
