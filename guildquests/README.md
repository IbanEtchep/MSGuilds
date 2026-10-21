# ArtisanGuildQuests

Bridge plugin exposing ArtisanGuilds guilds to MSQuests as a `guild` quest actor type. A guild's
quests are shared by all its members, and any member's progress advances them. Formerly
MSGuildQuests (on MSGuilds).

Neither MSQuests nor ArtisanGuilds depends on the other — this plugin depends on both.

## Building

Built with ArtisanGuilds, from the root of this repository. MSQuests (branch `artisanquests`)
is consumed from the local Maven repo, so publish it first:

```bash
cd <MSQuests>      && ./gradlew publishToMavenLocal -PartisanApiVersion=2.13.0
cd <ArtisanGuilds> && ./gradlew build
```

The plugin jar lands in `guildquests/build/libs/ArtisanGuildQuests-<version>.jar`. Nothing is
shaded — MSQuests and ArtisanGuilds are `compileOnly` and provided at runtime.

## Using it

Set `actor_type: "GUILD"` on a quest group:

```yaml
key: guild_weekly
name: "Quêtes de guilde"
actor_type: "GUILD"
max_active: 3
reset_cron: "0 0 * * 1"
distribution:
  strategy: RANDOM
  amount: 3
  triggers:
    - actor_load
    - period_reset
```

Placeholders available on a guild actor: `%actor_name%`, `%guild_name%`, `%guild_level%`,
`%guild_members%`.

## Guild rankup

A building block for a guild-level ladder (one quest per level, `strategy: SEQUENTIAL`); the
reward raising the level is a command action.

| Type | Kind | Params | Effect |
| --- | --- | --- | --- |
| `guild_bank` | objective | `amount` | Paid out of the guild bank **in one go**, once the balance covers it. Needs the `BANK_WITHDRAW` rank permission. Advanced by the `msquests:contribute <group:key>` menu command, like `deliver_item`. |

Its player messages live in MSQuests' lang file, under `objective.contribute.guild_bank_*`.

## How it stays in sync

MSQuests caches the profile↔actor link and only consults `QuestActor.isMember()` when an actor
or a player profile is loaded. Every membership change therefore has to be pushed back in:

| ArtisanGuilds event | Action |
| --- | --- |
| startup | `loadActor` for every guild |
| `GuildCreateEvent` | `loadActor` |
| `GuildPostDisbandEvent` | `unloadActor` |
| `GuildMemberJoinEvent` | `refreshActorMembership` |
| `GuildMemberLeaveEvent` | `refreshActorMembership` |

Promote/demote/transfer need no handling — they do not change who is a member.

ArtisanGuilds fires these events on every server, for a change made on another one too
(`isRemote()`), on the guild object each server already holds: the actors stay in step across
the network without any reconciliation.
