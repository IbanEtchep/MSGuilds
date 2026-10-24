package com.github.ibanetchep.artisanguilds.sync;

import com.github.ibanetchep.artisanguilds.Fixture;
import com.github.ibanetchep.artisanguilds.api.ChatMode;
import com.github.ibanetchep.artisanguilds.api.event.GuildCreateEvent;
import com.github.ibanetchep.artisanguilds.api.event.GuildMemberJoinEvent;
import com.github.ibanetchep.artisanguilds.api.event.GuildMemberLeaveEvent;
import com.github.ibanetchep.artisanguilds.api.event.GuildPostDisbandEvent;
import com.github.ibanetchep.artisanguilds.model.GuildData;
import com.github.ibanetchep.artisanguilds.model.MemberData;
import com.github.ibanetchep.artisanguilds.storage.GuildRepository.MemberRow;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/** Two servers on one database: {@code other} changes, {@code here} hears of it. */
class GuildSyncTest {

    private Fixture here;
    private Fixture other;

    @BeforeEach
    void setUp() throws Exception {
        here = new Fixture();
        other = new Fixture(here.dataSource);
        here.sync.subscribe(mock(Plugin.class), here.network);
    }

    /** What {@code other} published, delivered to {@code here}. */
    private void relay() {
        other.network.published.forEach(p -> here.network.deliver(p.channel(), p.payload()));
        other.network.published.clear();
    }

    @Test
    void aGuildCreatedElsewhereAppearsWithARemoteEvent() {
        Player owner = other.player("Iban");
        other.guilds.createGuild(owner, "Alpha");

        relay();

        GuildData guild = here.registry.byName("Alpha");
        assertNotNull(guild);
        assertNotNull(guild.getMember(owner.getUniqueId()));
        assertEquals(4, guild.getRanks().size());
        GuildCreateEvent created = here.events(GuildCreateEvent.class).getFirst();
        assertSame(guild, created.getGuild());
        assertTrue(created.isRemote());
        // The owner's own message finds them already there: no join.
        assertTrue(here.events(GuildMemberJoinEvent.class).isEmpty());
    }

    @Test
    void joinKickAndDisbandElsewhereFireRemoteEventsOnTheSameGuild() {
        Player owner = other.player("Iban");
        other.guilds.createGuild(owner, "Alpha");
        relay();
        GuildData held = here.registry.byName("Alpha");
        GuildData there = other.registry.byName("Alpha");

        Player joiner = other.player("Joiner");
        there.invites().add(joiner.getUniqueId());
        other.guilds.joinGuild(joiner, there);
        relay();

        assertSame(held, here.registry.byName("Alpha"));
        assertNotNull(held.getMember(joiner.getUniqueId()));
        GuildMemberJoinEvent joined = here.events(GuildMemberJoinEvent.class).getFirst();
        assertEquals(joiner.getUniqueId(), joined.getPlayerId());
        assertTrue(joined.isRemote());

        other.guilds.kick(owner, joiner.getUniqueId());
        relay();

        assertNull(held.getMember(joiner.getUniqueId()));
        GuildMemberLeaveEvent left = here.events(GuildMemberLeaveEvent.class).getFirst();
        assertEquals(GuildMemberLeaveEvent.Reason.KICKED, left.getReason());
        assertTrue(left.isRemote());

        other.guilds.disbandGuild(owner);
        relay();

        assertNull(here.registry.byName("Alpha"));
        GuildPostDisbandEvent disbanded = here.events(GuildPostDisbandEvent.class).getFirst();
        assertSame(held, disbanded.getGuild());
        assertTrue(disbanded.isRemote());
    }

    @Test
    void aChangedRankOrChatModeIsAppliedWithoutEvent() {
        Player owner = other.player("Iban");
        other.guilds.createGuild(owner, "Alpha");
        relay();
        here.platform.events.clear();
        GuildData held = here.registry.byName("Alpha");
        MemberData member = held.getMember(owner.getUniqueId());

        here.sync.applyMember(owner.getUniqueId(),
                new MemberRow(owner.getUniqueId(), held.getId(), held.getRanks().getFirst().getId(), ChatMode.ALLY),
                GuildMemberLeaveEvent.Reason.UNKNOWN);

        assertSame(member, held.getMember(owner.getUniqueId()));
        assertSame(held.getRanks().getFirst(), member.getRank());
        assertEquals(ChatMode.ALLY, member.getChatMode());
        assertTrue(here.platform.events.isEmpty());
    }

    @Test
    void invitesTravelAndExpire() {
        Player owner = other.player("Iban");
        other.guilds.createGuild(owner, "Alpha");
        relay();
        UUID target = UUID.randomUUID();

        other.guilds.invite(owner, target, "Target");
        relay();

        GuildData held = here.registry.byName("Alpha");
        assertTrue(held.invites().contains(target));
        here.platform.later.forEach(Runnable::run);
        assertTrue(held.invites().isEmpty());
    }
}
