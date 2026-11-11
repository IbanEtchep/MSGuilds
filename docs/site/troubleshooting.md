---
title: Troubleshooting
description: What a message means and how to fix it.
sidebar:
  order: 5
---

| Symptom | Cause | Fix |
|---|---|---|
| The start stops on the MSGuilds import | The import failed; nothing was half imported. | Read the error above it (Core's database unreachable, a corrupt row), fix it, restart. |
| `/g bank` does nothing | Vault or an economy plugin is missing. | Install both. |
| `/g` prints help instead of a menu | Artisan has no guild menu loaded (deleted or broken). | **Reset** the menu in the editor. |
| A member cannot kick or invite | Their rank lacks the permission. | Give it in the ranks menu. |
| Items duplicated or lost in the guild chest | Two servers have `chest.enabled: true`. | Keep it on one server ([recipe](../recipes/chest-one-server/)). |
