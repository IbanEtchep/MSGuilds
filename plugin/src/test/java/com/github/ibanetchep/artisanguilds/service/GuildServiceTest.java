package com.github.ibanetchep.artisanguilds.service;

import com.github.ibanetchep.artisanguilds.Fixture;
import com.github.ibanetchep.artisanguilds.api.event.GuildCreateEvent;
import com.github.ibanetchep.artisanguilds.api.event.GuildMemberJoinEvent;
import com.github.ibanetchep.artisanguilds.api.event.GuildMemberLeaveEvent;
import com.github.ibanetchep.artisanguilds.model.GuildData;
import com.github.ibanetchep.artisanguilds.model.MemberData;
import com.github.ibanetchep.artisanguilds.model.RankData;
import com.github.ibanetchep.artisanguilds.sync.GuildSync;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuildServiceTest {

    private Fixture f;
    private Player owner;

    @BeforeEach
    void setUp() throws Exception {
        f = new Fixture();
        owner = f.player("Iban");
    }

    private GuildData create() {
        f.guilds.createGuild(owner, "Alpha");
        return f.registry.byName("Alpha");
    }

    /** Invited then joined, at {@code rank} (0 member, 1 moderator, 2 admin, 3 owner). */
    private Player member(GuildData guild, String name, int rank) {
        Player player = f.player(name);
        guild.invites().add(player.getUniqueId());
        f.guilds.joinGuild(player, guild);
        guild.getMember(player.getUniqueId()).setRank(guild.getRanks().get(rank));
        return player;
    }

    @Test
    void createsAGuildWithItsOwnerAndSavesIt() throws Exception {
        GuildData guild = create();

        assertNotNull(guild);
        MemberData member = guild.getMember(owner.getUniqueId());
        assertTrue(member.isOwner());
        assertSame(guild.getRanks().getLast(), member.getRank());
        assertEquals(4, guild.getRanks().size());
        assertTrue(f.bundle.got(owner, Msg.GUILD_CREATED));
        GuildCreateEvent event = f.events(GuildCreateEvent.class).getFirst();
        assertSame(guild, event.getGuild());
        assertFalse(event.isRemote());
        assertEquals(guild.getId(), f.repository.loadMember(owner.getUniqueId()).guildId());
        assertEquals(4, f.repository.load(guild.getId()).ranks().size());
        assertTrue(f.network.published.contains(new Fixture.Published(GuildSync.GUILD, guild.getId().toString())));
    }

    @Test
    void refusesATakenOrInvalidNameAndASecondGuild() {
        create();
        Player other = f.player("Other");

        f.guilds.createGuild(other, "alpha");
        f.guilds.createGuild(other, "a b");
        f.guilds.createGuild(other, "ab");
        f.guilds.createGuild(owner, "Beta");

        assertEquals(List.of(Msg.GUILD_ALREADY_EXISTS.key(), Msg.GUILD_NAME_NO_SPACE.key(), Msg.GUILD_NAME_INVALID_LENGTH.key()),
                f.bundle.keysSentTo(other));
        assertTrue(f.bundle.got(owner, Msg.ERROR_SELF_ALREADY_IN_GUILD));
        assertEquals(1, f.registry.all().size());
    }

    @Test
    void joinsOnlyWhenInvitedThenLeaves() throws Exception {
        GuildData guild = create();
        Player player = f.player("Joiner");

        f.guilds.joinGuild(player, guild);
        assertTrue(f.bundle.got(player, Msg.ERROR_NOT_INVITED));
        assertNull(guild.getMember(player.getUniqueId()));

        f.guilds.invite(owner, player.getUniqueId(), "Joiner");
        assertTrue(guild.invites().contains(player.getUniqueId()));
        assertTrue(f.network.published.contains(new Fixture.Published(GuildSync.INVITE, GuildSync.pair(guild.getId(), player.getUniqueId()))));
        f.guilds.joinGuild(player, guild);

        MemberData member = guild.getMember(player.getUniqueId());
        assertSame(guild.defaultRank(), member.getRank());
        assertFalse(guild.invites().contains(player.getUniqueId()));
        assertNotNull(f.repository.loadMember(player.getUniqueId()));
        GuildMemberJoinEvent joined = f.events(GuildMemberJoinEvent.class).getFirst();
        assertEquals(player.getUniqueId(), joined.getPlayerId());
        assertFalse(joined.isRemote());

        f.guilds.quitGuild(player);

        assertNull(guild.getMember(player.getUniqueId()));
        assertNull(f.repository.loadMember(player.getUniqueId()));
        GuildMemberLeaveEvent left = f.events(GuildMemberLeaveEvent.class).getFirst();
        assertEquals(GuildMemberLeaveEvent.Reason.QUIT, left.getReason());
        assertTrue(f.network.published.contains(new Fixture.Published(GuildSync.PLAYER, player.getUniqueId() + ";QUIT")));
    }

    @Test
    void theOwnerCannotLeave() {
        GuildData guild = create();

        f.guilds.quitGuild(owner);

        assertTrue(f.bundle.got(owner, Msg.ERROR_LEAVE_OWNER));
        assertNotNull(guild.getMember(owner.getUniqueId()));
    }

    @Test
    void aFullGuildTakesNoOneElse() {
        GuildData guild = create();
        member(guild, "Second", 0);
        Player third = f.player("Third");
        guild.invites().add(third.getUniqueId());

        f.guilds.joinGuild(third, guild);

        assertTrue(f.bundle.got(third, Msg.ERROR_GUILD_FULL));
        assertEquals(2, guild.getMembers().size());
    }

    @Test
    void kickNeedsThePermissionAndAHigherRank() throws Exception {
        GuildData guild = create();
        f.leveling.setLevel(guild, 3);
        Player member = member(guild, "Member", 0);
        Player other = member(guild, "Other", 0);
        Player moderator = member(guild, "Moderator", 1);
        Player admin = member(guild, "Admin", 2);

        f.guilds.kick(member, other.getUniqueId());
        assertTrue(f.bundle.got(member, Msg.ERROR_INSUFFICIENT_RANK));

        f.guilds.kick(moderator, admin.getUniqueId());
        assertTrue(f.bundle.got(moderator, Msg.ERROR_KICK_RANK));

        f.guilds.kick(admin, owner.getUniqueId());
        assertTrue(f.bundle.got(admin, Msg.ERROR_KICK_RANK));
        assertNotNull(guild.getMember(owner.getUniqueId()));
        assertTrue(f.events(GuildMemberLeaveEvent.class).isEmpty());

        f.guilds.kick(moderator, other.getUniqueId());

        assertNull(guild.getMember(other.getUniqueId()));
        assertNull(f.repository.loadMember(other.getUniqueId()));
        assertTrue(f.bundle.got(other, Msg.KICK_TARGET));
        GuildMemberLeaveEvent event = f.events(GuildMemberLeaveEvent.class).getFirst();
        assertEquals(other.getUniqueId(), event.getPlayerId());
        assertEquals(GuildMemberLeaveEvent.Reason.KICKED, event.getReason());
    }

    @Test
    void promotesAndDemotesWithinTheRanks() {
        GuildData guild = create();
        Player member = member(guild, "Member", 0);
        RankData lowest = guild.getRanks().getFirst();

        f.guilds.demote(owner, member.getUniqueId());
        assertTrue(f.bundle.got(owner, Msg.ERROR_ALREADY_LOWEST_RANK));

        f.guilds.promote(owner, member.getUniqueId());

        assertSame(guild.rankAbove(lowest), guild.getMember(member.getUniqueId()).getRank());
    }

    @Test
    void onlyTheOwnerTransfersTheGuild() throws Exception {
        GuildData guild = create();
        Player admin = member(guild, "Admin", 2);

        f.guilds.transfer(admin, admin.getUniqueId());
        assertTrue(f.bundle.got(admin, Msg.ERROR_TRANSFER_OWNER));
        f.guilds.transfer(owner, owner.getUniqueId());
        assertTrue(f.bundle.got(owner, Msg.ERROR_TRANSFER_SELF));
        assertEquals(owner.getUniqueId(), guild.getOwnerId());

        f.guilds.transfer(owner, admin.getUniqueId());

        assertEquals(admin.getUniqueId(), guild.getOwnerId());
        assertTrue(guild.getMember(admin.getUniqueId()).isOwner());
        assertEquals(admin.getUniqueId(), f.repository.load(guild.getId()).ownerId());
    }

    @Test
    void withdrawNeedsThePermissionAndTheMoney() throws Exception {
        GuildData guild = create();
        Player member = member(guild, "Member", 0);
        f.leveling.setLevel(guild, 2);
        Player admin = member(guild, "Admin", 2);
        assertTrue(f.bank.deposit(guild, 100));

        f.bank.withdraw(member, 10);
        assertTrue(f.bundle.got(member, Msg.ERROR_INSUFFICIENT_RANK));

        f.bank.withdraw(admin, 500);
        assertTrue(f.bundle.got(admin, Msg.BANK_INSUFFICIENT_FUNDS));
        assertEquals(100, guild.getBalance());
        assertFalse(f.bank.withdraw(guild, 500, "Quests"));

        f.bank.withdraw(admin, 40);

        assertTrue(f.bundle.got(admin, Msg.BANK_WITHDRAW_SUCCESS));
        assertEquals(60, guild.getBalance());
        assertEquals(40, f.economy.balance(admin));
        assertEquals(60, f.repository.load(guild.getId()).balance());
        assertTrue(f.bank.withdraw(guild, 60, "Quests"));
        assertEquals(0, guild.getBalance());
        assertEquals(2, f.repository.logs(guild.getId()).stream().filter(l -> l.contains(Msg.LOG_BANK_WITHDRAW.key())).count());
    }

    @Test
    void depositTakesFromThePlayer() {
        GuildData guild = create();
        f.economy.balances.put(owner, 30.0);

        f.bank.deposit(owner, 50);
        assertTrue(f.bundle.got(owner, Msg.PLAYER_INSUFFICIENT_FUNDS));

        f.bank.deposit(owner, 20);
        assertEquals(20, guild.getBalance());
        assertEquals(10, f.economy.balance(owner));
    }

    @Test
    void experienceSetsTheLevel() throws Exception {
        GuildData guild = create();
        assertEquals(1, guild.getLevel());
        assertEquals(2, f.leveling.info(guild).maxMembers());

        f.leveling.addExp(guild, 150);
        assertEquals(2, guild.getLevel());
        assertEquals(5, f.leveling.info(guild).maxMembers());
        assertEquals(2, f.repository.load(guild.getId()).level());

        f.leveling.setLevel(guild, 3);
        assertEquals(300, guild.getExp());
        assertEquals(3, guild.getLevel());

        f.leveling.removeExp(guild, 1000);
        assertEquals(0, guild.getExp());
        assertEquals(1, guild.getLevel());

        assertThrows(IllegalArgumentException.class, () -> f.leveling.setLevel(guild, 99));
        assertEquals(1, f.leveling.levelFor(99));
        assertEquals(3, f.leveling.levelFor(10_000));
    }
}
