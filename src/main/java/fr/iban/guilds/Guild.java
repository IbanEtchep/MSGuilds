package fr.iban.guilds;

import fr.iban.bukkitcore.CoreBukkitPlugin;
import fr.iban.common.teleport.SLocation;
import fr.iban.guilds.enums.Rank;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Guild {

    private UUID id;
    private String name;
    private final Map<UUID, GuildPlayer> members = new ConcurrentHashMap<>();
    private double balance;
    private long exp;
    private SLocation home;
    private final Date createdAt;
    private @Nullable List<String> cachedLogs;
    private final List<UUID> invites = new ArrayList<>();
    private final List<UUID> allianceInvites = new ArrayList<>();

    private final List<Guild> alliances = new ArrayList<>();

    public Guild(UUID id, String name, double balance, long exp, Date createdAt) {
        this.id = id;
        this.name = name;
        this.balance = balance;
        this.exp = exp;
        this.createdAt = createdAt;
    }

    public Guild(String name) {
        this(UUID.randomUUID(), name, 0, 0, new Date());
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<UUID, GuildPlayer> getMembers() {
        return members;
    }

    public GuildPlayer getOwner() {
        return getMembers().values().stream()
                .filter(guildPlayer -> guildPlayer.getRank() == Rank.OWNER)
                .findFirst().orElse(null);
    }

    public GuildPlayer getMember(UUID uuid) {
        return members.get(uuid);
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public long getExp() {
        return exp;
    }

    public void setExp(long exp) {
        this.exp = exp;
    }

    public SLocation getHome() {
        return home;
    }

    public void setHome(SLocation home) {
        this.home = home;
    }

    public void sendMessageToOnlineMembers(String message, boolean raw) {
        getMembers().values().forEach(member -> member.sendMessageIfOnline(message, raw));
    }

    public void sendMessageToOnlineMembers(String message) {
        sendMessageToOnlineMembers(message, false);
    }

    public void sendMessageToAllies(String message) {
        getAlliances().forEach(guild -> guild.sendMessageToOnlineMembers(message));
        sendMessageToOnlineMembers(message);
    }

    public void sendMessageToOnlineMembers(String message, Rank minRank, boolean raw) {
        getMembers().values().stream()
                .filter(member -> member.isGranted(minRank))
                .forEach(member -> member.sendMessageIfOnline(message, raw));
    }

    public List<UUID> getInvites() {
        return invites;
    }

    public String getDate() {
        return new SimpleDateFormat("dd/MM/yy HH:mm:ss").format(createdAt);
    }

    public List<UUID> getOnlinePlayers() {
        return CoreBukkitPlugin.getInstance().getPlayerManager().getOnlinePlayers().keySet().stream()
                .filter(uuid -> getMembers().containsKey(uuid)).toList();
    }

    public int getOnlinePlayerAmount() {
        return getOnlinePlayers().size();
    }

    public List<String> getCachedLogs() {
        return cachedLogs;
    }

    public void setCachedLogs(List<String> cachedLogs) {
        this.cachedLogs = cachedLogs;
    }

    public void invalidateLogs() {
        this.cachedLogs = null;
    }

    public List<UUID> getAllianceInvites() {
        return allianceInvites;
    }

    public List<Guild> getAlliances() {
        return alliances;
    }
}
