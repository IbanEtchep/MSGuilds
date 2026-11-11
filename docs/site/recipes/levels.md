---
title: Levels that grow the guild
description: Three levels that raise the member cap and open the guild chest.
sidebar:
  order: 1
---

A new guild holds 5 members and no chest; level 1 raises it to 10 with a 3-row chest, level 2 to 20 with
two full pages.

```yaml
# plugins/Artisan/modules/guilds/data/settings.yml
levels:
  0: { xp: 0, max-members: 5,  chest-rows: 0, chest-pages: 0 }
  1: { xp: 1, max-members: 10, chest-rows: 3, chest-pages: 1 }
  2: { xp: 2, max-members: 20, chest-rows: 6, chest-pages: 2 }
```

Test it: `guild admin setlevel Smiths 2` from the console, then `/g chest 2` as a member.
