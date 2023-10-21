package fr.iban.guilds.util;

import java.util.UUID;

public class GuildRequestMessage {

    private UUID senderID;
    private UUID targetID;

    public GuildRequestMessage(UUID guildID, UUID playerID) {
        this.senderID = guildID;
        this.targetID = playerID;
    }

    public UUID getSenderID() {
        return senderID;
    }

    public void setSenderID(UUID senderID) {
        this.senderID = senderID;
    }

    public UUID getTargetID() {
        return targetID;
    }

    public void setTargetID(UUID targetID) {
        this.targetID = targetID;
    }
}
