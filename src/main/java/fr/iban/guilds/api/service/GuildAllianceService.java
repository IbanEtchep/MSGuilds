package fr.iban.guilds.api.service;

import fr.iban.guilds.model.Guild;
import org.bukkit.entity.Player;

public interface GuildAllianceService {
    void sendAllianceRequest(Player player, Guild targetGuild);
    void acceptAllianceRequest(Player player, Guild targetGuild);
    void revokeAlliance(Player sender, Guild target);
}
