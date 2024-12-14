package fr.iban.guilds.api.service;

import fr.iban.guilds.model.GuildRank;
import org.bukkit.entity.Player;

public interface GuildRankService {
    void rankMoveUp(Player player, GuildRank rank);
    void rankMoveDown(Player player, GuildRank rank);
    void deleteRank(Player player, GuildRank rank);
    void renameRank(Player player, GuildRank rank, String newName);
}
