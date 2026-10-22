package com.donutauction;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AuctionScreen extends Screen {
    private EditBox itemBox, timeBox;
    private Button modeBtn, announceBtn;
    private String status = "";

    public AuctionScreen() { super(Component.literal("Donut Auction")); }

    @Override
    protected void init() {
        int cx = width / 2;

        itemBox = new EditBox(font, cx - 100, 40, 200, 20, Component.literal("Item"));
        itemBox.setMaxLength(48);
        itemBox.setValue(AuctionState.itemLabel);
        addRenderableWidget(itemBox);

        addRenderableWidget(Button.builder(Component.literal("Use held item"), b -> {
            var p = Minecraft.getInstance().player;
            if (p != null && !p.getMainHandItem().isEmpty()) {
                AuctionState.stack = p.getMainHandItem().copy();
                itemBox.setValue(AuctionState.stack.getHoverName().getString());
            } else status = "Hold an item first.";
        }).bounds(cx - 100, 64, 200, 20).build());

        // preset timers
        String[] labels = {"30s", "1m", "2m", "5m", "10m"};
        int[] secs = {30, 60, 120, 300, 600};
        for (int i = 0; i < labels.length; i++) {
            final int s = secs[i];
            final String l = labels[i];
            addRenderableWidget(Button.builder(Component.literal(l), b -> timeBox.setValue(l))
                .bounds(cx - 100 + i * 40, 106, 38, 20).build());
        }

        timeBox = new EditBox(font, cx - 100, 134, 200, 20, Component.literal("Time"));
        timeBox.setHint(Component.literal("Custom: 90, 1m30s, 2h..."));
        timeBox.setValue("1m");
        addRenderableWidget(timeBox);

        modeBtn = Button.builder(Component.empty(), b -> {
            AuctionState.totalMode = !AuctionState.totalMode;
            refresh();
        }).bounds(cx - 100, 162, 200, 20).build();
        addRenderableWidget(modeBtn);

        announceBtn = Button.builder(Component.empty(), b -> {
            AuctionState.announce = !AuctionState.announce;
            refresh();
        }).bounds(cx - 100, 186, 200, 20).build();
        addRenderableWidget(announceBtn);

        addRenderableWidget(Button.builder(Component.literal("START"), b -> {
            long t = AuctionState.parseTime(timeBox.getValue());
            if (t <= 0) { status = "Invalid time."; return; }
            AuctionState.itemLabel = itemBox.getValue();
            AuctionState.start(t);
            onClose();
        }).bounds(cx - 100, 214, 98, 20).build());

        addRenderableWidget(Button.builder(Component.literal("END NOW"), b -> {
            AuctionState.stop(true);
            onClose();
        }).bounds(cx + 2, 214, 98, 20).build());

        refresh();
    }

    private void refresh() {
        modeBtn.setMessage(Component.literal("Winner: " +
            (AuctionState.totalMode ? "Total paid" : "Highest single payment")));
        announceBtn.setMessage(Component.literal("Announce in chat: " +
            (AuctionState.announce ? "ON" : "OFF")));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        g.drawCenteredString(font, "Donut Auction", width / 2, 15, 0xFFFFFFFF);
        g.drawString(font, "Item name", width / 2 - 100, 30, 0xFFAAAAAA);
        g.drawString(font, "Timer presets / custom", width / 2 - 100, 94, 0xFFAAAAAA);
        if (!status.isEmpty())
            g.drawCenteredString(font, status, width / 2, 244, 0xFFFF5555);
    }
}
