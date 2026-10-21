package com.github.ibanetchep.artisanguilds.command;

import com.github.ibanetchep.artisanguilds.Guilds;
import com.github.ibanetchep.artisanguilds.api.ChatMode;
import com.github.ibanetchep.artisanguilds.model.GuildData;
import com.github.ibanetchep.artisanguilds.model.MemberData;
import com.github.ibanetchep.artisanguilds.model.RankData;
import com.github.ibanetchep.artisanguilds.service.GuildMessages;
import com.github.ibanetchep.artisanguilds.service.Msg;
import net.artisanmc.modules.api.playercommand.CommandArgument;
import net.artisanmc.modules.api.playercommand.PlayerCommandContext;
import net.artisanmc.modules.api.playercommand.PlayerCommandDeclaration;
import net.artisanmc.modules.api.playercommand.PlayerCommandExecutor;
import net.artisanmc.modules.api.text.TextApi;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@code /guild} (alias {@code /g}), MSGuilds' {@code GuildCMD} declared through Artisan's player
 * commands: the admin renames or dresses it with a {@code command_override/v1} file. Each
 * sub-command parses its arguments into guilds and members, then calls the services.
 */
public final class GuildCommands {

    public static final String ROOT = "guilds:guild";
    /** Menus of the module's folder (lot 4), opened by Artisan in place of an executor. */
    public static final String RANKS_MENU = "guilds:ranks";
    public static final String DISBAND_MENU = "guilds:disband_confirm";

    static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss");

    private final Guilds guilds;
    private final GuildMessages messages;
    private final TextApi text;
    private final PlayerLookup players;
    private final Runnable reload;
    private final Consumer<Player> lands;
    private final Logger logger;

    public GuildCommands(Guilds guilds, GuildMessages messages, TextApi text, PlayerLookup players,
                         Runnable reload, Consumer<Player> lands, Logger logger) {
        this.guilds = guilds;
        this.messages = messages;
        this.text = text;
        this.players = players;
        this.reload = reload;
        this.lands = lands;
        this.logger = logger;
    }

