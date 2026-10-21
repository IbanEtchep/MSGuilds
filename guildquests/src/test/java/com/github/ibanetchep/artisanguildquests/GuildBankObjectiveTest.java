package com.github.ibanetchep.artisanguildquests;

import com.github.ibanetchep.artisanguilds.api.ArtisanGuildsApi;
import com.github.ibanetchep.artisanguilds.api.Guild;
import com.github.ibanetchep.artisanguilds.api.GuildMember;
import com.github.ibanetchep.artisanguilds.api.GuildPermission;
import com.github.ibanetchep.msquests.bukkit.quest.objective.ContributableObjective.Contribution;
import com.github.ibanetchep.msquests.core.dto.QuestObjectiveConfigDTO;
import com.github.ibanetchep.msquests.core.quest.actor.Quest;
import com.github.ibanetchep.msquests.core.quest.actor.QuestActor;
import com.github.ibanetchep.msquests.core.quest.actor.QuestStage;
import com.github.ibanetchep.msquests.core.quest.objective.QuestObjectiveStatus;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GuildBankObjectiveTest {

    private final ArtisanGuildsApi guilds = mock(ArtisanGuildsApi.class);
    private final UUID playerId = UUID.randomUUID();
    private final Player player = mock(Player.class);
    private final GuildMember member = mock(GuildMember.class);
    private Guild guild;

    @BeforeEach
    void setUp() {
        GuildBankObjective.setGuilds(guilds);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("Iban");
        guild = GuildQuestActorTest.guild(UUID.randomUUID(), "Alpha", Map.of(playerId, member));
        when(guild.getMember(playerId)).thenReturn(member);
    }

    private GuildBankObjective objective(QuestActor actor, int amount) {
        Quest quest = mock(Quest.class);
        when(quest.getActor()).thenReturn(actor);
        QuestStage stage = mock(QuestStage.class);
        when(stage.getQuest()).thenReturn(quest);
        GuildBankObjectiveConfig config = new GuildBankObjectiveConfig(
                new QuestObjectiveConfigDTO("pay", GuildBankObjectiveConfig.TYPE, Map.of("amount", amount)));
        return new GuildBankObjective(stage, config, 0, QuestObjectiveStatus.IN_PROGRESS);
    }

    @Test
    void paysTheWholeAmountOnceTheBankCoversIt() {
        when(guild.getBalance()).thenReturn(20_000.0);
        when(member.isGranted(GuildPermission.BANK_WITHDRAW)).thenReturn(true);
        when(guilds.withdraw(eq(guild), eq(15_000.0), anyString())).thenReturn(true);

        Contribution paid = objective(new GuildQuestActor(guild), 15_000).contribute(player);

        assertEquals(15_000, paid.amount());
        assertEquals("objective.contribute.guild_bank_paid", paid.messageKey());
        assertEquals("15 000", paid.placeholders().get("amount"));
    }

    @Test
    void neverPaysInInstalments() {
        when(guild.getBalance()).thenReturn(10_000.0);
        when(member.isGranted(GuildPermission.BANK_WITHDRAW)).thenReturn(true);

        Contribution refused = objective(new GuildQuestActor(guild), 15_000).contribute(player);

        assertEquals(0, refused.amount());
        assertEquals("objective.contribute.guild_bank_insufficient", refused.messageKey());
        verify(guilds, never()).withdraw(eq(guild), anyDouble(), anyString());
    }

    @Test
    void needsTheBankWithdrawPermission() {
        when(guild.getBalance()).thenReturn(20_000.0);

        Contribution refused = objective(new GuildQuestActor(guild), 15_000).contribute(player);

        assertEquals("objective.contribute.guild_bank_no_permission", refused.messageKey());
        verify(guilds, never()).withdraw(eq(guild), anyDouble(), anyString());
    }

    @Test
    void refusesAPlayerOutsideTheGuildOrAQuestOfAnotherActorType() {
        Player stranger = mock(Player.class);
        when(stranger.getUniqueId()).thenReturn(UUID.randomUUID());

        assertEquals(0, objective(new GuildQuestActor(guild), 100).contribute(stranger).amount());
        assertEquals(0, objective(mock(QuestActor.class), 100).contribute(player).amount());
    }

    @Test
    void aFailedWithdrawalPaysNothing() {
        when(guild.getBalance()).thenReturn(20_000.0);
        when(member.isGranted(GuildPermission.BANK_WITHDRAW)).thenReturn(true);
        when(guilds.withdraw(eq(guild), eq(15_000.0), anyString())).thenReturn(false);

        assertEquals("objective.contribute.guild_bank_insufficient",
                objective(new GuildQuestActor(guild), 15_000).contribute(player).messageKey());
    }
}
