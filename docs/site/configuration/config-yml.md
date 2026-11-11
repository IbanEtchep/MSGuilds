---
title: config.yml
description: What belongs to this server only, the guild chest switch.
sidebar:
  order: 2
---

`plugins/ArtisanGuilds/config.yml`:

```yaml
config-version: 2
chest:
  enabled: true
```

| Key | Type | Default | Effect |
|---|---|---|---|
| `chest.enabled` | boolean | `true` | `false` on every server but the one hosting the guild chest. The chest lives in that server's memory: two servers writing it would lose or duplicate items. |
