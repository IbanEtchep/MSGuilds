package com.github.ibanetchep.artisanguildquests;

import com.github.ibanetchep.artisanguilds.api.Guild;
import com.github.ibanetchep.msquests.core.lang.Translator;
import com.github.ibanetchep.msquests.core.quest.actor.QuestActor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * An ArtisanGuilds guild seen as a MSQuests actor: its quests are shared by every member, and
 * any member's progress advances them.
 *
 * <p>Membership is read straight off the live {@link Guild} rather than copied, so it is never
 * stale. What can go stale is the profile↔actor link MSQuests caches — hence
 * {@code QuestActorService.refreshActorMembership} on every membership change.
 */
public class GuildQuestActor extends QuestActor {

    public static final String TYPE = "guild";

    private final Guild guild;

    public GuildQuestActor(Guild guild) {
        super(guild.getId(), guild.getName());
        this.guild = guild;
    }

    public Guild getGuild() {
        return guild;
    }

    @Override
    public String getActorType() {
        return TYPE;
    }

    @Override
    public boolean isMember(UUID playerId) {
        return guild.getMembers().containsKey(playerId);
    }

    /** The guild can be renamed while loaded; MSQuests copied the name at construction. */
    @Override
    public String getName() {
        return guild.getName();
    }

    @Override
    public Map<String, String> getPlaceholders(Translator translator) {
        Map<String, String> placeholders = new HashMap<>(super.getPlaceholders(translator));
        placeholders.put("actor_name", guild.getName());
        placeholders.put("guild_name", guild.getName());
        placeholders.put("guild_level", String.valueOf(guild.getLevel()));
        placeholders.put("guild_members", String.valueOf(guild.getMembers().size()));
        return placeholders;
    }
}
