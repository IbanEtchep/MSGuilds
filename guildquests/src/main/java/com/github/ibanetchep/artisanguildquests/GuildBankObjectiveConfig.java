package com.github.ibanetchep.artisanguildquests;

import com.github.ibanetchep.msquests.core.dto.QuestObjectiveConfigDTO;
import com.github.ibanetchep.msquests.core.lang.Translator;
import com.github.ibanetchep.msquests.core.quest.config.QuestObjectiveConfig;
import com.github.ibanetchep.msquests.core.quest.config.annotation.ConfigField;
import com.github.ibanetchep.msquests.core.quest.config.annotation.ObjectiveType;

import java.util.Map;

/** Pay {@code amount} out of the guild bank, all at once. */
@ObjectiveType(GuildBankObjectiveConfig.TYPE)
public class GuildBankObjectiveConfig extends QuestObjectiveConfig {

    public static final String TYPE = "guild_bank";

    @ConfigField(name = "amount", required = true)
    private final int amount;

    public GuildBankObjectiveConfig(QuestObjectiveConfigDTO dto) {
        super(dto);
        this.amount = ((Number) dto.params().get("amount")).intValue();
    }

    @Override
    public QuestObjectiveConfigDTO toDTO() {
        return new QuestObjectiveConfigDTO(getKey(), getType(), Map.of("amount", amount));
    }

    @Override
    public int getTarget() {
        return amount;
    }

    @Override
    public Map<String, String> getPlaceholders(Translator translator) {
        return Map.of("amount", GuildBankObjective.format(amount));
    }
}
