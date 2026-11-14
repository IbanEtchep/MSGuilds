---
title: Placeholders
description: The player's guild, its level, members and bank.
sidebar:
  order: 4
---

In Artisan texts and in PlaceholderAPI, `%msguilds_<key>%`:

| Key | Example value | Value |
|---|---|---|
| `name` | `Smiths` | The guild's name; a "no guild" text when the player has none |
| `level` | `3` | Its level |
| `members` | `7` | Its members |
| `online` | `2` | Members online |
| `balance` | `12 500` | The bank's balance |
| `rank` | `Officer` | The player's rank |
| `owner` | `Steve` | The owner's name |

Without a guild, every key but `name` is empty.
