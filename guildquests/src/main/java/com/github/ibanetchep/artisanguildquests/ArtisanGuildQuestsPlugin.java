package com.github.ibanetchep.artisanguildquests;

import com.github.ibanetchep.artisanguilds.api.ArtisanGuildsApi;
import com.github.ibanetchep.msquests.bukkit.BukkitQuestsPlugin;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bridges ArtisanGuilds and MSQuests: neither plugin knows about the other, this one depends on
 * both and registers a "guild" quest actor type backed by ArtisanGuilds' guilds.
 */
public class ArtisanGuildQuestsPlugin extends JavaPlugin {

    private GuildActorService guildActorService;

    @Override
    public void onEnable() {
        Plugin questsPlugin = getServer().getPluginManager().getPlugin("MSQuests");
        ArtisanGuildsApi guilds = ArtisanGuildsApi.get();

        if (!(questsPlugin instanceof BukkitQuestsPlugin quests)) {
            getLogger().severe("MSQuests is not available, disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (guilds == null) {
            getLogger().severe("ArtisanGuilds is not available (disabled at start?), disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Only feeds command completion in MSQuests; membership resolution goes through isMember().
        quests.getActorTypeRegistry().registerType(GuildQuestActor.TYPE, GuildQuestActor.class);

        // Before ServerLoadEvent, when MSQuests reads the quest files that use them.
        GuildBankObjective.setGuilds(guilds);
        quests.getObjectiveTypeRegistry().register(GuildBankObjectiveConfig.class, GuildBankObjective.class,
                new ContributionOnlyHandler<>(quests, GuildBankObjectiveConfig.TYPE));

        guildActorService = new GuildActorService(
                quests.getQuestActorService(),
                quests.getQuestActorRegistry(),
                guilds
        );

        int loaded = guildActorService.loadAll();
        getLogger().info("Loaded " + loaded + " guild actors.");

        getServer().getPluginManager().registerEvents(new GuildActorListener(guildActorService), this);
    }

    public GuildActorService getGuildActorService() {
        return guildActorService;
    }
}
