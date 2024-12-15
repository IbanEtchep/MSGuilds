package fr.iban.guilds.model;

import fr.iban.bukkitcore.CoreBukkitPlugin;
import fr.iban.common.teleport.SLocation;
import fr.iban.guilds.enums.DefaultRank;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Guild {

    private UUID id;
    private String name;
    private UUID owner;
    private final Map<UUID, GuildPlayer> members = new ConcurrentHashMap<>();
    private double balance;
    private long exp;
    private SLocation home;
    private final Date createdAt;
    private @Nullable List<String> cachedLogs;
    private final List<UUID> invites = new ArrayList<>();
    private final List<UUID> allianceInvites = new ArrayList<>();

    private final List<Guild> alliances = new ArrayList<>();

    private List<GuildRank> ranks = new ArrayList<>();

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

    public List<GuildPlayer> getMembersByRank(GuildRank rank) {
        return members.values().stream().filter(member -> member.getRank().equals(rank)).toList();
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

    public void sendMessageToOnlineMembers(Component message) {
        getMembers().values().forEach(member -> member.sendMessageIfOnline(message));
    }

    public void sendMessageToAllies(Component message) {
        getAlliances().forEach(guild -> guild.sendMessageToOnlineMembers(message));
        sendMessageToOnlineMembers(message);
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

    public @Nullable List<String> getCachedLogs() {
        return cachedLogs;
    }

    public void setCachedLogs(@Nullable List<String> cachedLogs) {
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

    public UUID getOwnerUUID() {
        return owner;
    }

    public void setOwnerUUID(UUID owner) {
        this.owner = owner;
    }

    public List<GuildRank> getRanks() {
        return ranks.stream().sorted(Comparator.comparingInt(GuildRank::getOrder)).toList();
    }

    public void setRanks(List<GuildRank> ranks) {
        this.ranks = ranks;

        for (GuildRank rank : ranks) {
            rank.setGuild(this);
        }
    }

    public void addRank(GuildRank rank) {
        rank.setOrder(ranks.size());
        rank.setGuild(this);
        this.ranks.add(rank);
    }

    public void moveRankDown(GuildRank rank) {
        int index = ranks.indexOf(rank);
        if(index == 0) return;
        GuildRank previous = ranks.get(index - 1);
        ranks.set(index - 1, rank);
        ranks.set(index, previous);
        rank.setOrder(index - 1);
        previous.setOrder(index);
    }

    public void moveRankUp(GuildRank rank) {
        int index = ranks.indexOf(rank);
        if(index == ranks.size() - 1) return;
        GuildRank next = ranks.get(index + 1);
        ranks.set(index + 1, rank);
        ranks.set(index, next);
        rank.setOrder(index + 1);
        next.setOrder(index);
    }

    public GuildRank getNextRank(GuildRank rank) {
        int index = ranks.indexOf(rank);
        if(index == ranks.size() - 1) return null;
        return ranks.get(index + 1);
    }

    public GuildRank getPreviousRank(GuildRank rank) {
        int index = ranks.indexOf(rank);
        if(index == 0) return null;
        return ranks.get(index - 1);
    }

    public void removeRank(GuildRank rank) {
        this.ranks.remove(rank);
    }

    public GuildRank getRank(String name) {
        return ranks.stream().filter(rank -> rank.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public GuildRank getDefautRank() {
        return ranks.get(0);
    }
}
