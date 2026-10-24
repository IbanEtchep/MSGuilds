rootProject.name = "ArtisanGuilds"

include("plugin")

project(":plugin").name = "artisan-guilds-plugin"

// ArtisanGuildQuests (formerly MSGuildQuests): guilds as MSQuests actors.
include("guildquests")
project(":guildquests").name = "artisan-guild-quests"