    /** The whole tree, registered once with {@code api.getPlayerCommands()}. */
    public PlayerCommandDeclaration declaration() {
        return PlayerCommandDeclaration.builder(ROOT)
                .name("guild")
                .aliases("g")
                .description(Map.of("en", "Guilds", "fr", "Guildes"))
                .executor(player(this::help))
                .child(sub("reload", "Reload the configuration and messages", "Recharger la configuration et les messages")
                        .permission("guilds.reload")
                        .consoleAllowed(true)
                        .executor(ctx -> {
                            reload.run();
                            messages.send(ctx.getSender(), Msg.RELOAD);
                        })
                        .build())
                .child(sub("help", "List the guild commands", "Lister les commandes de guilde")
                        .executor(player(this::help)).build())
                .child(sub("create", "Create a guild", "Créer une guilde")
                        .argument(CommandArgument.string("name"))
                        .executor(player((p, ctx) -> guilds.guilds().createGuild(p, ctx.get("name", String.class))))
                        .build())
                .child(sub("disband", "Disband your guild", "Dissoudre votre guilde")
                        .opensMenu(DISBAND_MENU)
                        .child(sub("disband.confirm", "confirm", "Disband your guild, without confirmation",
                                "Dissoudre votre guilde, sans confirmation")
                                .executor(player((p, ctx) -> guilds.guilds().disbandGuild(p))).build())
                        .build())
                .child(sub("chat", "Switch between public, guild and alliance chat",
                        "Passer du chat public au chat de guilde ou d'alliance")
                        .permission("guilds.chat")
                        .argument(CommandArgument.choice("mode", List.of("public", "guild", "ally")).optional())
                        .executor(player(this::chat))
                        .build())
                .child(sub("invite", "Invite a player into your guild", "Inviter un joueur dans votre guilde")
                        .argument(CommandArgument.string("player"))
                        .executor(player(this::invite)).build())
                .child(sub("revoke", "Withdraw an invitation", "Annuler une invitation")
                        .argument(CommandArgument.string("player"))
                        .executor(player(this::revoke)).build())
                .child(memberCommand("promote", "Promote a member", "Promouvoir un membre",
                        (p, member) -> guilds.guilds().promote(p, member.getPlayerId())))
                .child(memberCommand("demote", "Demote a member", "Rétrograder un membre",
                        (p, member) -> guilds.guilds().demote(p, member.getPlayerId())))
                .child(memberCommand("transfer", "Hand your guild over to a member", "Céder votre guilde à un membre",
                        (p, member) -> guilds.guilds().transfer(p, member.getPlayerId())))
                .child(memberCommand("kick", "Kick a member out", "Expulser un membre",
                        (p, member) -> guilds.guilds().kick(p, member.getPlayerId())))
                .child(sub("join", "Join a guild that invited you", "Rejoindre une guilde qui vous a invité")
                        .argument(CommandArgument.string("guild"))
                        .executor(player((p, ctx) -> withGuild(p, ctx.get("guild", String.class),
                                guild -> guilds.guilds().joinGuild(p, guild))))
                        .build())
                .child(sub("leave", "Leave your guild", "Quitter votre guilde")
                        .executor(player((p, ctx) -> guilds.guilds().quitGuild(p))).build())
                .child(sub("bank", "Your guild's bank", "La banque de votre guilde")
                        .executor(player((p, ctx) -> guilds.bank().showBalance(p)))
                        .child(sub("bank.balance", "balance", "Show the bank's balance", "Afficher le solde de la banque")
                                .executor(player((p, ctx) -> guilds.bank().showBalance(p))).build())
                        .child(sub("bank.deposit", "deposit", "Deposit money in the bank", "Déposer de l'argent dans la banque")
                                .argument(CommandArgument.string("amount"))
                                .executor(player((p, ctx) -> withAmount(p, ctx, amount -> guilds.bank().deposit(p, amount))))
                                .build())
                        .child(sub("bank.withdraw", "withdraw", "Withdraw money from the bank", "Retirer de l'argent de la banque")
                                .argument(CommandArgument.string("amount"))
                                .executor(player((p, ctx) -> withAmount(p, ctx, amount -> guilds.bank().withdraw(p, amount))))
                                .build())
                        .build())
                .child(sub("home", "Teleport to your guild's home", "Se téléporter au home de la guilde")
                        .executor(player((p, ctx) -> guilds.guilds().teleportHome(p))).build())
                .child(sub("sethome", "Set your guild's home here", "Placer le home de la guilde ici")
                        .executor(player((p, ctx) -> guilds.guilds().setHome(p))).build())
                .child(sub("delhome", "Delete your guild's home", "Supprimer le home de la guilde")
                        .executor(player((p, ctx) -> guilds.guilds().delHome(p))).build())
                .child(sub("lands", "Your guild's lands", "Les territoires de la guilde")
                        .executor(player((p, ctx) -> lands.accept(p))).build())
                .child(sub("info", "Show a guild", "Afficher une guilde")
                        .argument(CommandArgument.string("guild").optional())
                        .consoleAllowed(true)
                        .executor(this::info).build())
                .child(sub("list", "List the guilds online", "Lister les guildes en ligne")
                        .argument(CommandArgument.integer("page").optional())
                        .consoleAllowed(true)
                        .executor(this::list).build())
                .child(sub("logs", "Read a guild's log", "Lire le journal d'une guilde")
                        .argument(CommandArgument.integer("page").optional())
                        .argument(CommandArgument.string("guild").optional())
                        .consoleAllowed(true)
                        .executor(this::logs).build())
                .child(sub("alliance", "Your guild's alliances", "Les alliances de la guilde")
                        .child(allianceCommand("invite", "Offer an alliance to a guild", "Proposer une alliance à une guilde",
                                (p, guild) -> guilds.alliances().request(p, guild)))
                        .child(allianceCommand("accept", "Accept a guild's alliance", "Accepter l'alliance d'une guilde",
                                (p, guild) -> guilds.alliances().accept(p, guild)))
                        .child(allianceCommand("remove", "End an alliance", "Rompre une alliance",
                                (p, guild) -> guilds.alliances().revoke(p, guild)))
                        .build())
                .child(sub("ranks", "Manage your guild's ranks", "Gérer les grades de la guilde")
                        .opensMenu(RANKS_MENU).build())
                .build();
    }

    // --- Executors -------------------------------------------------------------------------------

