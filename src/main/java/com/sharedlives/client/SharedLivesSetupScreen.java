package com.sharedlives.client;

import com.sharedlives.network.SetupResponsePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public class SharedLivesSetupScreen extends Screen {

    private boolean modEnabled = true;
    private int lives = 5;

    private Button toggleButton;
    private Button minusButton;
    private Button plusButton;

    public SharedLivesSetupScreen() {
        super(Component.literal("Shared Lives — First Time Setup"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        toggleButton = Button.builder(toggleLabel(), btn -> {
            modEnabled = !modEnabled;
            btn.setMessage(toggleLabel());
            minusButton.active = modEnabled;
            plusButton.active = modEnabled;
        }).bounds(cx - 75, cy - 50, 150, 20).build();

        // [-]  <number>  [+]
        // Gap between the buttons is cx-30 to cx+30 (60 px) — the number renders there
        minusButton = Button.builder(Component.literal(" - "), btn -> {
            if (lives > 1) lives--;
        }).bounds(cx - 60, cy, 30, 20).build();

        plusButton = Button.builder(Component.literal(" + "), btn -> {
            if (lives < 100) lives++;
        }).bounds(cx + 30, cy, 30, 20).build();

        Button confirmButton = Button.builder(
                Component.literal("Confirm").withStyle(ChatFormatting.GREEN),
                btn -> {
                    PacketDistributor.sendToServer(new SetupResponsePayload(modEnabled, lives));
                    this.onClose();
                }
        ).bounds(cx - 50, cy + 40, 100, 20).build();

        addRenderableWidget(toggleButton);
        addRenderableWidget(minusButton);
        addRenderableWidget(plusButton);
        addRenderableWidget(confirmButton);
    }

    private Component toggleLabel() {
        return modEnabled
                ? Component.literal("Shared Lives: ON").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                : Component.literal("Shared Lives: OFF").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g, mouseX, mouseY, delta);

        int cx = this.width / 2;
        int cy = this.height / 2;

        g.drawCenteredString(font,
                Component.literal("♥  Shared Lives Setup  ♥").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                cx, cy - 90, 0xFFFFFF);

        g.drawCenteredString(font,
                Component.literal("Configure the shared life pool for this world.").withStyle(ChatFormatting.GRAY),
                cx, cy - 70, 0xFFFFFF);

        if (modEnabled) {
            g.drawCenteredString(font,
                    Component.literal("Starting lives:").withStyle(ChatFormatting.WHITE),
                    cx, cy - 16, 0xFFFFFF);
        }

        // Draw buttons first
        super.render(g, mouseX, mouseY, delta);

        // Draw the number AFTER the buttons so it appears on top, centred in the gap [-] N [+]
        if (modEnabled) {
            g.drawCenteredString(font,
                    Component.literal(String.valueOf(lives)).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD),
                    cx, cy + 6, 0xFFFFFF);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
