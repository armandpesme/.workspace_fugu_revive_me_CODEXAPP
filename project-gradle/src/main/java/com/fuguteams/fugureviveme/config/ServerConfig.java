package com.fuguteams.fugureviveme.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class ServerConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<String> DEATH_RESPAWN_DIMENSION_ID;
    public static final ForgeConfigSpec.IntValue DEATH_RESPAWN_X;
    public static final ForgeConfigSpec.IntValue DEATH_RESPAWN_Y;
    public static final ForgeConfigSpec.IntValue DEATH_RESPAWN_Z;
    public static final ForgeConfigSpec.DoubleValue DEATH_RESPAWN_YAW;
    public static final ForgeConfigSpec.DoubleValue DEATH_RESPAWN_PITCH;

    public static final ForgeConfigSpec.BooleanValue TEMPORARY_KO_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TEMPORARY_KO_BIOMES;
    public static final ForgeConfigSpec.IntValue TEMPORARY_KO_DURATION_TICKS;
    public static final ForgeConfigSpec.IntValue TEMPORARY_KO_MAX_HITS;
    public static final ForgeConfigSpec.IntValue TEMPORARY_KO_REVIVE_DURATION_TICKS;
    public static final ForgeConfigSpec.DoubleValue TEMPORARY_KO_REVIVE_MAX_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue TEMPORARY_KO_REVIVED_HEALTH_PERCENT;

    public static final ForgeConfigSpec.BooleanValue PROLONGED_KO_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PROLONGED_KO_BIOMES;
    public static final ForgeConfigSpec.IntValue PROLONGED_KO_DURATION_TICKS;
    public static final ForgeConfigSpec.IntValue PROLONGED_KO_MAX_HITS;
    public static final ForgeConfigSpec.DoubleValue PROLONGED_KO_BOSS_SEARCH_RADIUS;
    public static final ForgeConfigSpec.ConfigValue<String> PROLONGED_KO_BOSS_TAG;

    public static final ForgeConfigSpec.IntValue RESURRECTION_SICKNESS_DURATION_TICKS;

    public static final ForgeConfigSpec.ConfigValue<String> RETURN_PENDANT_MAIN_DIMENSION;
    public static final ForgeConfigSpec.IntValue RETURN_PENDANT_CAST_TIME_TICKS;
    public static final ForgeConfigSpec.IntValue RETURN_PENDANT_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.DoubleValue RETURN_PENDANT_MOVEMENT_TOLERANCE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("death_respawn");
        DEATH_RESPAWN_DIMENSION_ID = builder.define(
                "dimension_id",
                "fugubiomes:fugu_royaume_des_esprits",
                ResourceIdValidator::isValid
        );
        DEATH_RESPAWN_X = builder.defineInRange("x", 2428, -30_000_000, 30_000_000);
        DEATH_RESPAWN_Y = builder.defineInRange("y", 66, -2048, 2048);
        DEATH_RESPAWN_Z = builder.defineInRange("z", -1805, -30_000_000, 30_000_000);
        DEATH_RESPAWN_YAW = builder.defineInRange("yaw", 0.0, -180.0, 180.0);
        DEATH_RESPAWN_PITCH = builder.defineInRange("pitch", 0.0, -90.0, 90.0);
        builder.pop();

        builder.push("temporary_ko");
        TEMPORARY_KO_ENABLED = builder.define("enabled", true);
        TEMPORARY_KO_BIOMES = builder.defineListAllowEmpty(
                "biomes",
                KoBiomeDefaults.TEMPORARY_BIOMES,
                ResourceIdValidator::isValid
        );
        TEMPORARY_KO_DURATION_TICKS = positiveInt(builder, "duration_ticks", 1200);
        TEMPORARY_KO_MAX_HITS = positiveInt(builder, "max_hits", 3);
        TEMPORARY_KO_REVIVE_DURATION_TICKS = positiveInt(builder, "revive_duration_ticks", 100);
        TEMPORARY_KO_REVIVE_MAX_DISTANCE = builder.defineInRange(
                "revive_max_distance",
                3.0,
                0.0,
                1024.0
        );
        TEMPORARY_KO_REVIVED_HEALTH_PERCENT = builder.defineInRange(
                "revived_health_percent",
                25.0,
                0.0,
                100.0
        );
        builder.pop();

        builder.push("prolonged_ko");
        PROLONGED_KO_ENABLED = builder.define("enabled", true);
        PROLONGED_KO_BIOMES = builder.defineListAllowEmpty(
                "biomes",
                KoBiomeDefaults.PROLONGED_BIOMES,
                ResourceIdValidator::isValid
        );
        PROLONGED_KO_DURATION_TICKS = positiveInt(builder, "duration_ticks", 6000);
        PROLONGED_KO_MAX_HITS = positiveInt(builder, "max_hits", 3);
        PROLONGED_KO_BOSS_SEARCH_RADIUS = builder.defineInRange(
                "boss_search_radius",
                20.0,
                0.0,
                1024.0
        );
        PROLONGED_KO_BOSS_TAG = builder.define(
                "boss_tag",
                "fugu_revive_me:fugu_boss",
                ResourceIdValidator::isValid
        );
        builder.pop();

        builder.push("resurrection_sickness");
        RESURRECTION_SICKNESS_DURATION_TICKS = positiveInt(builder, "duration_ticks", 6000);
        builder.pop();

        builder.push("return_pendant");
        RETURN_PENDANT_MAIN_DIMENSION = builder.define(
                "main_dimension",
                "minecraft:overworld",
                ResourceIdValidator::isValid
        );
        RETURN_PENDANT_CAST_TIME_TICKS = positiveInt(builder, "cast_time_ticks", 600);
        RETURN_PENDANT_COOLDOWN_TICKS = positiveInt(builder, "cooldown_ticks", 6000);
        RETURN_PENDANT_MOVEMENT_TOLERANCE = builder.defineInRange(
                "movement_tolerance",
                0.1,
                0.0,
                1024.0
        );
        builder.pop();

        SPEC = builder.build();
    }

    private ServerConfig() {
    }

    private static ForgeConfigSpec.IntValue positiveInt(
            ForgeConfigSpec.Builder builder,
            String path,
            int defaultValue
    ) {
        return builder.defineInRange(path, defaultValue, 1, Integer.MAX_VALUE);
    }
}
