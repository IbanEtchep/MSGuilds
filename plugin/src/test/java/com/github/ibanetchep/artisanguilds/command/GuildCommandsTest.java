package com.github.ibanetchep.artisanguilds.command;

import com.github.ibanetchep.artisanguilds.Fixture;
import com.github.ibanetchep.artisanguilds.Guilds;
import com.github.ibanetchep.artisanguilds.api.ChatMode;
import com.github.ibanetchep.artisanguilds.model.GuildData;
import com.github.ibanetchep.artisanguilds.service.AllianceService;
import com.github.ibanetchep.artisanguilds.service.Msg;
import com.github.ibanetchep.artisanguilds.service.RankService;
import net.artisanmc.modules.api.network.NetworkPlayer;
import net.artisanmc.modules.api.playercommand.CommandArgument;
import net.artisanmc.modules.api.playercommand.PlayerCommandContext;
import net.artisanmc.modules.api.playercommand.SuggestionContext;
import net.artisanmc.modules.api.playercommand.PlayerCommandDeclaration;
import net.artisanmc.modules.api.text.TextApi;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class GuildCommandsTest {

    private Fixture f;
    private Player owner;
    private final Map<String, PlayerLookup.Found> known = new HashMap<>();
    private int reloads;
    /** Who {@code /guild} sent to the guild menu. */
    private final java.util.List<Player> menus = new java.util.ArrayList<>();
    private PlayerCommandDeclaration root;
    private GuildSuggestions suggestions;
    private final List<NetworkPlayer> online = new ArrayList<>();
    private final Map<UUID, String> names = new HashMap<>();

    @BeforeEach
    void setUp() throws Exception {
        f = new Fixture();
        owner = f.player("Iban");
        GuildData.online = f.platform::isOnline;
        Guilds guilds = new Guilds(f.registry, f.store, f.leveling, f.sync, f.guilds, f.bank,
                new RankService(f.registry, f.store, f.messages, TextApi.NOOP),
                new AllianceService(f.registry, f.store, f.messages, f.network, f.sync), f.chest);
        suggestions = new GuildSuggestions(f.registry, () -> online, id -> names.get(id));
        root = new GuildCommands(guilds, f.messages, TextApi.NOOP, known::get, suggestions, () -> reloads++, p -> { },
                p -> menus.add(p), Logger.getLogger("GuildCommandsTest")).declaration();
    }

    private GuildData guild() {
        f.guilds.createGuild(owner, "Alpha");
        return f.registry.byName("Alpha");
    }

    private Player member(GuildData guild, String name) {
        Player player = f.player(name);
        guild.invites().add(player.getUniqueId());
        f.guilds.joinGuild(player, guild);
        return player;
    }

    private void run(CommandSender sender, String path, Map<String, Object> args) {
        PlayerCommandDeclaration node = find(root, GuildCommands.ROOT + (path.isEmpty() ? "" : "." + path));
        assertNotNull(node, path);
        node.getExecutor().execute(new Context(sender, args));
    }

    private static @Nullable PlayerCommandDeclaration find(PlayerCommandDeclaration node, String id) {
        if (node.getId().equals(id)) return node;
        for (PlayerCommandDeclaration child : node.getChildren()) {
            PlayerCommandDeclaration found = find(child, id);
            if (found != null) return found;
        }
        return null;
    }

    @Test
    void declaresMsGuildsCommandsUnderTheSameNames() {
        assertEquals("guild", root.getName());
        assertEquals(List.of("g"), root.getAliases());
        assertEquals(List.of("reload", "help", "create", "disband", "chat", "invite", "revoke", "promote", "demote",
                        "transfer", "kick", "join", "leave", "bank", "chest", "home", "sethome", "delhome", "lands",
                        "info", "list", "logs", "alliance", "ranks", "members", "admin"),
                root.getChildren().stream().map(PlayerCommandDeclaration::getName).toList());
        assertEquals(List.of("balance", "deposit", "withdraw"),
                find(root, "guilds:guild.bank").getChildren().stream().map(PlayerCommandDeclaration::getName).toList());
        assertEquals(List.of("invite", "accept", "remove"),
                find(root, "guilds:guild.alliance").getChildren().stream().map(PlayerCommandDeclaration::getName).toList());
        assertEquals("guilds.reload", find(root, "guilds:guild.reload").getPermission());
        assertTrue(find(root, "guilds:guild.reload").isConsoleAllowed());
        assertEquals("guilds.chat", find(root, "guilds:guild.chat").getPermission());
        assertEquals(List.of("public", "guild", "ally"), find(root, "guilds:guild.chat").getArguments().getFirst().getValues());
        assertTrue(root.getChildren().stream().allMatch(c -> c.getDescription().keySet().containsAll(List.of("en", "fr"))));
    }

    @Test
    void ranksAndDisbandOpenTheModuleMenusAndDisbandConfirmRunsIt() {
        assertEquals(GuildCommands.RANKS_MENU, find(root, "guilds:guild.ranks").getOpensMenu());
        assertEquals(GuildCommands.DISBAND_MENU, find(root, "guilds:guild.disband").getOpensMenu());
        GuildData guild = guild();

        run(owner, "disband.confirm", Map.of());

        assertNull(f.registry.get(guild.getId()));
    }

    @Test
    void membersAreNamedWithoutCase() {
        GuildData guild = guild();
        Player alice = member(guild, "Alice");
        guild.getMember(alice.getUniqueId()).setName("Alice");
        var before = guild.getMember(alice.getUniqueId()).getRank();

        run(owner, "promote", Map.of("member", "alice"));

        assertEquals(before.getOrder() + 1, guild.getMember(alice.getUniqueId()).getRank().getOrder());
    }

    @Test
    void aMemberOutsideTheGuildOrNoGuildIsRefused() {
        guild();
        run(owner, "kick", Map.of("member", "Nobody"));
        assertTrue(f.bundle.got(owner, Msg.ERROR_PLAYER_NOT_IN_GUILD));

        Player loner = f.player("Loner");
        run(loner, "kick", Map.of("member", "Iban"));
        assertTrue(f.bundle.got(loner, Msg.ERROR_NOT_GUILD_MEMBER));
    }

    @Test
    void invitesAPlayerFoundOnTheNetwork() {
        GuildData guild = guild();
        UUID bob = UUID.randomUUID();
        known.put("Bob", new PlayerLookup.Found(bob, "Bob"));

        run(owner, "invite", Map.of("player", "Bob"));
        run(owner, "invite", Map.of("player", "Ghost"));

        assertTrue(guild.invites().contains(bob));
        assertTrue(f.bundle.got(owner, Msg.ERROR_PLAYER_NOT_FOUND));
    }

    @Test
    void readsAmounts() {
        assertEquals(1.5, GuildCommands.amount("1,5"));
        assertEquals(200.0, GuildCommands.amount("200"));
        assertNull(GuildCommands.amount("-3"));
        assertNull(GuildCommands.amount("0"));
        assertNull(GuildCommands.amount("abc"));
        assertNull(GuildCommands.amount("NaN"));
        assertNull(GuildCommands.amount("Infinity"));

        guild();
        run(owner, "bank.withdraw", Map.of("amount", "beaucoup"));
        assertTrue(f.bundle.got(owner, Msg.ERROR_NEED_POSITIVE_AMOUNT));
    }

    @Test
    void depositsInTheBank() {
        GuildData guild = guild();
        f.economy.balances.put(owner, 500.0);

        run(owner, "bank.deposit", Map.of("amount", "120"));

        assertEquals(120.0, guild.getBalance());
    }

    @Test
    void pagesOfTenLines() {
        assertNull(GuildCommands.Page.of(null, 0));
        assertEquals(new GuildCommands.Page(1, 2), GuildCommands.Page.of(null, 15));
        assertEquals(new GuildCommands.Page(2, 2), GuildCommands.Page.of(2, 15));
        assertNull(GuildCommands.Page.of(3, 15));
        assertNull(GuildCommands.Page.of(0, 15));
        List<Integer> lines = java.util.stream.IntStream.range(0, 15).boxed().toList();
        assertEquals(List.of(10, 11, 12, 13, 14), GuildCommands.Page.of(2, 15).of(lines));
    }

    @Test
    void joinsAndChatsByName() {
        GuildData guild = guild();
        Player alice = f.player("Alice");
        run(alice, "join", Map.of("guild", "Nope"));
        assertTrue(f.bundle.got(alice, Msg.ERROR_GUILD_NOT_FOUND));

        guild.invites().add(alice.getUniqueId());
        run(alice, "join", Map.of("guild", "Alpha"));
        run(alice, "chat", Map.of("mode", "ally"));

        assertEquals(ChatMode.ALLY, guild.getMember(alice.getUniqueId()).getChatMode());
    }

    @Test
    void theConsoleNamesTheGuildItReads() {
        guild();
        CommandSender console = mock(CommandSender.class);

        run(console, "info", Map.of());
        assertTrue(f.bundle.got(console, Msg.ERROR_SPECIFY_GUILD));

        run(console, "info", Map.of("guild", "Alpha"));
        assertTrue(f.bundle.got(console, Msg.INFO_HEADER));

        run(console, "list", Map.of());
        assertTrue(f.bundle.got(console, Msg.LIST_HEADER));
        assertFalse(f.bundle.got(console, Msg.LIST_INVALID_PAGE));

        run(console, "reload", Map.of());
        assertEquals(1, reloads);
        assertTrue(f.bundle.got(console, Msg.RELOAD));
    }

    private List<String> suggest(CommandSender sender, String path, String argument) {
        PlayerCommandDeclaration node = find(root, GuildCommands.ROOT + "." + path);
        assertNotNull(node, path);
        CommandArgument arg = node.getArguments().stream().filter(a -> a.getName().equals(argument)).findFirst().orElseThrow();
        assertNotNull(arg.getSuggestions(), path + " " + argument);
        return arg.getSuggestions().suggest(new SuggestionContext() {
            @Override public CommandSender getSender() { return sender; }
            @Override public List<String> getPreviousArguments() { return List.of(); }
            @Override public String getInput() { return ""; }
        });
    }

    @Test
    void suggestsGuildsMembersAndPlayersFromMemory() {
        GuildData alpha = guild();
        Player bob = member(alpha, "Bob");
        f.guilds.createGuild(f.player("Carl"), "Beta");
        GuildData beta = f.registry.byName("Beta");

        assertEquals(java.util.Set.of("Alpha", "Beta"), java.util.Set.copyOf(suggest(owner, "join", "guild")));
        assertEquals(List.of("Bob"), suggest(owner, "kick", "member"));
        assertEquals(List.of("Iban"), suggest(bob, "promote", "member"));

        UUID dave = UUID.randomUUID();
        online.add(new NetworkPlayer(dave, "Dave", "survie"));
        online.add(new NetworkPlayer(bob.getUniqueId(), "Bob", "lobby"));
        assertEquals(List.of("Dave"), suggest(owner, "invite", "player"));

        names.put(dave, "Dave");
        alpha.invites().add(dave);
        assertEquals(List.of("Dave"), suggest(owner, "revoke", "player"));

        assertEquals(List.of("Beta"), suggest(owner, "alliance.invite", "guild"));
        beta.allianceInvites().add(alpha.getId());
        assertEquals(List.of("Beta"), suggest(owner, "alliance.accept", "guild"));
        assertEquals(List.of(), suggest(owner, "alliance.remove", "guild"));
        alpha.allianceIds().add(beta.getId());
        assertEquals(List.of("Beta"), suggest(owner, "alliance.remove", "guild"));
        assertEquals(List.of(), suggest(owner, "alliance.invite", "guild"));
    }

    @Test
    void aPlayerWithoutGuildIsOfferedNoMember() {
        guild();
        assertEquals(List.of(), suggest(f.player("Eve"), "kick", "member"));
        assertEquals(List.of(), suggest(f.player("Fay"), "revoke", "player"));
    }

    @Test
    void guildOpensTheMenuForAMemberAndTheHelpForAnyoneElse() {
        Player stranger = f.player("Eve");
        run(stranger, "", Map.of());
        assertTrue(f.bundle.got(stranger, Msg.HELP_NO_GUILD));
        assertEquals(List.of(), menus);

        guild();
        run(owner, "", Map.of());
        assertEquals(List.of(owner), menus);
        assertEquals(GuildCommands.MEMBERS_MENU, find(root, "guilds:guild.members").getOpensMenu());
    }

    @Test
    void setLevelPutsTheGuildAtTheLevelsExperienceFromTheConsole() {
        GuildData guild = guild();
        CommandSender console = mock(CommandSender.class);
        PlayerCommandDeclaration setlevel = find(root, "guilds:guild.admin.setlevel");
        assertTrue(setlevel.isConsoleAllowed());
        assertEquals(GuildCommands.SETLEVEL_PERMISSION, setlevel.getPermission());

        run(console, "admin.setlevel", Map.of("guild", "alpha", "level", 3));
        assertEquals(3, guild.getLevel());
        assertEquals(300, guild.getExp());
        assertTrue(f.bundle.got(console, Msg.LEVEL_SET));

        run(console, "admin.setlevel", Map.of("guild", "Alpha", "level", 9));
        assertEquals(3, guild.getLevel());
        assertTrue(f.bundle.got(console, Msg.LEVEL_UNKNOWN));

        run(console, "admin.setlevel", Map.of("guild", "Nope", "level", 1));
        assertTrue(f.bundle.got(console, Msg.ERROR_GUILD_NOT_FOUND));
    }

    /** Every {@code module_command} of the shipped menus names a command of the tree. */
    @Test
    void shippedMenusCallCommandsThatExist() {
        var system = net.artisanmc.modules.api.template.TemplateDeclaration.fromResources(getClass().getClassLoader(),
                "artisan-system/guilds", "guilds_menus", 1, List.of(
                        "menus/main.yaml", "menus/members.yaml", "menus/disband_confirm.yaml", "dialogs/kick.yaml")).build();
        java.util.regex.Pattern call = java.util.regex.Pattern.compile("module_command: \\{ command: \"([^\"]+)\"");
        int calls = 0;
        for (String path : system.getPaths()) {
            var m = call.matcher(new String(system.getFile(path), java.nio.charset.StandardCharsets.UTF_8));
            while (m.find()) {
                calls++;
                assertNotNull(find(root, m.group(1)), path + ": " + m.group(1));
            }
        }
        assertTrue(calls >= 9, "calls found: " + calls);
    }

    private record Context(CommandSender sender, Map<String, Object> args) implements PlayerCommandContext {
        @Override public String getCommandId() { return GuildCommands.ROOT; }
        @Override public CommandSender getSender() { return sender; }
        @Override public @Nullable Player getPlayer() { return sender instanceof Player p ? p : null; }
        @Override public String getLanguage() { return "en"; }
        @Override public Map<String, Object> getArguments() { return args; }
    }
}
