package com.github.ibanetchep.artisanguilds.model;

import com.github.ibanetchep.artisanguilds.api.GuildPermission;
import com.github.ibanetchep.artisanguilds.api.GuildRank;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public final class RankData implements GuildRank {

    private final UUID id;
    private volatile String name;
    private volatile int order;
    private final Set<GuildPermission> permissions;

    public RankData(UUID id, String name, int order, Set<GuildPermission> permissions) {
        this.id = id;
        this.name = name;
        this.order = order;
        this.permissions = Collections.synchronizedSet(permissions.isEmpty()
                ? EnumSet.noneOf(GuildPermission.class) : EnumSet.copyOf(permissions));
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int getOrder() {
        return order;
    }

    void setOrder(int order) {
        this.order = order;
    }

    @Override
    public Set<GuildPermission> getPermissions() {
        synchronized (permissions) {
            return Set.copyOf(permissions);
        }
    }

    public void setPermission(GuildPermission permission, boolean granted) {
        if (granted) permissions.add(permission);
        else permissions.remove(permission);
    }
}
