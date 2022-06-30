package fr.iban.guilds.util;

import java.util.UUID;

public class GuildRequestMessage {

    private UUID guildID;
    private UUID playerID;

    public GuildRequestMessage(UUID guildID, UUID playerID) {
        this.guildID = guildID;
        this.playerID = playerID;
    }

    public UUID getGuildID() {
        return guildID;
    }

    public void setGuildID(UUID guildID) {
        this.guildID = guildID;
    }

    public UUID getPlayerID() {
        return playerID;
    }

    public void setPlayerID(UUID playerID) {
        this.playerID = playerID;
    }
}
