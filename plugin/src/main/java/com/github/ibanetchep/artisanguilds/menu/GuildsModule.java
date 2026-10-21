package com.github.ibanetchep.artisanguilds.menu;

import com.github.ibanetchep.artisanguilds.Guilds;
import com.github.ibanetchep.artisanguilds.service.GuildMessages;
import net.artisanmc.modules.api.ArtisanAPI;
import net.artisanmc.modules.api.ArtisanModule;
import net.artisanmc.modules.api.action.ActionDeclaration;
import net.artisanmc.modules.api.datasource.DataSourceDeclaration;
import net.artisanmc.modules.api.datasource.DataSourceField;
import net.artisanmc.modules.api.model.ModelField;
import net.artisanmc.modules.api.model.ModelFieldType;
import net.artisanmc.modules.api.template.TemplateDeclaration;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ArtisanGuilds inside Artisan:
 * <ul>
 *   <li>the folder {@code plugins/Artisan/modules/guilds/}, listed under the module in the editor;</li>
 *   <li>its system menus and dialogs, kept there by Artisan and opened by id: {@code guilds:ranks}
 *       (from {@code /guild ranks}), {@code guilds:rank_permissions}, {@code guilds:rank_delete_confirm},
 *       {@code guilds:disband_confirm} (from {@code /guild disband}), the dialogs
 *       {@code guilds:rank_create} and {@code guilds:rank_rename};</li>
 *   <li>the data sources {@code guilds:viewer}, {@code guilds:ranks}, {@code guilds:rank_permissions}
 *       and {@code guilds:members} ({@link GuildMenuData});</li>
 *   <li>the actions {@code guilds:rank_*} of the ranks menus ({@link GuildMenuActions}).</li>
 * </ul>
 */
public final class GuildsModule implements ArtisanModule {

    public static final String ID = "guilds";
    /** The module's folder: the {@code guilds} of {@code guilds:ranks}. */
    static final String FOLDER = "guilds";

    /** The shipped menus and dialogs: raise {@link #SYSTEM_VERSION} whenever one of them changes. */
    static final String SYSTEM = "guilds_menus";
    static final int SYSTEM_VERSION = 1;
    static final String SYSTEM_ROOT = "artisan-system/guilds";
    static final List<String> SYSTEM_FILES = List.of(
            "menus/ranks.yaml",
            "menus/rank_permissions.yaml",
            "menus/rank_delete_confirm.yaml",
            "menus/disband_confirm.yaml",
            "dialogs/rank_create.yaml",
            "dialogs/rank_rename.yaml");

    public static final String VIEWER = "guilds:viewer";
    public static final String RANKS = "guilds:ranks";
    public static final String RANK_PERMISSIONS = "guilds:rank_permissions";
    public static final String MEMBERS = "guilds:members";

    public static final String RANK_MOVE = "guilds:rank_move";
    public static final String RANK_DELETE = "guilds:rank_delete";
    public static final String RANK_RENAME = "guilds:rank_rename";
    public static final String RANK_CREATE = "guilds:rank_create";
    public static final String RANK_PERMISSION = "guilds:rank_permission";

    private final GuildMenuData data;
    private final GuildMenuActions actions;
    private final Logger logger;

    public GuildsModule(Guilds guilds, GuildMessages messages, Logger logger) {
        this.data = new GuildMenuData(guilds.registry(), messages);
        this.actions = new GuildMenuActions(guilds.registry(), guilds.ranks(), messages);
        this.logger = logger;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getSchemaVersion() {
        return "guilds/v1";
    }

    @Override
    public void onEnable(ArtisanAPI api) {
        claimFolder(api);
        registerSystemFiles(api);
        registerDataSources(api);
        registerActions(api);
    }

    @Override
    public void onDisable() {
        // Artisan removes what this module registered.
    }

    /** Before the system files: Artisan writes them into the module's folder. */
    private void claimFolder(ArtisanAPI api) {
        try {
            File folder = api.getProjectRoots().moduleFolder(FOLDER, ID, null);
            logger.fine("Module folder: " + folder.getPath());
        } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException e) {
            logger.log(Level.WARNING, "The folder plugins/Artisan/modules/" + FOLDER + " is not available: "
                    + "the guild menus will not open", e);
        }
    }

