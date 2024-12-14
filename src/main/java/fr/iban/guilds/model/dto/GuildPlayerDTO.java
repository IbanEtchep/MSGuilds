package fr.iban.guilds.model.dto;

import fr.iban.guilds.enums.ChatMode;

import java.util.UUID;

public record GuildPlayerDTO(UUID uuid, UUID guildId, String rank, ChatMode chatMode) {
}