    void help(Player player, PlayerCommandContext ctx) {
        player.sendMessage("");
        messages.send(player, Msg.HELP_HEADER);
        player.sendMessage("");
        messages.send(player, guilds.registry().member(player.getUniqueId()) == null ? Msg.HELP_NO_GUILD : Msg.HELP_MEMBER);
    }

    void chat(Player player, PlayerCommandContext ctx) {
        String mode = ctx.get("mode", String.class);
        if (mode == null) guilds.guilds().toggleChatMode(player);
        else guilds.guilds().setChatMode(player, ChatMode.valueOf(mode.toUpperCase(Locale.ROOT)));
    }

    void invite(Player player, PlayerCommandContext ctx) {
        PlayerLookup.Found target = players.find(ctx.get("player", String.class));
        if (target == null) messages.send(player, Msg.ERROR_PLAYER_NOT_FOUND);
        else guilds.guilds().invite(player, target.id(), target.name());
    }

    void revoke(Player player, PlayerCommandContext ctx) {
        PlayerLookup.Found target = players.find(ctx.get("player", String.class));
        if (target == null) messages.send(player, Msg.ERROR_PLAYER_NOT_FOUND);
        else guilds.guilds().revokeInvite(player, target.id(), target.name());
    }

    void info(PlayerCommandContext ctx) {
        CommandSender sender = ctx.getSender();
        GuildData guild = guildOf(ctx, ctx.get("guild", String.class));
        if (guild == null) return;
        messages.send(sender, Msg.INFO_HEADER, "guild", guild.getName());
        messages.send(sender, Msg.INFO_CREATED, "date", DATE.format(guild.getCreatedAt().atZone(ZoneId.systemDefault())));
        for (RankData rank : guild.getRanks()) {
            List<MemberData> members = guild.membersWithRank(rank);
            if (members.isEmpty()) continue;
            messages.sendRich(sender, Msg.INFO_RANK_FORMAT, Map.of("rank", rank.getName()),
                    "players", String.join(", ", members.stream().map(MemberData::getName).toList()));
        }
        List<String> alliances = guild.getAlliances().stream().map(g -> g.getName()).toList();
        if (!alliances.isEmpty()) messages.send(sender, Msg.INFO_ALLIANCES, "alliances", String.join(", ", alliances));
    }

    void list(PlayerCommandContext ctx) {
        CommandSender sender = ctx.getSender();
        List<GuildData> online = guilds.registry().all().stream()
                .filter(g -> !g.getOnlineMembers().isEmpty())
                .sorted(Comparator.comparingInt((GuildData g) -> -g.getOnlineMembers().size()).thenComparing(GuildData::getName))
                .toList();
        Page page = Page.of(ctx.get("page", Integer.class), online.size());
        if (page == null) {
            messages.send(sender, Msg.LIST_INVALID_PAGE, "maxpage", Page.count(online.size()));
            return;
        }
        messages.send(sender, Msg.LIST_HEADER, "page", page.number(), "maxpage", page.count());
        page.of(online).forEach(g -> messages.send(sender, Msg.LIST_FORMAT, "guild", g.getName(),
                "online", g.getOnlineMembers().size(), "total", g.members().size()));
        messages.send(sender, Msg.LIST_FOOTER);
    }

    void logs(PlayerCommandContext ctx) {
        CommandSender sender = ctx.getSender();
        GuildData guild = guildOf(ctx, ctx.get("guild", String.class));
        if (guild == null) return;
        Integer requested = ctx.get("page", Integer.class);
        guilds.store().logs(guild.getId()).whenComplete((logs, error) -> {
            if (error != null) {
                logger.log(Level.WARNING, "Could not read the log of " + guild.getName(), error);
                messages.send(sender, Msg.LOGS_EMPTY);
                return;
            }
            if (logs.isEmpty()) {
                messages.send(sender, Msg.LOGS_EMPTY);
                return;
            }
            Page page = Page.of(requested, logs.size());
            if (page == null) {
                messages.send(sender, Msg.LIST_INVALID_PAGE, "maxpage", Page.count(logs.size()));
                return;
            }
            messages.send(sender, Msg.LOGS_HEADER, "page", page.number(), "maxpage", page.count());
            // MSGuilds wrote its log lines with § colours.
            page.of(logs).forEach(log -> messages.sendRich(sender, Msg.LOGS_ENTRY, Map.of("log", text.fromLegacy(log))));
            messages.send(sender, Msg.LOGS_FOOTER);
        });
    }

