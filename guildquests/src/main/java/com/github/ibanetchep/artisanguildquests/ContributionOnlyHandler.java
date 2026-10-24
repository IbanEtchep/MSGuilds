package com.github.ibanetchep.artisanguildquests;

import com.github.ibanetchep.msquests.core.platform.MSQuestsPlatform;
import com.github.ibanetchep.msquests.core.quest.objective.QuestObjective;
import com.github.ibanetchep.msquests.core.quest.objective.QuestObjectiveHandler;

/** For objectives advanced only through {@code msquests:contribute}: no game event to listen to. */
class ContributionOnlyHandler<T extends QuestObjective> extends QuestObjectiveHandler<T> {

    private final String type;

    ContributionOnlyHandler(MSQuestsPlatform platform, String type) {
        super(platform);
        this.type = type;
    }

    @Override
    protected String getObjectiveType() {
        return type;
    }

    @Override
    public void init() {}

    @Override
    public void destroy() {}
}
