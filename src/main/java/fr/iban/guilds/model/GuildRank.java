package fr.iban.guilds.model;

import fr.iban.guilds.enums.GuildPermission;
import fr.iban.guilds.util.ChatUtils;
import net.kyori.adventure.text.Component;

import java.util.Set;
import java.util.UUID;

public class GuildRank {

    private UUID id;
    private String name;
    private Set<GuildPermission> permissions;
    private int order;
    private Guild guild;

    public GuildRank(UUID id, String name, int order, Set<GuildPermission> permissions) {
        this.id = id;
        this.name = name;
        this.order = order;
        this.permissions = permissions;
    }

    public String getName() {
        return name;
    }

    public Component getDisplayName() {
        return ChatUtils.parseMiniMessage(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<GuildPermission> getPermissions() {
        return permissions;
    }

    public boolean hasPermission(GuildPermission permission) {
        return permissions.contains(permission);
    }

    public void addPermission(GuildPermission permission) {
        permissions.add(permission);
    }

    public void removePermission(GuildPermission permission) {
        permissions.remove(permission);
    }

    public void setPermissions(Set<GuildPermission> permissions) {
        this.permissions = permissions;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Guild getGuild() {
        return guild;
    }

    public void setGuild(Guild guild) {
        this.guild = guild;
    }
}