    // --- Arguments -------------------------------------------------------------------------------

    /** The named guild, else the sender's own; null (and the sender told) when there is none. */
    private @Nullable GuildData guildOf(PlayerCommandContext ctx, @Nullable String name) {
        if (name != null) {
            GuildData guild = guilds.registry().byName(name);
            if (guild == null) messages.send(ctx.getSender(), Msg.ERROR_GUILD_NOT_FOUND);
            return guild;
        }
        Player player = ctx.getPlayer();
        GuildData own = player == null ? null : guilds.registry().byPlayer(player.getUniqueId());
        if (own == null) messages.send(ctx.getSender(), Msg.ERROR_SPECIFY_GUILD);
        return own;
    }

    private void withGuild(Player player, String name, Consumer<GuildData> then) {
        GuildData guild = guilds.registry().byName(name);
        if (guild == null) messages.send(player, Msg.ERROR_GUILD_NOT_FOUND);
        else then.accept(guild);
    }

    /** A member of the player's guild by name; the services check what the player may do to them. */
    @Nullable MemberData member(Player player, String name) {
        GuildData guild = guilds.registry().byPlayer(player.getUniqueId());
        if (guild == null) {
            messages.send(player, Msg.ERROR_NOT_GUILD_MEMBER);
            return null;
        }
        MemberData member = guild.members().stream()
                .filter(m -> m.getName() != null && m.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
        if (member == null) messages.send(player, Msg.ERROR_PLAYER_NOT_IN_GUILD);
        return member;
    }

    private void withAmount(Player player, PlayerCommandContext ctx, java.util.function.DoubleConsumer then) {
        Double amount = amount(ctx.get("amount", String.class));
        if (amount == null) messages.send(player, Msg.ERROR_NEED_POSITIVE_AMOUNT);
        else then.accept(amount);
    }

    /** A positive, finite amount; {@code 1,5} reads as {@code 1.5}. */
    static @Nullable Double amount(@Nullable String raw) {
        if (raw == null) return null;
        try {
            double value = Double.parseDouble(raw.replace(',', '.'));
            return Double.isFinite(value) && value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** One page of ten lines; null when the page asked for does not exist. */
    record Page(int number, int count) {
        static int count(int lines) {
            return (int) Math.ceil(lines / (double) PAGE_SIZE);
        }

        static @Nullable Page of(@Nullable Integer requested, int lines) {
            int number = requested == null ? 1 : requested;
            int count = count(lines);
            return number < 1 || number > count ? null : new Page(number, count);
        }

        <T> List<T> of(List<T> lines) {
            return lines.stream().skip((number - 1L) * PAGE_SIZE).limit(PAGE_SIZE).toList();
        }
    }

    // --- Declarations ----------------------------------------------------------------------------

    private static PlayerCommandDeclaration.Builder sub(String name, String en, String fr) {
        return sub(name, name, en, fr);
    }

    /** {@code path} names the id ({@code bank.deposit}), {@code name} what players type. */
    private static PlayerCommandDeclaration.Builder sub(String path, String name, String en, String fr) {
        return PlayerCommandDeclaration.builder(ROOT + "." + path).name(name).description(Map.of("en", en, "fr", fr));
    }

    private PlayerCommandDeclaration memberCommand(String name, String en, String fr,
                                                   java.util.function.BiConsumer<Player, MemberData> then) {
        return sub(name, en, fr)
                .argument(CommandArgument.string("member"))
                .executor(player((p, ctx) -> {
                    MemberData member = member(p, ctx.get("member", String.class));
                    if (member != null) then.accept(p, member);
                }))
                .build();
    }

    private PlayerCommandDeclaration allianceCommand(String name, String en, String fr,
                                                     java.util.function.BiConsumer<Player, GuildData> then) {
        return sub("alliance." + name, name, en, fr)
                .argument(CommandArgument.string("guild"))
                .executor(player((p, ctx) -> withGuild(p, ctx.get("guild", String.class), guild -> then.accept(p, guild))))
                .build();
    }

    /** Artisan runs a command without {@code consoleAllowed} for players only. */
    private static PlayerCommandExecutor player(java.util.function.BiConsumer<Player, PlayerCommandContext> run) {
        return ctx -> run.accept(ctx.getPlayer(), ctx);
    }
}
