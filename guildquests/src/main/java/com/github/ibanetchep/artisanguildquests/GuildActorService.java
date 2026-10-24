package com.github.ibanetchep.artisanguildquests;

import com.github.ibanetchep.artisanguilds.api.ArtisanGuildsApi;
import com.github.ibanetchep.artisanguilds.api.Guild;
import com.github.ibanetchep.msquests.core.quest.actor.QuestActor;
import com.github.ibanetchep.msquests.core.registry.QuestActorRegistry;
import com.github.ibanetchep.msquests.core.service.QuestActorService;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Keeps the set of loaded {@link GuildQuestActor}s in step with ArtisanGuilds.
 *
 * <p>MSQuests only consults {@link QuestActor#isMember(UUID)} when an actor or a profile is
 * loaded, so every membership change has to be pushed back in explicitly.
 */
public class GuildActorService {

    private final QuestActorService questActorService;
    private final QuestActorRegistry questActorRegistry;
    private final ArtisanGuildsApi guilds;

    public GuildActorService(
            QuestActorService questActorService,
            QuestActorRegistry questActorRegistry,
            ArtisanGuildsApi guilds
    ) {
        this.questActorService = questActorService;
        this.questActorRegistry = questActorRegistry;
        this.guilds = guilds;
    }

    /** Loads every existing guild. Guild counts are small enough not to warrant lazy loading. */
    public int loadAll() {
        int loaded = 0;
        for (Guild guild : guilds.getGuilds()) {
            load(guild);
            loaded++;
        }
        return loaded;
    }

    public void load(Guild guild) {
        questActorService.loadActor(new GuildQuestActor(guild));
    }

    public void unload(UUID guildId) {
        questActorService.unloadActor(guildId);
    }

    /**
     * Re-links the guild's actor with the profiles of its current members. Loads the actor first
     * if it is not known yet, which covers a membership change on a guild that was created while
     * this plugin was down.
     */
    public void refreshMembership(Guild guild) {
        GuildQuestActor actor = getActor(guild.getId());

        if (actor == null) {
            load(guild);
            return;
        }

        questActorService.refreshActorMembership(actor);
    }

    public @Nullable GuildQuestActor getActor(UUID guildId) {
        QuestActor actor = questActorRegistry.getActors().get(guildId);
        return actor instanceof GuildQuestActor guildActor ? guildActor : null;
    }
}
