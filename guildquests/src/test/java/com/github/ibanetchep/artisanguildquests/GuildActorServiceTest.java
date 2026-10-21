package com.github.ibanetchep.artisanguildquests;

import com.github.ibanetchep.artisanguilds.api.ArtisanGuildsApi;
import com.github.ibanetchep.artisanguilds.api.Guild;
import com.github.ibanetchep.msquests.core.quest.actor.QuestActor;
import com.github.ibanetchep.msquests.core.registry.QuestActorRegistry;
import com.github.ibanetchep.msquests.core.service.QuestActorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GuildActorServiceTest {

    private final QuestActorService actors = mock(QuestActorService.class);
    private final QuestActorRegistry registry = mock(QuestActorRegistry.class);
    private final ArtisanGuildsApi guilds = mock(ArtisanGuildsApi.class);
    private final Map<UUID, QuestActor> loaded = new HashMap<>();
    private GuildActorService service;

    @BeforeEach
    void setUp() {
        when(registry.getActors()).thenReturn(loaded);
        service = new GuildActorService(actors, registry, guilds);
    }

    private static Guild guild(String name) {
        return GuildQuestActorTest.guild(UUID.randomUUID(), name, Map.of());
    }

    @Test
    void loadsEveryGuildAtStart() {
        Guild alpha = guild("Alpha");
        Guild beta = guild("Beta");
        when(guilds.getGuilds()).thenReturn(List.of(alpha, beta));

        assertEquals(2, service.loadAll());

        ArgumentCaptor<QuestActor> loadedActor = ArgumentCaptor.forClass(QuestActor.class);
        verify(actors, times(2)).loadActor(loadedActor.capture());
        assertSame(alpha, ((GuildQuestActor) loadedActor.getAllValues().getFirst()).getGuild());
    }

    @Test
    void aMembershipChangeRefreshesTheKnownActor() {
        Guild alpha = guild("Alpha");
        GuildQuestActor actor = new GuildQuestActor(alpha);
        loaded.put(alpha.getId(), actor);

        service.refreshMembership(alpha);

        verify(actors).refreshActorMembership(actor);
        verify(actors, never()).loadActor(any());
    }

    @Test
    void aMembershipChangeOnAnUnknownGuildLoadsIt() {
        Guild alpha = guild("Alpha");

        service.refreshMembership(alpha);

        verify(actors).loadActor(any(GuildQuestActor.class));
        verify(actors, never()).refreshActorMembership(any());
    }

    @Test
    void unloadsADisbandedGuildAndIgnoresOtherActorTypes() {
        UUID id = UUID.randomUUID();
        loaded.put(id, mock(QuestActor.class));

        assertNull(service.getActor(id));
        service.unload(id);
        verify(actors).unloadActor(id);
    }
}