    private void registerSystemFiles(ArtisanAPI api) {
        try {
            api.getTemplates().register(TemplateDeclaration
                    .fromResources(getClass().getClassLoader(), SYSTEM_ROOT, SYSTEM, SYSTEM_VERSION, SYSTEM_FILES)
                    .title("Guild menus")
                    .system()
                    .build());
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.log(Level.WARNING, "The guild menus are not available", e);
        }
    }

    private void registerDataSources(ArtisanAPI api) {
        api.getDataSources().register(new DataSourceDeclaration(VIEWER, (player, params) -> data.viewer(player), List.of(
                new DataSourceField("in_guild", ModelFieldType.bool(), "The viewer is in a guild"),
                new DataSourceField("guild_name", ModelFieldType.string(), "Their guild's name"),
                new DataSourceField("rank", ModelFieldType.minimessage(), "Their rank's name"),
                new DataSourceField("is_owner", ModelFieldType.bool(), "They own the guild"),
                new DataSourceField("can_manage_ranks", ModelFieldType.bool(), "Owner, MANAGE_RANKS, or guilds.bypass"))));

        api.getDataSources().register(new DataSourceDeclaration(RANKS, (player, params) -> data.ranks(player), List.of(
                new DataSourceField("id", ModelFieldType.string(), "What the guilds:rank_* actions take"),
                new DataSourceField("name", ModelFieldType.minimessage(), null),
                new DataSourceField("position", ModelFieldType.integer(), "1 for the highest rank"),
                new DataSourceField("members", ModelFieldType.integer(), "Members holding it"),
                new DataSourceField("permissions", ModelFieldType.integer(), "Permissions granted"),
                new DataSourceField("top", ModelFieldType.bool(), "The highest rank"),
                new DataSourceField("bottom", ModelFieldType.bool(), "The lowest rank, that of new members"))));

        api.getDataSources().register(new DataSourceDeclaration(RANK_PERMISSIONS, data::rankPermissions, List.of(
                new DataSourceField("key", ModelFieldType.string(), "What guilds:rank_permission takes"),
                new DataSourceField("name", ModelFieldType.string(), "In the viewer's language"),
                new DataSourceField("granted", ModelFieldType.bool(), "Granted to the rank of params.rank"))));

        api.getDataSources().register(new DataSourceDeclaration(MEMBERS, (player, params) -> data.members(player), List.of(
                new DataSourceField("id", ModelFieldType.string(), "The player's UUID"),
                new DataSourceField("name", ModelFieldType.string(), null),
                new DataSourceField("rank", ModelFieldType.minimessage(), "Their rank's name"),
                new DataSourceField("owner", ModelFieldType.bool(), null),
                new DataSourceField("online", ModelFieldType.bool(), "On any server of the network"))));
    }

    private void registerActions(ArtisanAPI api) {
        ModelField rank = new ModelField("rank", ModelFieldType.string(), true)
                .withHelp("Id of a rank of the player's guild. In the shipped menus: {r.id} or {rank}.");
        ModelField name = new ModelField("name", ModelFieldType.minimessage(), true)
                .withHelp("2 to 64 characters. In the shipped dialogs: {name}.");

        api.getActionRegistry().register(new ActionDeclaration(RANK_MOVE, List.of(rank,
                new ModelField("direction", ModelFieldType.enumOf(List.of("up", "down")), true)), null, actions::move));
        api.getActionRegistry().register(new ActionDeclaration(RANK_DELETE,
                List.of(rank), actions::delete));
        api.getActionRegistry().register(new ActionDeclaration(RANK_RENAME,
                List.of(rank, name), null, actions::rename));
        api.getActionRegistry().register(new ActionDeclaration(RANK_CREATE,
                List.of(name), actions::create));
        api.getActionRegistry().register(new ActionDeclaration(RANK_PERMISSION, List.of(rank,
                new ModelField("permission", ModelFieldType.enumOf(GuildMenuData.permissionKeys()), true),
                new ModelField("granted", ModelFieldType.string())
                        .withHelp("true, false, or empty to toggle")), null, actions::permission));
    }
}
