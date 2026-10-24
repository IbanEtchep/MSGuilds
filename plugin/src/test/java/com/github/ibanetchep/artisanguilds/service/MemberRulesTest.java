package com.github.ibanetchep.artisanguilds.service;

import com.github.ibanetchep.artisanguilds.Fixture;
import com.github.ibanetchep.artisanguilds.api.GuildPermission;
import com.github.ibanetchep.artisanguilds.model.GuildData;
import com.github.ibanetchep.artisanguilds.model.MemberData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberRulesTest {

    private Fixture f;
    private Player owner;
    private GuildData guild;

    @BeforeEach
    void setUp() throws Exception {
        f = new Fixture();
        owner = f.player("Iban");
        f.guilds.createGuild(owner, "Alpha");
        guild = f.registry.byName("Alpha");
        f.leveling.setExp(guild, 300); // room for 10
    }

    /** Ranks: 0 member, 1 moderator, 2 admin, 3 owner. */
    private MemberData member(String name, int rank) {
        Player player = f.player(name);
        guild.invites().add(player.getUniqueId());
        f.guilds.joinGuild(player, guild);
        MemberData member = guild.getMember(player.getUniqueId());
        member.setRank(guild.getRanks().get(rank));
        return member;
    }

    @Test
    void theFounderIsNeverDemotedNorKicked() {
        MemberData founder = guild.getMember(owner.getUniqueId());
        MemberData admin = member("Admin", 3);

        assertEquals(Msg.ERROR_CANNOT_DEMOTE_OWNER, MemberRules.demoteRefusal(admin, founder));
        assertEquals(Msg.ERROR_KICK_RANK, MemberRules.kickRefusal(admin, founder));
        f.guilds.demote(owner, owner.getUniqueId());
        assertTrue(f.bundle.got(owner, Msg.ERROR_CANNOT_DEMOTE_OWNER));
    }

    @Test
    void onlyAStrictlyHigherRankActsOnAMember() {
        MemberData admin = member("Admin", 2);
        MemberData other = member("Other", 2);
        MemberData low = member("Low", 0);
        MemberData founder = guild.getMember(owner.getUniqueId());

        assertEquals(Msg.ERROR_TARGET_RANK_TOO_HIGH, MemberRules.promoteRefusal(admin, other));
        assertEquals(Msg.ERROR_KICK_RANK, MemberRules.kickRefusal(admin, other));
        assertNull(MemberRules.promoteRefusal(admin, low));
        assertNull(MemberRules.kickRefusal(admin, low));
        assertEquals(Msg.ERROR_ALREADY_LOWEST_RANK, MemberRules.demoteRefusal(admin, low));
        assertNull(MemberRules.demoteRefusal(founder, other), "the founder demotes an equal rank");
        assertEquals(Msg.ERROR_KICK_SELF, MemberRules.kickRefusal(admin, admin));
    }

    @Test
    void aRanksPermissionsChangeFromAboveOnly() {
        MemberData admin = member("Admin", 2);
        MemberData moderator = member("Moderator", 1);
        var adminRank = guild.getRanks().get(2);
        var memberRank = guild.getRanks().get(0);

        assertEquals(Msg.ERROR_RANK_TOO_HIGH, MemberRules.rankRefusal(admin, adminRank));
        assertNull(MemberRules.rankRefusal(admin, memberRank));
        assertEquals(Msg.ERROR_INSUFFICIENT_RANK, MemberRules.rankRefusal(moderator, memberRank));
        assertNull(MemberRules.rankRefusal(guild.getMember(owner.getUniqueId()), guild.getRanks().getLast()));

        RankService ranks = new RankService(f.registry, f.store, f.messages, net.artisanmc.modules.api.text.TextApi.NOOP);
        Player adminPlayer = f.platform.player(admin.getPlayerId());
        ranks.setPermission(adminPlayer, adminRank, GuildPermission.CHEST_WITHDRAW, false);
        assertTrue(adminRank.hasPermission(GuildPermission.CHEST_WITHDRAW), "unchanged");
        assertTrue(f.bundle.got(adminPlayer, Msg.ERROR_RANK_TOO_HIGH));
    }

    @Test
    void disbandingWaitsForAnEmptyChest() {
        f.chestEmpty = false;
        f.guilds.disbandGuild(owner);
        assertTrue(f.bundle.got(owner, Msg.CHEST_NOT_EMPTY_DISBAND));
        assertEquals(guild, f.registry.byName("Alpha"));

        f.chestEmpty = true;
        f.guilds.disbandGuild(owner);
        assertNull(f.registry.byName("Alpha"));
    }

    @Test
    void theChestOfALevelCarriesOnUntilALevelChangesIt() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                levels:
                  0: {xp: 0, max-members: 5}
                  2: {xp: 2, max-members: 7, chest-rows: 3, chest-pages: 1}
                  4: {xp: 4, max-members: 10, chest-rows: 6}
                  7: {xp: 7, max-members: 15, chest-pages: 2}
                  x: {xp: 9}
                """);
        var levels = GuildLeveling.read(config.getConfigurationSection("levels"), Logger.getLogger("test"));

        assertEquals(4, levels.size());
        assertEquals(0, levels.get(0).chestPages());
        assertEquals(3, levels.get(2).chestRows());
        assertEquals(1, levels.get(4).chestPages());
        assertEquals(6, levels.get(4).chestRows());
        assertEquals(2, levels.get(7).chestPages());
        assertEquals(6, levels.get(7).chestRows());
        GuildLeveling leveling = new GuildLeveling(levels, f.store);
        assertEquals(2, leveling.chestUnlockLevel());
        assertEquals(7, leveling.maxLevel());
        assertEquals(4, leveling.levelFor(5));
    }
}
