---
title: A guild level ladder with quests
description: One guild quest per level, shared by all members, whose reward raises the guild's level.
sidebar:
  order: 2
---

With Artisan Quests and ArtisanGuildQuests installed, a quest group with `actor_type: guild` belongs to the
guild: any member's progress counts. One quest per level, handed out in order, makes a ladder.

```yaml
# <project>/quest_groups/guild_weekly.yaml
schema: quest_group/v1
title: { en: "Guild ranks" }
actor_type: guild
limits: { max_active: 1 }
distribution: { strategy: sequential, amount: 1, triggers: [actor_load, quest_complete] }
quests:
  - key: rankup_1
    title: { en: "Reach level 1" }
    stages:
      - key: mine
        objectives:
          - { key: ore, type: block_break, material: IRON_ORE, amount: 500 }
    actions:
      complete:
        - run_command: { command: "guild admin setlevel %guild_name% 1", as: console }
```

`/guild admin setlevel` runs from the console, so a quest reward can call it. On a guild actor,
`%guild_name%`, `%guild_level%`, `%guild_members%` and `%actor_name%` are filled in. The objective type
`guild_bank` takes an amount from the guild bank in one go, once the balance covers it.
