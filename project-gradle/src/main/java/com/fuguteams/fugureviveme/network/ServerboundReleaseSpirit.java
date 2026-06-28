package com.fuguteams.fugureviveme.network;

import net.minecraft.network.FriendlyByteBuf;

public enum ServerboundReleaseSpirit {
    INSTANCE;

    public void encode(FriendlyByteBuf buffer) {
    }

    public static ServerboundReleaseSpirit decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }
}
