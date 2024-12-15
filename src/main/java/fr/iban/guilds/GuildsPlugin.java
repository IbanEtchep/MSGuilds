package fr.iban.guilds;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.impl.PlatformScheduler;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import fr.iban.bukkitcore.CoreBukkitPlugin;
import fr.iban.bukkitcore.manager.MessagingManager;
import fr.iban.guilds.api.GuildManager;
import fr.iban.guilds.api.service.GuildAllianceService;
import fr.iban.guilds.api.service.GuildBankService;
import fr.iban.guilds.api.service.GuildRankService;
import fr.iban.guilds.api.service.GuildService;
import fr.iban.guilds.command.GuildCMD;
import fr.iban.guilds.lang.LangManager;
import fr.iban.guilds.listener.ChatListeners;
import fr.iban.guilds.listener.CoreMessageListener;
import fr.iban.guilds.listener.ServiceListeners;
import fr.iban.guilds.manager.GuildsManagerImpl;
import fr.iban.guilds.model.Guild;
import fr.iban.guilds.placeholderapi.GuildsPlaceholdersExpansion;
import fr.iban.guilds.service.GuildAllianceServiceImpl;
import fr.iban.guilds.service.GuildBankServiceImpl;
import fr.iban.guilds.service.GuildRankServiceImpl;
import fr.iban.guilds.service.GuildServiceImpl;
import fr.iban.guilds.zmenu.ZMenuManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.bukkit.BukkitCommandHandler;
import revxrsal.commands.exception.CommandErrorException;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class GuildsPlugin extends JavaPlugin {

    private static GuildsPlugin instance;
    private GuildManager guildsManager;

    public static final String GUILD_SYNC_CHANNEL = "GuildSyncChannel";
    public static final String GUILD_PLAYER_SYNC_CHANNEL = "GuildPlayerSyncChannel";
    public static final String GUILD_INVITE_ADD = "AddGuildInviteChannel";
    public static final String GUILD_INVITE_REVOKE = "RevokeGuildInviteSyncChannel";
    public static final String GUILD_ALLIANCE_REQUEST = "GuildAllianceRequestChannel";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private Economy econ = null;
    private FoliaLib foliaLib;

    private ZMenuManager zMenuManager;
    private GuildsPlaceholdersExpansion placeholdersExpansion;
    private LangManager langManager;
    private YamlDocument config;
    private GuildService guildService;
    private GuildBankService guildBankService;
    private GuildAllianceService guildAllianceService;
    private GuildRankService guildRankService;

    @Override
    public void onEnable() {
        instance = this;
        foliaLib = new FoliaLib(this);

        loadConfig();

        this.langManager = new LangManager(this);
        langManager.load();

        this.guildsManager = new GuildsManagerImpl(this);
        this.guildService = new GuildServiceImpl(this);
        this.guildBankService = new GuildBankServiceImpl(this);
        this.guildAllianceService = new GuildAllianceServiceImpl(this);
        this.guildRankService = new GuildRankServiceImpl(this);

        ServicesManager servicesManager = getServer().getServicesManager();
        servicesManager.register(GuildManager.class, guildsManager, this, ServicePriority.Normal);
        servicesManager.register(GuildService.class, guildService, this, ServicePriority.Normal);
        servicesManager.register(GuildBankService.class, guildBankService, this, ServicePriority.Normal);
        servicesManager.register(GuildAllianceService.class, guildAllianceService, this, ServicePriority.Normal);
        servicesManager.register(GuildRankService.class, guildRankService, this, ServicePriority.Normal);

        registerCommands();
        setupEconomy();
        registerListeners(
                new CoreMessageListener(this),
                new ServiceListeners(this),
                new ChatListeners(this)
        );

        this.zMenuManager = new ZMenuManager(this);
        zMenuManager.loadZMenu();
        getServer().getPluginManager().registerEvents(zMenuManager, this);

        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
            placeholdersExpansion = new GuildsPlaceholdersExpansion(this);
            placeholdersExpansion.register();
        }
    }

    @Override
    public void onDisable() {
        executor.shutdown();
        zMenuManager.unloadZMenu();
        if(placeholdersExpansion != null){
            placeholdersExpansion.unregister();
        }
    }

    public static GuildsPlugin getInstance() {
        return instance;
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
        commandHandler.getAutoCompleter().registerParameterSuggestions(Guild.class, (args, sender, command) -> guildsManager.getGuildNames());
        commandHandler.registerValueResolver(Guild.class, context -> {
            String value = context.arguments().pop();
            Guild guild = guildsManager.getGuildByName(value);
            if (guild == null) {
                throw new CommandErrorException("La guilde " + value + " n''existe pas.");
            }
            return guild;
        });

        commandHandler.register(new GuildCMD(this));
        commandHandler.registerBrigadier();
    }

    public void runAsyncQueued(Runnable runnable) {
        executor.execute(runnable);
    }

    public void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return;
        }
        econ = rsp.getProvider();
    }

    public void loadConfig() {
        try {
            config = YamlDocument.create(
                    new File(getDataFolder(), "config.yml"),
                    Objects.requireNonNull(getResource("config.yml")),
                    GeneralSettings.builder().setKeyFormat(GeneralSettings.KeyFormat.OBJECT).build(),
                    LoaderSettings.DEFAULT,
                    DumperSettings.DEFAULT,
                    UpdaterSettings.DEFAULT
            );
            config.update();
            config.save();
        } catch (IOException e) {
            getLogger().severe("Failed to load configuration file, disabling plugin.");
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    public YamlDocument getConfiguration() {
        return config;
    }

    public Economy getEconomy() {
        return econ;
    }

    public GuildManager getGuildManager() {
        return guildsManager;
    }

    public ZMenuManager getMenuManager() {
        return zMenuManager;
    }

    public LangManager getLangManager() {
        return langManager;
    }

    public GuildBankService getGuildBankService() {
        return guildBankService;
    }

    public GuildAllianceService getGuildAllianceService() {
        return guildAllianceService;
    }

    public GuildRankService getGuildRankService() {
        return guildRankService;
    }

    public GuildService getGuildService() {
        return guildService;
    }

    public MessagingManager getMessagingManager() {
        return CoreBukkitPlugin.getInstance().getMessagingManager();
    }

    public PlatformScheduler getScheduler() {
        return foliaLib.getScheduler();
    }
}
