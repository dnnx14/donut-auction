package com.donutauction;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Map;

public class AuctionMod implements ClientModInitializer {
    private static KeyMapping openKey;

    @Override
    public void onInitializeClient() {
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
        boolean showResult = !AuctionState.active && AuctionState.lastWinner != null;
        if (!AuctionState.active && !showResult) return;

        Font f = mc.font;
        int x = 6, y = 6, w = 170;
        List<Map.Entry<String, Double>> top = AuctionState.sorted();
        int rows = Math.min(3, top.size());
        int h = 44 + (rows > 0 ? 4 + rows * 11 : 0);

        g.fill(x, y, x + w, y + h, 0x90000000);

        if (!AuctionState.stack.isEmpty()) g.renderItem(AuctionState.stack, x + 4, y + 4);
        String title = AuctionState.itemLabel.isBlank() ? "Auction" : AuctionState.itemLabel;
        g.drawString(f, title, x + 24, y + 8, 0xFFFFD700);

        if (AuctionState.active) {
            long left = Math.max(0, AuctionState.endMs - System.currentTimeMillis()) / 1000;
            String t = left >= 3600
                ? String.format("%d:%02d:%02d", left / 3600, (left % 3600) / 60, left % 60)
                : String.format("%02d:%02d", left / 60, left % 60);
            int col = left <= 10 ? 0xFFFF5555 : 0xFFFFFFFF;
            g.drawString(f, "Time: " + t, x + 6, y + 24, col);
        } else {
            g.drawString(f, "ENDED - WINNER", x + 6, y + 24, 0xFF55FF55);
        }

        if (top.isEmpty()) {
            g.drawString(f, "No bids yet", x + 6, y + 35, 0xFFAAAAAA);
        } else {
            g.drawString(f, "Lead: " + top.get(0).getKey() + " " + AuctionState.money(top.get(0).getValue()),
                x + 6, y + 35, 0xFF55FF55);
            for (int i = 0; i < rows; i++) {
                var e = top.get(i);
                g.drawString(f, (i + 1) + ". " + e.getKey() + " " + AuctionState.money(e.getValue()),
                    x + 6, y + 48 + i * 11, i == 0 ? 0xFFFFFFFF : 0xFFBBBBBB);
            }
        }
    }
}
