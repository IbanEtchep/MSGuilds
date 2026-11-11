---
title: Install
description: Put the jar next to Artisan, start the server, and create a guild.
sidebar:
  order: 1
---

1. Install [Artisan](../../artisan/getting-started/install-admin/) 1.0.0 or newer, and Vault with an
   economy plugin for the bank.
2. Drop `ArtisanGuilds-<version>.jar` into `plugins/`.
3. Start the server. The console shows:

   ```text
   [ArtisanGuilds] Guild tables created (artisan_guilds_*).
   [ArtisanGuilds] 0 guilds loaded (Artisan API 1.0.0).
   ```

4. In game, `/guild create Smiths`, then `/g`: the guild menu opens.

## What the first start writes

| Path | What |
|---|---|
| `plugins/ArtisanGuilds/config.yml` | What belongs to this server only: the [chest](../configuration/config-yml/) |
| `plugins/Artisan/modules/guilds/data/settings.yml` | [Settings](../configuration/settings/): language, levels |
| `plugins/Artisan/modules/guilds/menus/`, `dialogs/` | The guild, members, ranks and confirmation menus, rank dialogs |
| Tables `artisan_guilds_*` | Guilds, ranks, members, logs, alliances, chest |

## Coming from MSGuilds

Remove the MSGuilds jar and keep Core running for the first start. ArtisanGuilds copies MSGuilds'
`config.yml` and edited lang files once, then imports the guilds from Core's database (or from Artisan's
database when MSGuilds' tables are there). The import runs once; an error stops the start so that
nothing is half imported. The language and the levels of the old `config.yml` move to the settings.

`/guild`, `/g`, `guilds.chat` and `guilds.reload` keep their names. The PlaceholderAPI placeholders keep
the `msguilds` prefix.
