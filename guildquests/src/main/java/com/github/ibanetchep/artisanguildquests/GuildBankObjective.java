package com.github.ibanetchep.artisanguildquests;

import com.github.ibanetchep.artisanguilds.api.ArtisanGuildsApi;
import com.github.ibanetchep.artisanguilds.api.Guild;
import com.github.ibanetchep.artisanguilds.api.GuildMember;
import com.github.ibanetchep.artisanguilds.api.GuildPermission;
import com.github.ibanetchep.msquests.bukkit.quest.objective.ContributableObjective;
import com.github.ibanetchep.msquests.core.quest.actor.QuestStage;
import com.github.ibanetchep.msquests.core.quest.objective.AbstractQuestObjective;
import com.github.ibanetchep.msquests.core.quest.objective.QuestObjectiveStatus;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;

/**
 * Paid out of the guild bank in one go, once the balance covers it — never in instalments,
 * so a guild cannot drain its bank into a level it will not reach. Only members allowed to
 * withdraw from the bank may trigger the payment.
 */
public class GuildBankObjective extends AbstractQuestObjective<GuildBankObjectiveConfig> implements ContributableObjective {

    // Static because MSQuests builds objectives reflectively, with no room for services.
    private static ArtisanGuildsApi guilds;

    public GuildBankObjective(QuestStage questStage, GuildBankObjectiveConfig objectiveConfig, int progress, QuestObjectiveStatus status) {
        super(questStage, objectiveConfig, progress, status);
    }

    static void setGuilds(ArtisanGuildsApi api) {
        guilds = api;
    }

    @Override
    public Contribution contribute(Player player) {
        int amount = getTarget() - getProgress();
        Guild guild = getQuest().getActor() instanceof GuildQuestActor actor ? actor.getGuild() : null;
        GuildMember member = guild != null ? guild.getMember(player.getUniqueId()) : null;
        if (member == null) {
            // Not a guild quest (misconfigured actor_type) or not this guild's member.
            return new Contribution(0, "objective.contribute.guild_bank_no_permission", Map.of("amount", format(amount)));
        }

        if (guild.getBalance() < amount) {
            return insufficient(guild, amount);
        }
        if (!member.isGranted(GuildPermission.BANK_WITHDRAW)) {
            return new Contribution(0, "objective.contribute.guild_bank_no_permission", Map.of("amount", format(amount)));
        }
        if (!guilds.withdraw(guild, amount, "Niveau de guilde (" + player.getName() + ")")) {
            return insufficient(guild, amount);
        }
        return new Contribution(amount, "objective.contribute.guild_bank_paid", Map.of("amount", format(amount)));
    }

    private static Contribution insufficient(Guild guild, int amount) {
        return new Contribution(0, "objective.contribute.guild_bank_insufficient",
                Map.of("amount", format(amount), "balance", format((long) guild.getBalance())));
    }

    static String format(long amount) {
        return String.format(Locale.ROOT, "%,d", amount).replace(',', ' ');
    }
}
