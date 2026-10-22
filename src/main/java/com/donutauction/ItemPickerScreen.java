package com.donutauction;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ItemPickerScreen extends Screen {
    private static final int COLS = 9, ROWS = 6, CELL = 22, PER = COLS * ROWS;
    private static List<ItemStack> ALL;

    private final Screen parent;
    private EditBox search;
    private String query = "";
    private int page = 0;
    private List<ItemStack> filtered = new ArrayList<>();
    private final List<Button> cells = new ArrayList<>();
    private final List<ItemStack> cellStacks = new ArrayList<>();
    private int gridX, gridY;

    public ItemPickerScreen(Screen parent) {
        super(Component.literal("Pick an item"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (ALL == null) {
            ALL = new ArrayList<>();
            for (Item it : BuiltInRegistries.ITEM) {
                if (it == Items.AIR) continue;
                ALL.add(new ItemStack(it));
            }
        }
        gridX = (width - COLS * CELL) / 2;
        gridY = 52;

        search = new EditBox(font, width / 2 - 100, 24, 200, 20, Component.literal("Search"));
        search.setHint(Component.literal("Search items..."));
        search.setValue(query);
        search.setResponder(s -> { query = s; page = 0; applyFilter(); });
        addRenderableWidget(search);

        int by = gridY + ROWS * CELL + 8;
        addRenderableWidget(Button.builder(Component.literal("< Prev"), b -> changePage(-1))
            .bounds(width / 2 - 110, by, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Next >"), b -> changePage(1))
            .bounds(width / 2 + 50, by, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b ->
            Minecraft.getInstance().setScreen(parent))
            .bounds(width / 2 - 30, by + 26, 60, 20).build());

        cells.clear();
        cellStacks.clear();
        applyFilter();
    }

    private void applyFilter() {
        String q = query.toLowerCase(Locale.ROOT).trim();
        filtered = new ArrayList<>();
        for (ItemStack s : ALL) {
            if (q.isEmpty() || s.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q))
                filtered.add(s);
        }
        rebuildCells();
    }

    private int maxPage() { return Math.max(0, (filtered.size() - 1) / PER); }

    private void changePage(int d) {
        page = Math.max(0, Math.min(maxPage(), page + d));
        rebuildCells();
    }

    private void rebuildCells() {
        page = Math.max(0, Math.min(maxPage(), page));
        for (Button b : cells) removeWidget(b);
        cells.clear();
        cellStacks.clear();
        int start = page * PER;
        for (int i = 0; i < PER && start + i < filtered.size(); i++) {
            final ItemStack st = filtered.get(start + i);
            int x = gridX + (i % COLS) * CELL;
            int y = gridY + (i / COLS) * CELL;
            Button b = Button.builder(Component.empty(), btn -> pick(st))
                .bounds(x, y, CELL - 2, CELL - 2).build();
            cells.add(b);
            cellStacks.add(st);
            addRenderableWidget(b);
        }
    }

    private void pick(ItemStack st) {
        AuctionState.stack = st.copy();
        AuctionState.itemLabel = st.getHoverName().getString();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        changePage(sy < 0 ? 1 : -1);
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        g.drawCenteredString(font, "Pick an item (scroll or use Prev/Next)", width / 2, 10, 0xFFFFFFFF);

        String hover = null;
        for (int i = 0; i < cells.size(); i++) {
            Button b = cells.get(i);
            g.renderItem(cellStacks.get(i), b.getX() + 1, b.getY() + 1);
            if (mx >= b.getX() && mx < b.getX() + b.getWidth()
                && my >= b.getY() && my < b.getY() + b.getHeight())
                hover = cellStacks.get(i).getHoverName().getString();
        }
        int by = gridY + ROWS * CELL + 8;
        g.drawCenteredString(font, (page + 1) + " / " + (maxPage() + 1), width / 2, by + 6, 0xFFAAAAAA);
        if (hover != null) g.drawCenteredString(font, hover, width / 2, by + 54, 0xFFFFD700);
        if (filtered.isEmpty()) g.drawCenteredString(font, "No items found", width / 2, gridY + 40, 0xFFFF5555);
    }
}
