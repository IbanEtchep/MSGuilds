# ArtisanGuilds

Guilds for Paper servers, built on Artisan: ranks with permissions, a bank (Vault), a home, a chest,
alliances, a guild chat and levels. Formerly MSGuilds, on Core.

Requires Paper 1.21+ and **Artisan with API 1.0.0 or later**. Artisan provides the database (H2,
MySQL or MariaDB, set in `plugins/Artisan/environment.yml`), the menus, the translations and the network
sync.

User docs (install, the move from MSGuilds, settings, commands, placeholders, recipes):
[`docs/site/`](docs/site/), published on [artisanmc.net/docs/guilds](https://artisanmc.net/docs/guilds/) at
each release. This README is the developer's view.

## Layout

| Path | What |
|---|---|
| `plugin/` | The Paper plugin `ArtisanGuilds` (`paper-plugin.yml`, `Artisan` required, `Vault` and `Core` optional) |
| `guildquests/` | `ArtisanGuildQuests`: guilds as an ArtisanQuests actor type (see its README) |
| `src/` | The former MSGuilds plugin, kept for reference; not part of the build |

Module `guilds` in Artisan: the command `/guild` (`/g`), data sources `guilds:viewer`, `guilds:guild`,
`guilds:ranks`, `guilds:rank_permissions`, `guilds:members`, the `guilds:rank_*` actions, the menus and
dialogs of `plugin/src/main/resources/artisan-system/guilds/`, and the settings model
(`module/models/settings.yml`). Placeholders: `%msguilds_<name|level|members|online|balance|rank|owner>%`.

## Build

```bash
# artisan-core-api comes from repo.artisanmc.net/releases (artisanApiVersion in gradle.properties).
# ArtisanGuildQuests needs ArtisanQuests in the local Maven repository:
#   cd <artisan-quests> (branch artisanquests) && ./gradlew publishToMavenLocal -PartisanApiVersion=1.0.0
./gradlew build   # plugin/build/libs/ArtisanGuilds-<version>.jar, guildquests/build/libs/ArtisanGuildQuests-<version>.jar
```
