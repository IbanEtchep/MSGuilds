---
title: Commands and permissions
description: /guild and its sub-commands, rank permissions and server permissions.
sidebar:
  order: 3
---

`/guild` has the alias `/g`. It is declared to Artisan: rename it in its command file of
`plugins/Artisan/modules/guilds/commands/`, or in the editor.

| Command | Effect | Example |
|---|---|---|
| `/guild` | Opens your guild's menu. | `/g` |
| `/guild help` | Lists the commands. | |
| `/guild create <name>` | Creates a guild. | `/g create Smiths` |
| `/guild disband [confirm]` | Disbands your guild (owner). | |
| `/guild invite <player>`, `/guild revoke <player>` | Invites, or withdraws an invitation. | `/g invite Alex` |
| `/guild join <guild>`, `/guild leave` | Joins a guild that invited you, leaves yours. | `/g join Smiths` |
| `/guild promote <member>`, `demote <member>`, `kick <member>`, `transfer <member>` | Ranks and ownership. | `/g promote Alex` |
| `/guild ranks`, `/guild members` | The ranks and members menus. | |
| `/guild chat [public\|guild\|ally]` | Switches your chat. Needs `guilds.chat`. | `/g chat guild` |
| `/guild bank balance\|deposit <amount>\|withdraw <amount>` | The guild bank (Vault). | `/g bank deposit 500` |
| `/guild chest [page]` | The guild chest. | `/g chest 2` |
| `/guild home`, `sethome`, `delhome` | The guild home. | |
| `/guild lands` | The guild's lands. | |
| `/guild info [guild]`, `list [page]`, `logs [page] [guild]` | Information, online guilds, the guild log. | `/g info Smiths` |
| `/guild alliance invite\|accept\|remove <guild>` | Alliances. | `/g alliance invite Miners` |
| `/guild reload` | Reloads the configuration and messages. Needs `guilds.reload`. | |
| `/guild admin setlevel <guild> <level>` | Sets a guild's level; also from the console. Needs `guilds.admin.setlevel`. | `guild admin setlevel Smiths 3` |

## Rank permissions

Inside a guild, what a member may do is set per rank in the ranks menu: kick, invite, manage lands,
promote, demote, manage the home, withdraw from the bank, manage alliances, manage ranks, deposit to and
withdraw from the chest.

## Server permissions

| Permission | Default | Allows |
|---|---|---|
| `guilds.chat` | everyone | The guild and alliance chat |
| `guilds.reload` | op | `/guild reload` |
| `guilds.bypass` | op | Join without an invitation, past the member cap, and act as if every rank permission were granted |
| `guilds.admin` | op | Disband or transfer any guild |
| `guilds.admin.setlevel` | op | `/guild admin setlevel` |

## In menus

Data sources `guilds:viewer`, `guilds:guild`, `guilds:ranks`, `guilds:rank_permissions`, `guilds:members`;
actions `guilds:rank_move`, `guilds:rank_delete`, `guilds:rank_rename`, `guilds:rank_create`,
`guilds:rank_permission`, used by the shipped menus.
