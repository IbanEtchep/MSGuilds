package com.github.ibanetchep.artisanguilds;

import net.artisanmc.modules.api.network.NetworkApi;
import net.artisanmc.modules.api.scheduler.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** {@link GuildPlatform} on Paper and Folia, through Artisan's scheduler and network. */
final class PaperPlatform implements GuildPlatform {

    private final TaskScheduler scheduler;
    private final NetworkApi network;

    PaperPlatform(TaskScheduler scheduler, NetworkApi network) {
        this.scheduler = scheduler;
        this.network = network;
    }

    @Override
    public @Nullable Player player(UUID id) {
        return Bukkit.getPlayer(id);
    }

    @Override
    public boolean isOnline(UUID id) {
        return Bukkit.getPlayer(id) != null || network.getPresence().isOnline(id);
    }

    @Override
    public void sendRemote(UUID id, String miniMessage) {
        network.getPresence().sendMessage(id, miniMessage);
    }

    @Override
    public void callEvent(Event event) {
        if (scheduler.isGlobalThread()) Bukkit.getPluginManager().callEvent(event);
        else scheduler.runGlobal(() -> Bukkit.getPluginManager().callEvent(event));
    }

    @Override
    public void global(Runnable task) {
        scheduler.runGlobal(task);
    }

    @Override
    public void later(long ticks, Runnable task) {
        scheduler.runGlobalLater(ticks, task);
    }
}
