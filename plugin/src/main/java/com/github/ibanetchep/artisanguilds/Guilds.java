package com.github.ibanetchep.artisanguilds;

import com.github.ibanetchep.artisanguilds.chest.GuildChestService;
import com.github.ibanetchep.artisanguilds.model.GuildRegistry;
import com.github.ibanetchep.artisanguilds.service.AllianceService;
import com.github.ibanetchep.artisanguilds.service.BankService;
import com.github.ibanetchep.artisanguilds.service.GuildLeveling;
import com.github.ibanetchep.artisanguilds.service.GuildService;
import com.github.ibanetchep.artisanguilds.service.RankService;
import com.github.ibanetchep.artisanguilds.storage.GuildStore;
import com.github.ibanetchep.artisanguilds.sync.GuildSync;

/** Everything a running ArtisanGuilds holds, for the commands and menus to come. */
public record Guilds(GuildRegistry registry, GuildStore store, GuildLeveling leveling, GuildSync sync,
                     GuildService guilds, BankService bank, RankService ranks, AllianceService alliances,
                     GuildChestService chest) {}
