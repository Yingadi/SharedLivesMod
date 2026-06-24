package com.sharedlives;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class SharedLivesSavedData extends SavedData {
    private static final String DATA_NAME = SharedLivesMod.MOD_ID + "_data";
    private static final String KEY_LIVES = "lives";
    private static final String KEY_GAME_OVER = "gameOver";
    private static final String KEY_CONFIGURED = "configured";
    private static final String KEY_SETUP_ENABLED = "setupEnabled";

    private int lives;
    private boolean gameOver;
    private boolean configured;
    private boolean setupEnabled;

    private SharedLivesSavedData(int initialLives) {
        this.lives = initialLives;
        this.gameOver = false;
        this.configured = false;
        this.setupEnabled = true;
    }

    public static SharedLivesSavedData get(MinecraftServer server) {
        int initialLives = SharedLivesConfig.INITIAL_LIVES.get();
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(
                        () -> new SharedLivesSavedData(initialLives),
                        SharedLivesSavedData::load,
                        null
                ),
                DATA_NAME
        );
    }

    public static SharedLivesSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        SharedLivesSavedData data = new SharedLivesSavedData(5);
        data.lives = tag.getInt(KEY_LIVES);
        data.gameOver = tag.getBoolean(KEY_GAME_OVER);
        // Old saves don't have KEY_CONFIGURED → treat them as already configured so the mod keeps working.
        data.configured = !tag.contains(KEY_CONFIGURED) || tag.getBoolean(KEY_CONFIGURED);
        data.setupEnabled = !tag.contains(KEY_SETUP_ENABLED) || tag.getBoolean(KEY_SETUP_ENABLED);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt(KEY_LIVES, lives);
        tag.putBoolean(KEY_GAME_OVER, gameOver);
        tag.putBoolean(KEY_CONFIGURED, configured);
        tag.putBoolean(KEY_SETUP_ENABLED, setupEnabled);
        return tag;
    }

    public void configure(boolean enabled, int initialLives) {
        this.setupEnabled = enabled;
        this.lives = Math.max(1, initialLives);
        this.configured = true;
        setDirty();
    }

    public int getLives() { return lives; }
    public boolean isGameOver() { return gameOver; }
    public boolean isConfigured() { return configured; }
    public boolean isSetupEnabled() { return setupEnabled; }

    public int decrementAndGet() {
        if (lives > 0) lives--;
        if (lives == 0) gameOver = true;
        setDirty();
        return lives;
    }

    public int addLives(int amount) {
        lives = Math.max(0, lives + amount);
        gameOver = (lives == 0);
        setDirty();
        return lives;
    }
}
