---
title: The guild chest on one server of a network
description: Keep the chest on the survival server only, so items are never duplicated.
sidebar:
  order: 3
---

On a network where several servers share the guild database, only one server may hold the chest.

```yaml
# plugins/ArtisanGuilds/config.yml, on the survival server
chest:
  enabled: true
```

```yaml
# plugins/ArtisanGuilds/config.yml, on every other server
chest:
  enabled: false
```

`/g chest` on the other servers answers "The guild chest is not available on this server."
