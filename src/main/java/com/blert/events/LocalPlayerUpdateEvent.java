package com.blert.events;

import com.blert.raid.Hitpoints;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

import java.util.Optional;

/**
 * PlayerUpdateEvent which returns information about the logged-in player from the local client.
 */
public class LocalPlayerUpdateEvent extends PlayerUpdateEvent {
    private final Client client;

    public LocalPlayerUpdateEvent(int tick, Client client) {
        super(tick, WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()));
        this.client = client;
    }

    @Override
    public String getUsername() {
        return client.getLocalPlayer().getName();
    }

    @Override
    public Optional<Hitpoints> getHitpoints() {
        return Optional.of(
                new Hitpoints(client.getBoostedSkillLevel(Skill.HITPOINTS), client.getRealSkillLevel(Skill.HITPOINTS)));
    }
}
