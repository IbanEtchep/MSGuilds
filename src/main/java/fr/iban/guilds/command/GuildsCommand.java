package fr.iban.guilds.command;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;

public class GuildsCommand {

    @Command("guilds")
    public void guilds(Player sender) {
        Bukkit.broadcastMessage("heheeee");
    }

}
