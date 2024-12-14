package fr.iban.guilds.zmenu.data;

import fr.iban.guilds.model.Guild;
import fr.iban.guilds.model.GuildRank;
import fr.maxlego08.menu.api.Inventory;

public class GuildMenuData {

    private Guild guild;
    private GuildRank currentRank;

    public GuildMenuData(Guild guild) {
        this.guild = guild;
    }

    public Guild getGuild() {
        return guild;
    }

    public void setGuild(Guild guild) {
        this.guild = guild;
    }

    public GuildRank getCurrentRank() {
        return currentRank;
    }

    public void setCurrentRank(GuildRank currentRank) {
        this.currentRank = currentRank;
    }
}
