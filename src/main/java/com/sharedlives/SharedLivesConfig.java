package com.sharedlives;

import net.neoforged.neoforge.common.ModConfigSpec;

public class SharedLivesConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Enable the Shared Lives system. Set to false for vanilla hardcore behaviour.")
            .define("enabled", true);

    public static final ModConfigSpec.IntValue INITIAL_LIVES = BUILDER
            .comment("Number of shared lives when a new world is created. Min 1, max 1000.")
            .defineInRange("initialLives", 5, 1, 1000);

    public static final ModConfigSpec.BooleanValue SHOW_PLAYER_HEARTS = BUILDER
            .comment("Automatically show player health in the tab list on world creation (scoreboard objectives add h health).")
            .define("showPlayerHearts", true);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
