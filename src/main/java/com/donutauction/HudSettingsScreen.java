package com.donutauction;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class HudSettingsScreen extends Screen {
    private final Screen parent;

    public HudSettingsScreen(Screen parent) {
        super(Component.literal("HUD layout"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        AuctionState.previewHud = true;
        int cx = width / 2;
        int y = height - 110;

        addRenderableWidget(new Setting(cx - 100, y, 200, "Left <-> Right", 0, 1,
            AuctionState.hudX, v -> AuctionState.hudX = (float) (double) v));
        addRenderableWidget(new Setting(cx - 100, y + 24, 200, "Up <-> Down", 0, 1,
            AuctionState.hudY, v -> AuctionState.hudY = (float) (double) v));
        addRenderableWidget(new Setting(cx - 100, y + 48, 200, "Size", 0.5, 4.0,
            AuctionState.hudScale, v -> AuctionState.hudScale = (float) (double) v));

        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> {
            AuctionState.hudX = 0.01f; AuctionState.hudY = 0.01f; AuctionState.hudScale = 1.0f;
            rebuildWidgets();
        }).bounds(cx - 100, y + 74, 98, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), b ->
            Minecraft.getInstance().setScreen(parent))
            .bounds(cx + 2, y + 74, 98, 20).build());
    }

    @Override
    public void removed() {
        AuctionState.previewHud = false;
        AuctionState.save();
    }

    // keep the HUD preview crisp (no blurred background)
    public void renderBackground(GuiGraphics g, int mx, int my, float delta) { }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        g.drawCenteredString(font, "Move the sliders - the preview updates live",
            width / 2, height - 124, 0xFFFFFFFF);
    }

    private static class Setting extends AbstractSliderButton {
        private final String label;
        private final double min, max;
        private final Consumer<Double> apply;

        Setting(int x, int y, int w, String label, double min, double max, double cur, Consumer<Double> apply) {
            super(x, y, w, 20, Component.empty(), (cur - min) / (max - min));
            this.label = label; this.min = min; this.max = max; this.apply = apply;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            if (label == null) return;
            setMessage(Component.literal(label + ": " + String.format("%.2f", min + value * (max - min))));
        }

        @Override
        protected void applyValue() {
            apply.accept(min + value * (max - min));
        }
    }
}
