package com.donutauction;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AuctionScreen extends Screen {
    private EditBox itemBox, timeBox, minBox, worthBox;
    private Button modeBtn;
    private String status = "";

    public AuctionScreen() { super(Component.literal("Donut Auction")); }

    @Override
    protected void init() {
        int cx = width / 2;

        itemBox = new EditBox(font, cx - 100, 34, 200, 20, Component.literal("Item"));
        itemBox.setMaxLength(48);
        itemBox.setValue(AuctionState.itemLabel);
        itemBox.setResponder(s -> AuctionState.itemLabel = s);
        addRenderableWidget(itemBox);

        addRenderableWidget(Button.builder(Component.literal("Browse all items"), b ->
            Minecraft.getInstance().setScreen(new ItemPickerScreen(this)))
            .bounds(cx - 100, 58, 98, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Use held item"), b -> {
            var p = Minecraft.getInstance().player;
            if (p != null && !p.getMainHandItem().isEmpty()) {
                AuctionState.stack = p.getMainHandItem().copy();
                itemBox.setValue(AuctionState.stack.getHoverName().getString());
            } else status = "Hold an item first.";
        }).bounds(cx + 2, 58, 98, 20).build());

        String[] labels = {"30s", "1m", "2m", "5m", "10m"};
        for (int i = 0; i < labels.length; i++) {
            final String l = labels[i];
            addRenderableWidget(Button.builder(Component.literal(l), b -> timeBox.setValue(l))
                .bounds(cx - 100 + i * 40, 84, 38, 20).build());
        }

        timeBox = new EditBox(font, cx - 100, 108, 200, 20, Component.literal("Time"));
        timeBox.setHint(Component.literal("Custom timer: 90, 1m30s, 2h..."));
        timeBox.setValue(AuctionState.timeText);
        timeBox.setResponder(s -> AuctionState.timeText = s);
        addRenderableWidget(timeBox);

        minBox = new EditBox(font, cx - 100, 132, 98, 20, Component.literal("Min bid"));
        minBox.setHint(Component.literal("Min bid (1k, 5m)"));
        minBox.setValue(AuctionState.minText);
        minBox.setResponder(s -> AuctionState.minText = s);
        addRenderableWidget(minBox);

        worthBox = new EditBox(font, cx + 2, 132, 98, 20, Component.literal("Worth"));
        worthBox.setHint(Component.literal("Item worth (2m)"));
        worthBox.setValue(AuctionState.worthText);
        worthBox.setResponder(s -> AuctionState.worthText = s);
        addRenderableWidget(worthBox);

        modeBtn = Button.builder(Component.empty(), b -> {
            AuctionState.totalMode = !AuctionState.totalMode;
            refresh();
        }).bounds(cx - 100, 156, 200, 20).build();
        addRenderableWidget(modeBtn);

        addRenderableWidget(Button.builder(Component.literal("START"), b -> {
            long t = AuctionState.parseTime(timeBox.getValue());
            if (t <= 0) { status = "Invalid time."; return; }
            if (AuctionState.parseMoney(minBox.getValue()) < 0) { status = "Invalid min bid (try 500k or 1.5m)."; return; }
            if (AuctionState.parseMoney(worthBox.getValue()) < 0) { status = "Invalid worth (try 500k or 1.5m)."; return; }
            AuctionState.itemLabel = itemBox.getValue();
            AuctionState.start(t);
            onClose();
        }).bounds(cx - 100, 180, 98, 20).build());

        addRenderableWidget(Button.builder(Component.literal("END NOW"), b -> {
            AuctionState.stop(true);
            onClose();
        }).bounds(cx + 2, 180, 98, 20).build());

        addRenderableWidget(Button.builder(Component.literal("HUD size & position"), b ->
            Minecraft.getInstance().setScreen(new HudSettingsScreen(this)))
            .bounds(cx - 100, 204, 200, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Hide winner box"), b -> {
            AuctionState.dismiss();
            onClose();
        }).bounds(cx - 100, 228, 200, 20).build());

        refresh();
    }

    private void refresh() {
        modeBtn.setMessage(Component.literal("Winner: " +
            (AuctionState.totalMode ? "Total paid" : "Highest single payment")));

    }

    @Override
    public void removed() { AuctionState.save(); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        g.drawCenteredString(font, "Donut Auction", width / 2, 8, 0xFFFFFFFF);
        g.drawString(font, "Item name", width / 2 - 100, 24, 0xFFAAAAAA);
        if (!AuctionState.stack.isEmpty())
            g.renderItem(AuctionState.stack, width / 2 + 104, 36);
        if (!status.isEmpty())
            g.drawCenteredString(font, status, width / 2, 252, 0xFFFF5555);
    }
}
