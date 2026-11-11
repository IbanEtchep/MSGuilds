---
title: settings.yml
description: The guild messages' language and the levels: XP, members, chest size.
sidebar:
  order: 1
---

`plugins/Artisan/modules/guilds/data/settings.yml` is edited in the editor (the **guilds** folder,
**Settings**), where the levels are a table: one row per level, **Add** appends the next one.

```yaml
language: en
levels:
  0:  { xp: 0,  max-members: 5,  chest-rows: 0, chest-pages: 0 }
  1:  { xp: 1,  max-members: 7,  chest-rows: 0, chest-pages: 0 }
  2:  { xp: 2,  max-members: 7,  chest-rows: 3, chest-pages: 1 }
  4:  { xp: 4,  max-members: 10, chest-rows: 6, chest-pages: 1 }
  6:  { xp: 6,  max-members: 15, chest-rows: 6, chest-pages: 1 }
  7:  { xp: 7,  max-members: 15, chest-rows: 6, chest-pages: 2 }
  8:  { xp: 8,  max-members: 30, chest-rows: 6, chest-pages: 2 }
  10: { xp: 10, max-members: 30, chest-rows: 6, chest-pages: 5 }
```

The shipped file lists every level from 0 to 10.

| Key | Type | Default | Effect |
|---|---|---|---|
| `language` | `en`, `fr` | `en` | The messages' language when the player's is not offered. |
| `levels.<n>.xp` | integer | | XP the guild needs for level `n`. A guild's level is the highest one whose XP it reached. |
| `levels.<n>.max-members` | integer | | Members at most at that level. |
| `levels.<n>.chest-rows` | 1 to 6 | | Rows of each chest page. |
| `levels.<n>.chest-pages` | integer | | Pages of the guild chest; `0`: no chest. |

Levels are earned through `/guild admin setlevel`, run by hand or by a quest reward
([guild level ladder](../../recipes/guild-quests/)); it sets the XP to the level's threshold: one XP point
per level.

Messages are in `plugins/Artisan/modules/guilds/lang/<code>.yml`, overriding the jar's `en.yml` and `fr.yml`.
