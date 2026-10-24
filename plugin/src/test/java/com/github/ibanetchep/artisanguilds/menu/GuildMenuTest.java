package com.github.ibanetchep.artisanguilds.menu;

import com.github.ibanetchep.artisanguilds.Fixture;
import com.github.ibanetchep.artisanguilds.settings.GuildSettings;
import net.artisanmc.modules.api.model.ModelRegistry;
import net.artisanmc.modules.api.screen.EditorScreenDeclaration;
import net.artisanmc.modules.api.screen.EditorScreenRegistry;
import com.github.ibanetchep.artisanguilds.Guilds;
import com.github.ibanetchep.artisanguilds.api.GuildPermission;
import com.github.ibanetchep.artisanguilds.command.GuildCommands;
import com.github.ibanetchep.artisanguilds.model.GuildData;
import com.github.ibanetchep.artisanguilds.model.RankData;
import com.github.ibanetchep.artisanguilds.service.AllianceService;
import com.github.ibanetchep.artisanguilds.service.Msg;
import com.github.ibanetchep.artisanguilds.service.RankService;
import net.artisanmc.modules.api.ArtisanAPI;
import net.artisanmc.modules.api.action.ActionContext;
import net.artisanmc.modules.api.action.ActionRegistry;
import net.artisanmc.modules.api.action.ActionRuntime;
import net.artisanmc.modules.api.datasource.DataSourceRegistry;
import net.artisanmc.modules.api.project.ProjectRootRegistry;
import net.artisanmc.modules.api.template.TemplateDeclaration;
import net.artisanmc.modules.api.template.TemplateRegistry;
import net.artisanmc.modules.api.text.TextApi;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GuildMenuTest {

    private Fixture f;
    private Guilds guilds;
    private GuildMenuData data;
    private GuildMenuActions actions;
    private Player owner;
    private GuildData guild;

    @BeforeEach
    void setUp() throws Exception {
        f = new Fixture();
        guilds = new Guilds(f.registry, f.store, f.leveling, f.sync, f.guilds, f.bank,
                new RankService(f.registry, f.store, f.messages, TextApi.NOOP),
                new AllianceService(f.registry, f.store, f.messages, f.network, f.sync), f.chest);
        data = new GuildMenuData(f.registry, f.leveling, f.messages);
        actions = new GuildMenuActions(f.registry, guilds.ranks(), f.messages);
        owner = f.player("Iban");
        f.guilds.createGuild(owner, "Alpha");
        guild = f.registry.byName("Alpha");
    }

    /** Invited then joined, at the lowest rank. */
    private Player member(GuildData guild, String name) {
        Player player = f.player(name);
        guild.invites().add(player.getUniqueId());
        f.guilds.joinGuild(player, guild);
        return player;
    }

    private RankData top() {
        return guild.getRanks().getLast();
    }

    private RankData bottom() {
        return guild.getRanks().getFirst();
    }

    // --- data sources ---

    @Test
    void viewerTellsWhoMayManageTheRanks() {
        Player member = member(guild, "Bob");
        Player outsider = f.player("Eve");

        Map<String, Object> own = data.viewer(owner).getFirst();
        assertEquals(true, own.get("in_guild"));
        assertEquals("Alpha", own.get("guild_name"));
        assertEquals(true, own.get("is_owner"));
        assertEquals(true, own.get("can_manage_ranks"));

        assertEquals(false, data.viewer(member).getFirst().get("can_manage_ranks"));
        assertEquals(false, data.viewer(outsider).getFirst().get("in_guild"));
        assertEquals(true, data.viewer(null).getFirst().get("can_manage_ranks"), "the editor's preview shows the managed menu");
    }

    @Test
    void guildRowShowsTheLevelAndItsChestToMembersOnly() {
        Map<String, Object> row = data.guild(owner).getFirst();
        assertEquals("Alpha", row.get("name"));
        assertEquals(1, row.get("level"));
        assertEquals(3, row.get("max_level"));
        assertEquals(1, row.get("members"));
        assertEquals(2, row.get("max_members"));
        assertEquals(0, row.get("chest_pages"));
        assertEquals("Iban", row.get("owner"));
        assertEquals(true, row.get("can_manage_ranks"));
        assertTrue(data.guild(f.player("Eve")).isEmpty());
        assertEquals(1, data.guild(null).size(), "the editor's preview");
    }

    @Test
    void memberRowsSayWhatTheViewerMayDoToEach() {
        Player bob = member(guild, "Bob");
        guild.getMember(bob.getUniqueId()).setName("Bob");

        Map<String, Object> seenByOwner = data.members(owner).stream().filter(r -> r.get("name").equals("Bob")).findFirst().orElseThrow();
        assertEquals(true, seenByOwner.get("can_promote"));
        assertEquals(false, seenByOwner.get("can_demote"), "already the lowest rank");
        assertEquals(true, seenByOwner.get("can_kick"));
        assertEquals(false, seenByOwner.get("self"));

        Map<String, Object> founderSeenByBob = data.members(bob).getFirst();
        assertEquals(true, founderSeenByBob.get("owner"));
        assertEquals(false, founderSeenByBob.get("can_kick"));
        assertEquals(false, founderSeenByBob.get("can_demote"));
        assertTrue(data.ranks(owner).stream().allMatch(r -> r.get("can_manage").equals(true)), "the founder manages every rank");
    }

    @Test
    void ranksAreListedHighestFirstForTheirManagersOnly() {
        Player member = member(guild, "Bob");

        List<Map<String, Object>> rows = data.ranks(owner);
        assertEquals(guild.getRanks().size(), rows.size());
        assertEquals(top().getId().toString(), rows.getFirst().get("id"));
        assertEquals(true, rows.getFirst().get("top"));
        assertEquals(bottom().getId().toString(), rows.getLast().get("id"));
        assertEquals(true, rows.getLast().get("bottom"));
        assertEquals(1, rows.getLast().get("members"), "Bob holds the lowest rank");

        assertTrue(data.ranks(member).isEmpty());
        assertTrue(data.ranks(f.player("Eve")).isEmpty());
        assertFalse(data.ranks(null).isEmpty());
    }

    @Test
    void permissionsOfARankOfTheViewersOwnGuildOnly() {
        bottom().setPermission(GuildPermission.INVITE_MEMBER, true);
        Map<String, Object> params = Map.of("rank", bottom().getId().toString());

        List<Map<String, Object>> rows = data.rankPermissions(owner, params);
        assertEquals(GuildPermission.values().length, rows.size());
        Map<String, Object> invite = rows.stream().filter(r -> r.get("key").equals("invite_member")).findFirst().orElseThrow();
        assertEquals(true, invite.get("granted"));
        assertEquals("messages.permissions.invite_member", invite.get("name"), "the name comes from the lang files");

        Player other = f.player("Eve");
        f.guilds.createGuild(other, "Beta");
        assertTrue(data.rankPermissions(other, params).isEmpty(), "a rank of another guild");
        assertTrue(data.rankPermissions(member(guild, "Bob"), params).isEmpty(), "without MANAGE_RANKS");
        assertTrue(data.rankPermissions(owner, Map.of("rank", "nope")).isEmpty());
        assertEquals(GuildPermission.values().length, data.rankPermissions(null, Map.of()).size());
    }

    @Test
    void membersOfTheViewersGuild() {
        member(guild, "Bob");

        List<Map<String, Object>> rows = data.members(owner);
        assertEquals(2, rows.size());
        assertEquals(owner.getUniqueId().toString(), rows.getFirst().get("id"), "the owner's rank is the highest");
        assertEquals(true, rows.getFirst().get("owner"));
        assertTrue(data.members(f.player("Eve")).isEmpty());
    }

    // --- actions ---

    private record Ctx(Player getPlayer, Map<String, Object> getArgs) implements ActionContext {
        @Override public String getLang() { return "en"; }
        @Override public @Nullable String getProjectId() { return GuildsModule.FOLDER; }
        @Override public ActionContext withArgs(Map<String, Object> args) { return new Ctx(getPlayer, args); }
    }

    private static final class Runtime implements ActionRuntime {
        final java.util.List<String> warnings = new java.util.ArrayList<>();
        @Override public String text(ActionContext ctx, @Nullable Object text) { return text == null ? "" : text.toString(); }
        @Override public ActionContext run(ActionContext ctx, @Nullable Object chain) { return ctx; }
        @Override public void warn(String message) { warnings.add(message); }
    }

    private final Runtime runtime = new Runtime();

    private void run(Player player, Handler handler, Object... args) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) map.put((String) args[i], args[i + 1]);
        handler.execute(new Ctx(player, Map.of()), map, runtime);
    }

    private interface Handler {
        ActionContext execute(ActionContext ctx, Map<String, Object> args, ActionRuntime runtime);
    }

    @Test
    void moveSwapsWithTheNeighbour() {
        RankData rank = bottom();
        run(owner, actions::move, "rank", rank.getId().toString(), "direction", "up");
        assertEquals(1, guild.getRank(rank.getId()).getOrder());
        assertTrue(f.bundle.got(owner, Msg.RANK_MOVED_UP));

        run(owner, actions::move, "rank", rank.getId().toString(), "direction", "down");
        assertEquals(0, guild.getRank(rank.getId()).getOrder());

        run(owner, actions::move, "rank", rank.getId().toString(), "direction", "sideways");
        assertEquals(1, runtime.warnings.size());
    }

    @Test
    void everyActionIsRefusedWithoutManageRanks() {
        Player member = member(guild, "Bob");
        RankData rank = bottom();
        int ranks = guild.getRanks().size();

        run(member, actions::move, "rank", rank.getId().toString(), "direction", "up");
        run(member, actions::permission, "rank", rank.getId().toString(), "permission", "manage_ranks");
        run(member, actions::rename, "rank", rank.getId().toString(), "name", "Boss");
        run(member, actions::create, "name", "Boss");
        run(member, actions::delete, "rank", top().getId().toString());

        assertEquals(0, guild.getRank(rank.getId()).getOrder());
        assertFalse(rank.hasPermission(GuildPermission.MANAGE_RANKS));
        assertEquals(ranks, guild.getRanks().size());
        assertEquals(5, f.bundle.keysSentTo(member).stream().filter(Msg.ERROR_INSUFFICIENT_RANK.key()::equals).count());
    }

    @Test
    void aRankOfAnotherGuildIsNotFound() {
        Player other = f.player("Eve");
        f.guilds.createGuild(other, "Beta");
        RankData foreign = f.registry.byName("Beta").getRanks().getFirst();

        run(owner, actions::permission, "rank", foreign.getId().toString(), "permission", "manage_ranks");
        run(owner, actions::delete, "rank", foreign.getId().toString());

        assertFalse(foreign.hasPermission(GuildPermission.MANAGE_RANKS));
        assertEquals(4, f.registry.byName("Beta").getRanks().size());
        assertTrue(f.bundle.got(owner, Msg.ERROR_RANK_NOT_FOUND));
    }

    @Test
    void permissionTogglesOrSets() {
        RankData rank = bottom();
        String id = rank.getId().toString();

        run(owner, actions::permission, "rank", id, "permission", "kick_member");
        assertTrue(rank.hasPermission(GuildPermission.KICK_MEMBER));
        run(owner, actions::permission, "rank", id, "permission", "kick_member");
        assertFalse(rank.hasPermission(GuildPermission.KICK_MEMBER));
        run(owner, actions::permission, "rank", id, "permission", "KICK_MEMBER", "granted", "true");
        run(owner, actions::permission, "rank", id, "permission", "kick_member", "granted", "true");
        assertTrue(rank.hasPermission(GuildPermission.KICK_MEMBER));
        run(owner, actions::permission, "rank", id, "permission", "kick_member", "granted", "false");
        assertFalse(rank.hasPermission(GuildPermission.KICK_MEMBER));

        run(owner, actions::permission, "rank", id, "permission", "fly");
        assertEquals(1, runtime.warnings.size());
    }

    @Test
    void createRenameAndDeleteFromTheDialogsAndMenus() {
        int ranks = guild.getRanks().size();
        run(owner, actions::create, "name", "<gold>Veteran");
        assertEquals(ranks + 1, guild.getRanks().size());
        RankData veteran = top();
        assertEquals("<gold>Veteran", veteran.getName());

        run(owner, actions::rename, "rank", veteran.getId().toString(), "name", " <red>Elder ");
        assertEquals("<red>Elder", veteran.getName());

        run(owner, actions::create, "name", "");
        assertTrue(f.bundle.got(owner, Msg.ERROR_RANK_NAME_LENGTH));
        assertEquals(ranks + 1, guild.getRanks().size());

        run(owner, actions::delete, "rank", veteran.getId().toString());
        assertEquals(ranks, guild.getRanks().size());
        assertTrue(f.bundle.got(owner, Msg.RANK_DELETED));

        member(guild, "Bob");
        run(owner, actions::delete, "rank", bottom().getId().toString());
        assertTrue(f.bundle.got(owner, Msg.ERROR_RANK_NOT_EMPTY), "a rank members hold stays");
    }

    // --- module ---

    @Test
    void enablingRegistersFolderMenusDataSourcesAndActions() {
        ArtisanAPI api = mock(ArtisanAPI.class);
        ProjectRootRegistry roots = mock(ProjectRootRegistry.class);
        when(roots.moduleFolder(any(), any(), isNull())).thenReturn(new File("guilds"));
        DataSourceRegistry sources = new DataSourceRegistry();
        ActionRegistry actionRegistry = new ActionRegistry();
        TemplateRegistry templates = new TemplateRegistry();
        ModelRegistry models = new ModelRegistry();
        EditorScreenRegistry screens = new EditorScreenRegistry();
        models.bindOwner(GuildsModule.ID);
        screens.bindOwner(GuildsModule.ID);
        sources.bindOwner(GuildsModule.ID);
        actionRegistry.bindOwner(GuildsModule.ID);
        templates.bindOwner(GuildsModule.ID);
        when(api.getProjectRoots()).thenReturn(roots);
        when(api.getDataSources()).thenReturn(sources);
        when(api.getActionRegistry()).thenReturn(actionRegistry);
        when(api.getTemplates()).thenReturn(templates);
        when(api.getModels()).thenReturn(models);
        when(api.getScreens()).thenReturn(screens);

        new GuildsModule(guilds, f.messages, () -> {}, Logger.getLogger("GuildMenuTest")).onEnable(api);

        verify(roots).moduleFolder(eq("guilds"), eq(GuildsModule.ID), isNull());
        assertEquals(Set.of(GuildsModule.VIEWER, GuildsModule.GUILD, GuildsModule.RANKS, GuildsModule.RANK_PERMISSIONS,
                        GuildsModule.MEMBERS),
                sources.refs());
        assertEquals(Set.of(GuildsModule.RANK_MOVE, GuildsModule.RANK_DELETE, GuildsModule.RANK_RENAME,
                GuildsModule.RANK_CREATE, GuildsModule.RANK_PERMISSION), actionRegistry.names());
        TemplateDeclaration system = templates.find(GuildsModule.ID, GuildsModule.SYSTEM);
        assertNotNull(system);
        assertTrue(system.isSystem());
        assertEquals(GuildsModule.SYSTEM_FILES, system.getPaths());
        assertEquals(1, sources.fetch(GuildsModule.VIEWER, owner, Map.of()).size());
        assertTrue(models.has(GuildSettings.MODEL));
        assertEquals(List.of(GuildSettings.TABLE),
                screens.declared().stream().map(EditorScreenDeclaration::getTable).toList());
    }

    /** The menus the commands open are shipped, and every file reads as YAML of its kind. */
    @Test
    @SuppressWarnings("unchecked")
    void shippedFilesMatchTheCommands() {
        TemplateDeclaration system = TemplateDeclaration.fromResources(getClass().getClassLoader(),
                GuildsModule.SYSTEM_ROOT, GuildsModule.SYSTEM, GuildsModule.SYSTEM_VERSION, GuildsModule.SYSTEM_FILES).build();
        for (String path : system.getPaths()) {
            Map<String, Object> doc = new Yaml().load(new String(system.getFile(path), StandardCharsets.UTF_8));
            String kind = path.startsWith("menus/") ? "menu/v1" : "dialog/v1";
            assertEquals(kind, doc.get("schema"), path);
        }
        for (String ref : List.of(GuildCommands.MAIN_MENU, GuildCommands.MEMBERS_MENU, GuildCommands.RANKS_MENU,
                GuildCommands.DISBAND_MENU)) {
            String[] parts = ref.split(":");
            assertEquals(GuildsModule.FOLDER, parts[0]);
            assertTrue(system.getPaths().contains("menus/" + parts[1] + ".yaml"), ref);
        }
        String disband = new String(system.getFile("menus/disband_confirm.yaml"), StandardCharsets.UTF_8);
        assertTrue(disband.contains("module_command: { command: \"guilds:guild.disband.confirm\" }"));
    }
}
