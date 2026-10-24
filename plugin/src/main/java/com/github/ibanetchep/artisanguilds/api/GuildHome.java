package com.github.ibanetchep.artisanguilds.api;

import net.artisanmc.modules.api.network.NetworkLocation;

/**
 * A guild's home, on any server of the network. {@code server} is Artisan's name of the server
 * ({@code name} in {@code environment.yml}); empty for a server alone.
 */
public record GuildHome(String server, String world, double x, double y, double z, float yaw, float pitch) {

    public NetworkLocation toLocation() {
        return new NetworkLocation(server, world, x, y, z, yaw, pitch);
    }

    public static GuildHome of(NetworkLocation l) {
        return new GuildHome(l.getServer(), l.getWorld(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
    }
}
