package com.sunrisestudio.vipaccess.api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TempAccessGrantEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final UUID playerUUID;
    private final long expiryTime;

    public TempAccessGrantEvent(UUID playerUUID, long expiryTime) {
        this.playerUUID = playerUUID;
        this.expiryTime = expiryTime;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}