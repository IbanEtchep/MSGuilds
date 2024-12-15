package fr.iban.guilds.util;

import java.util.UUID;

public record GuildRequestMessage (UUID senderID, UUID targetID) {}
