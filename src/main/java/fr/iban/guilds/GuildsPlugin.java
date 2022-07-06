package fr.iban.guilds;

import fr.iban.bukkitcore.CoreBukkitPlugin;
import fr.iban.bukkitcore.commands.CoreCommandHandlerVisitor;
import fr.iban.bukkitcore.manager.BukkitPlayerManager;
import fr.iban.guilds.command.GuildCMD;
import fr.iban.guilds.listener.CoreMessageListener;
import fr.iban.guilds.listener.GuildListeners;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.autocomplete.SuggestionProviderFactory;
import revxrsal.commands.bukkit.BukkitCommandHandler;
import revxrsal.commands.exception.CommandErrorException;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class GuildsPlugin extends JavaPlugin {

    private GuildsManager guildsManager;
    public static final String GUILD_SYNC_CHANNEL = "GuildSyncChannel";
    public static final String GUILD_PLAYER_SYNC_CHANNEL = "GuildPlayerSyncChannel";
    public static final String GUILD_INVITE_ADD = "AddGuildInviteChannel";
    public static final String GUILD_INVITE_REVOKE = "RevokeGuildInviteSyncChannel";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Economy economy;

    @Override
    public void onEnable() {
        this.guildsManager = new GuildsManager(this);
        registerCommands();
        setupEconomy();
        registerListeners(new CoreMessageListener(this), new GuildListeners(this));
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
        commandHandler.accept(CoreBukkitPlugin.getInstance().getCommandHandlerVisitor());

        //Guild resolver
        commandHandler.getAutoCompleter().registerParameterSuggestions(Guild.class, SuggestionProvider.of(guildsManager.getGuildNames()));
        commandHandler.registerValueResolver(Guild.class, context -> {
            String value = context.arguments().pop();
            Guild guild = guildsManager.getGuildByName(value);
            if (guild == null) {
                throw new CommandErrorException("La guilde " + value + " n'existe pas.");
            }
            return guild;
        });

        commandHandler.register(new GuildCMD(this));
        commandHandler.registerBrigadier();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }

    public void runAsyncQueued(Runnable runnable) {
        executor.execute(runnable);
    }

    public GuildsManager getGuildsManager() {
        return guildsManager;
    }
}
