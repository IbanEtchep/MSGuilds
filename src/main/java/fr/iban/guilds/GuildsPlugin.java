package fr.iban.guilds;

import fr.iban.bukkitcore.CoreBukkitPlugin;
import fr.iban.guilds.command.GuildCMD;
import fr.iban.guilds.listener.CoreMessageListener;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.bukkit.BukkitCommandHandler;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class GuildsPlugin extends JavaPlugin {

    private GuildsManager guildsManager;
    public static final String GUILD_SYNC_CHANNEL = "GuildSyncChannel";
    public static final String GUILD_PLAYER_SYNC_CHANNEL = "GuildPlayerSyncChannel";
    public static final String GUILD_INVITE_ADD = "AddGuildInviteChannel";
    public static final String GUILD_INVITE_REVOKE = "RevokeGuildInviteSyncChannel";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onEnable() {
        this.guildsManager = new GuildsManager(this);
        registerCommands();
        registerListeners(new CoreMessageListener(this));
    }

    @Override
    public void onDisable() {
        executor.shutdown();
    }

    private void registerListeners(Listener... listeners) {
        PluginManager pm = Bukkit.getPluginManager();
        for (Listener listener : listeners) {
            pm.registerEvents(listener, this);
        }
    }

    private void registerCommands() {
        BukkitCommandHandler commandHandler = BukkitCommandHandler.create(this);
        commandHandler.setLocale(Locale.FRENCH);
        commandHandler.register(new GuildCMD(this));
        commandHandler.registerBrigadier();
    }

    public void runAsyncQueued(Runnable runnable) {
        executor.execute(runnable);
    }

    public GuildsManager getGuildsManager() {
        return guildsManager;
    }
}
