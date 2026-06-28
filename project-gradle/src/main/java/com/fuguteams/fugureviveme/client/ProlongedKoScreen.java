package com.fuguteams.fugureviveme.client;

import com.fuguteams.fugureviveme.network.FuguNetwork;
import com.fuguteams.fugureviveme.network.ServerboundReleaseSpirit;
import com.fuguteams.fugureviveme.state.ReviveState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class ProlongedKoScreen extends Screen {
    private int buttonX;
    private int buttonY;
    private int buttonWidth;
    private int buttonHeight;
    private long feedbackUntilGameTime;

    public ProlongedKoScreen() {
        super(Component.translatable("screen.fugu_revive_me.prolonged_ko.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        ReviveState state = ClientReviveStateStore.get().snapshot().state();
        if (state != ReviveState.PROLONGED_KO && state != ReviveState.FULLY_DOWNED && minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft current = Minecraft.getInstance();
        if (current.level == null) {
            return;
        }
        ClientReviveStateStore store = ClientReviveStateStore.get();
        ReviveState state = store.snapshot().state();
        long clientTime = current.level.getGameTime();
        long remainingTicks = store.remainingKoTicks(clientTime);
        int timerColor = ClientUiFormat.prolongedTimerColor(remainingTicks);
        int pulse = remainingTicks <= 600L ? 34 : 22;
        ClientHudPrimitives.drawVignette(graphics, width, height, timerColor, pulse);

        int panelWidth = Math.min(620, width - 32);
        int panelHeight = 176;
        int x = (width - panelWidth) / 2;
        int y = Math.max(24, height - panelHeight - 54);
        ClientHudPrimitives.drawPanel(graphics, x, y, panelWidth, panelHeight, timerColor);

        Font font = current.font;
        Component title = state == ReviveState.FULLY_DOWNED
                ? Component.translatable("screen.fugu_revive_me.prolonged_ko.state_fully_downed")
                : Component.translatable("screen.fugu_revive_me.prolonged_ko.state_prolonged");
        int titleColor = state == ReviveState.FULLY_DOWNED ? ClientUiFormat.COLOR_RED : ClientUiFormat.COLOR_AMBER;
        graphics.drawCenteredString(font, title, x + panelWidth / 2, y + 16, titleColor);
        graphics.drawCenteredString(font, Component.literal(ClientUiFormat.formatTicks(remainingTicks)),
                x + panelWidth / 2, y + 34, ClientHudPrimitives.TEXT);

        int barWidth = Math.min(420, panelWidth - 80);
        int barX = x + (panelWidth - barWidth) / 2;
        ClientHudPrimitives.drawProgressBar(graphics, barX, y + 53, barWidth, 7, store.koProgress(clientTime), timerColor);

        Component bossText = store.snapshot().linkedBossUuid().isPresent()
                ? Component.translatable("screen.fugu_revive_me.prolonged_ko.boss_linked")
                : Component.translatable("screen.fugu_revive_me.prolonged_ko.boss_not_linked");
        graphics.drawString(font, bossText, x + 36, y + 76, ClientUiFormat.COLOR_AMBER, false);
        graphics.drawString(font, Component.translatable("screen.fugu_revive_me.prolonged_ko.ally_unavailable"),
                x + 36, y + 92, ClientHudPrimitives.TEXT, false);
        graphics.drawString(font, Component.translatable("screen.fugu_revive_me.prolonged_ko.anchor_unavailable"),
                x + 36, y + 108, ClientHudPrimitives.TEXT, false);
        graphics.drawString(font, Component.translatable("screen.fugu_revive_me.prolonged_ko.hits", store.snapshot().hitsTaken()),
                x + panelWidth - 188, y + 92, ClientHudPrimitives.TEXT, false);
        graphics.drawString(font, Component.translatable("screen.fugu_revive_me.prolonged_ko.issue"),
                x + panelWidth - 278, y + 108, ClientHudPrimitives.TEXT, false);

        buttonWidth = 160;
        buttonHeight = 22;
        buttonX = x + (panelWidth - buttonWidth) / 2;
        buttonY = y + panelHeight - 38;
        boolean hovered = isInsideButton(mouseX, mouseY);
        int border = hovered ? ClientUiFormat.COLOR_RED : timerColor;
        graphics.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, 0xCC0A0D14);
        graphics.hLine(buttonX, buttonX + buttonWidth, buttonY, border);
        graphics.hLine(buttonX, buttonX + buttonWidth, buttonY + buttonHeight, border);
        graphics.vLine(buttonX, buttonY, buttonY + buttonHeight, border);
        graphics.vLine(buttonX + buttonWidth, buttonY, buttonY + buttonHeight, border);
        graphics.drawCenteredString(font, Component.translatable("screen.fugu_revive_me.prolonged_ko.release"),
                buttonX + buttonWidth / 2, buttonY + 7, ClientHudPrimitives.TEXT);

        if (clientTime < feedbackUntilGameTime) {
            graphics.drawCenteredString(font, Component.translatable("screen.fugu_revive_me.prolonged_ko.sent"),
                    x + panelWidth / 2, buttonY - 14, ClientHudPrimitives.CYAN);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isInsideButton(mouseX, mouseY)) {
            FuguNetwork.channel().sendToServer(ServerboundReleaseSpirit.INSTANCE);
            if (minecraft != null && minecraft.level != null) {
                feedbackUntilGameTime = minecraft.level.getGameTime() + 40L;
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && minecraft != null && minecraft.player != null) {
            minecraft.player.turn(dragX * 0.45D, dragY * 0.45D);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft != null && minecraft.options.keyChat.matches(keyCode, scanCode)) {
            minecraft.setScreen(new ChatScreen(""));
            return true;
        }
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            if (minecraft.level != null) {
                feedbackUntilGameTime = minecraft.level.getGameTime() + 40L;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean isInsideButton(double mouseX, double mouseY) {
        return mouseX >= buttonX && mouseX <= buttonX + buttonWidth
                && mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
    }
}
