package com.donutauction;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Map;

public class AuctionMod implements ClientModInitializer {
    private static KeyMapping openKey;

    @Override
    public void onInitializeClient() {
        AuctionState.load();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.donutauction.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J,
            KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.consumeClick()) mc.setScreen(new AuctionScreen());
            AuctionState.tick();
        });

        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> AuctionState.onChat(msg.getString()));

        HudElementRegistry.addLast(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("donutauction", "hud"),
            (g, tick) -> drawHud(g));
    }

    private static void drawHud(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;

        boolean preview = AuctionState.previewHud;
        boolean showResult = !AuctionState.active && AuctionState.lastWinner != null;
        boolean sample = preview && !AuctionState.active && !showResult;
        if (!AuctionState.active && !showResult && !preview) return;

        Font f = mc.font;
        List<Map.Entry<String, Double>> top = AuctionState.sorted();
        if (sample) top = List.of(Map.entry("Steve", 1_500_000.0), Map.entry("Alex", 900_000.0));
        int rows = Math.min(3, top.size());

        int w = 170;
        int h = 44 + (rows > 0 ? 4 + rows * 11 : 0);
        float s = AuctionState.hudScale;
        float px = Math.max(0, g.guiWidth() - w * s) * AuctionState.hudX;
        float py = Math.max(0, g.guiHeight() - h * s) * AuctionState.hudY;

        g.pose().pushMatrix();
        g.pose().translate(px, py);
        g.pose().scale(s, s);

        g.fill(0, 0, w, h, 0x90000000);

        ItemStack icon = AuctionState.stack;
        if (icon.isEmpty() && sample) icon = new ItemStack(Items.DIAMOND);
        if (!icon.isEmpty()) g.renderItem(icon, 4, 4);

        String title = AuctionState.itemLabel.isBlank() ? (sample ? "Diamond" : "Auction") : AuctionState.itemLabel;
        g.drawString(f, title, 24, 8, 0xFFFFD700);

        if (AuctionState.active) {
            long left = Math.max(0, AuctionState.endMs - System.currentTimeMillis()) / 1000;
            String t = left >= 3600
                ? String.format("%d:%02d:%02d", left / 3600, (left % 3600) / 60, left % 60)
                : String.format("%02d:%02d", left / 60, left % 60);
            g.drawString(f, "Time: " + t, 6, 24, left <= 10 ? 0xFFFF5555 : 0xFFFFFFFF);
        } else if (sample) {
            g.drawString(f, "Time: 01:00", 6, 24, 0xFFFFFFFF);
        } else {
            g.drawString(f, "ENDED - WINNER", 6, 24, 0xFF55FF55);
        }

        if (top.isEmpty()) {
            g.drawString(f, "No bids yet", 6, 35, 0xFFAAAAAA);
        } else {
            g.drawString(f, "Lead: " + top.get(0).getKey() + " " + AuctionState.money(top.get(0).getValue()),
                6, 35, 0xFF55FF55);
            for (int i = 0; i < rows; i++) {
                var e = top.get(i);
                g.drawString(f, (i + 1) + ". " + e.getKey() + " " + AuctionState.money(e.getValue()),
                    6, 48 + i * 11, i == 0 ? 0xFFFFFFFF : 0xFFBBBBBB);
            }
        }

        g.pose().popMatrix();
    }
}
