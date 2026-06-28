package com.fuguteams.fugureviveme.client;

import com.fuguteams.fugureviveme.registry.ModItems;
import com.fuguteams.fugureviveme.state.ReviveActionType;
import com.fuguteams.fugureviveme.state.ReviveState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;

final class TemporaryKoOverlay {
    private TemporaryKoOverlay() {
        throw new AssertionError("TemporaryKoOverlay is a static utility and must not be instantiated");
    }

    static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.level == null) {
            return;
        }
        ClientReviveStateStore store = ClientReviveStateStore.get();
        if (store.snapshot().state() != ReviveState.TEMPORARY_KO) {
            return;
        }
        long clientTime = minecraft.level.getGameTime();
        long remainingTicks = store.remainingKoTicks(clientTime);
        int timerColor = ClientUiFormat.temporaryTimerColor(remainingTicks);
        int pulse = remainingTicks <= 200L ? (int) (18.0D + Math.sin((clientTime + partialTick) * 0.35D) * 10.0D) : 10;
        ClientHudPrimitives.drawVignette(graphics, screenWidth, screenHeight, timerColor, pulse);

        int width = Math.min(520, screenWidth - 32);
        int height = 115;
        int x = (screenWidth - width) / 2;
        int y = Math.max(18, screenHeight - 140);
        Font font = minecraft.font;
        ClientHudPrimitives.drawPanel(graphics, x, y, width, height, timerColor);

        int centerX = x + width / 2;
        int titleColor = store.snapshot().hitsTaken() >= 2 || remainingTicks <= 200L
                ? ClientUiFormat.COLOR_RED
                : ClientUiFormat.COLOR_AMBER;
        graphics.drawCenteredString(font, Component.literal("K.O."), centerX, y + 14, titleColor);
        graphics.drawCenteredString(font, Component.literal(ClientUiFormat.formatTicks(remainingTicks)),
                centerX, y + 31, ClientHudPrimitives.TEXT);

        int barWidth = Math.min(300, width - 80);
        int barX = centerX - barWidth / 2;
        ClientHudPrimitives.drawProgressBar(graphics, barX, y + 48, barWidth, 6, store.koProgress(clientTime), timerColor);

        drawHits(graphics, font, x + 28, y + 68, store.snapshot().hitsTaken());
        drawAction(graphics, font, x + width - 205, y + 68, store, clientTime, hasSoulAnchor(minecraft.player.getInventory()));
    }

    private static void drawHits(GuiGraphics graphics, Font font, int x, int y, int hitsTaken) {
        graphics.drawString(font, Component.translatable("overlay.fugu_revive_me.hits"), x, y, ClientHudPrimitives.TEXT, false);
        for (int index = 0; index < 3; index++) {
            int segmentX = x + index * 22;
            int color = index < hitsTaken ? ClientUiFormat.COLOR_RED : ClientHudPrimitives.MUTED;
            graphics.fill(segmentX, y + 14, segmentX + 14, y + 28, color);
            graphics.hLine(segmentX, segmentX + 14, y + 14, ClientHudPrimitives.TEXT);
            graphics.hLine(segmentX, segmentX + 14, y + 28, ClientHudPrimitives.TEXT);
            graphics.vLine(segmentX, y + 14, y + 28, ClientHudPrimitives.TEXT);
            graphics.vLine(segmentX + 14, y + 14, y + 28, ClientHudPrimitives.TEXT);
        }
    }

    private static void drawAction(
            GuiGraphics graphics,
            Font font,
            int x,
            int y,
            ClientReviveStateStore store,
            long clientTime,
            boolean hasSoulAnchor
    ) {
        ReviveActionType actionType = store.snapshot().actionType();
        Component label;
        int color;
        if (actionType == ReviveActionType.ALLY_REVIVE) {
            label = Component.translatable("overlay.fugu_revive_me.ally_revive");
            color = ClientHudPrimitives.CYAN;
        } else if (actionType == ReviveActionType.SELF_REVIVE) {
            label = Component.translatable("overlay.fugu_revive_me.soul_anchor.casting");
            color = ClientHudPrimitives.CYAN;
        } else if (hasSoulAnchor) {
            label = Component.translatable("overlay.fugu_revive_me.soul_anchor.available");
            color = ClientUiFormat.COLOR_AMBER;
        } else {
            label = Component.translatable("overlay.fugu_revive_me.soul_anchor.missing");
            color = ClientHudPrimitives.MUTED;
        }
        graphics.drawString(font, label, x, y, color, false);
        if (actionType == ReviveActionType.ALLY_REVIVE || actionType == ReviveActionType.SELF_REVIVE) {
            ClientHudPrimitives.drawProgressBar(graphics, x, y + 16, 170, 5,
                    store.actionProgress(clientTime), ClientHudPrimitives.CYAN);
        }
    }

    private static boolean hasSoulAnchor(Inventory inventory) {
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.SOUL_ANCHOR.get())) {
                return true;
            }
        }
        return false;
    }
}
