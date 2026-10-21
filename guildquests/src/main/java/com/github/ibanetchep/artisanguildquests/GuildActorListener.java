package com.github.ibanetchep.artisanguildquests;

import com.github.ibanetchep.artisanguilds.api.event.GuildCreateEvent;
import com.github.ibanetchep.artisanguilds.api.event.GuildMemberJoinEvent;
import com.github.ibanetchep.artisanguilds.api.event.GuildMemberLeaveEvent;
import com.github.ibanetchep.artisanguilds.api.event.GuildPostDisbandEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * ArtisanGuilds fires these for a change made on another server too ({@code isRemote()}), on the
 * guild object this server already holds: every server keeps its actors in step.
 */
public class GuildActorListener implements Listener {

    private final GuildActorService guildActorService;

    public GuildActorListener(GuildActorService guildActorService) {
        this.guildActorService = guildActorService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGuildCreate(GuildCreateEvent event) {
        guildActorService.load(event.getGuild());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGuildDisband(GuildPostDisbandEvent event) {
        guildActorService.unload(event.getGuild().getId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMemberJoin(GuildMemberJoinEvent event) {
        guildActorService.refreshMembership(event.getGuild());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMemberLeave(GuildMemberLeaveEvent event) {
        guildActorService.refreshMembership(event.getGuild());
    }
}
