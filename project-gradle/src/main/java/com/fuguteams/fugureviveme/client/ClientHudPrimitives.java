package com.fuguteams.fugureviveme.client;

import net.minecraft.client.gui.GuiGraphics;

final class ClientHudPrimitives {
    static final int BACKGROUND = 0xD90A0D14;
    static final int BACKGROUND_TOP = 0xE6161B26;
    static final int TEXT = 0xFFE0E0E0;
    static final int MUTED = 0xFF5E6470;
    static final int CYAN = 0xFF00F2FF;

    private ClientHudPrimitives() {
        throw new AssertionError("ClientHudPrimitives is a static utility and must not be instantiated");
    }

    static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height, int borderColor) {
        graphics.fill(x + 4, y, x + width - 4, y + height, BACKGROUND);
        graphics.fill(x, y + 4, x + width, y + height - 4, BACKGROUND);
        graphics.fillGradient(x + 2, y + 2, x + width - 2, y + height - 2, BACKGROUND_TOP, BACKGROUND);
        graphics.hLine(x + 5, x + width - 6, y, borderColor);
        graphics.hLine(x + 5, x + width - 6, y + height - 1, borderColor);
        graphics.vLine(x, y + 5, y + height - 6, borderColor);
        graphics.vLine(x + width - 1, y + 5, y + height - 6, borderColor);
    }

    static void drawProgressBar(GuiGraphics graphics, int x, int y, int width, int height, float progress, int color) {
        graphics.fill(x, y, x + width, y + height, 0xB0000000);
        int filled = Math.round(width * Math.max(0.0F, Math.min(1.0F, progress)));
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + height, color);
        }
    }

    static void drawVignette(GuiGraphics graphics, int screenWidth, int screenHeight, int color, int alpha) {
        int tinted = (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
        int bandX = Math.max(16, screenWidth / 12);
        int bandY = Math.max(16, screenHeight / 10);
        graphics.fill(0, 0, screenWidth, bandY, tinted);
        graphics.fill(0, screenHeight - bandY, screenWidth, screenHeight, tinted);
        graphics.fill(0, 0, bandX, screenHeight, tinted);
        graphics.fill(screenWidth - bandX, 0, screenWidth, screenHeight, tinted);
    }
}
