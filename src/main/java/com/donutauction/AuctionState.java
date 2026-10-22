package com.donutauction;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.*;

public final class AuctionState {
    // If DonutSMP words payments differently, edit this one line.
    public static final Pattern PAY = Pattern.compile(
        "([\\w.]{3,17}) paid you \\$([\\d,]+(?:\\.\\d+)?)\\s*([kKmMbB])?");

    public static boolean active = false;
    public static long endMs = 0;
    public static String itemLabel = "";
    public static String timeText = "1m";
    public static ItemStack stack = ItemStack.EMPTY;
    public static boolean totalMode = false;
    public static boolean announce = true;
    public static String lastWinner = null;
    public static double lastWinningBid = 0;

    // HUD layout (position is 0..1 across the free screen space)
    public static float hudX = 0.01f;
    public static float hudY = 0.01f;
    public static float hudScale = 1.0f;
    public static boolean previewHud = false;

    public static final Map<String, Double> bids = new HashMap<>();

    public static void start(long seconds) {
        bids.clear();
        lastWinner = null;
        endMs = System.currentTimeMillis() + seconds * 1000L;
        active = true;
        say("AUCTION START: " + (itemLabel.isBlank() ? "item" : itemLabel)
            + " | " + seconds + "s | /pay me your bid! Highest "
            + (totalMode ? "total" : "payment") + " wins.");
    }

    public static void stop(boolean announceResult) {
        if (!active) return;
        active = false;
        List<Map.Entry<String, Double>> top = sorted();
        if (top.isEmpty()) {
            if (announceResult) say("AUCTION ENDED: no bids.");
            return;
        }
        lastWinner = top.get(0).getKey();
        lastWinningBid = top.get(0).getValue();
        if (announceResult) say("AUCTION WON by " + lastWinner + " with " + money(lastWinningBid)
            + " for " + (itemLabel.isBlank() ? "the item" : itemLabel) + "! GG");
    }

    public static void tick() {
        if (active && System.currentTimeMillis() >= endMs) stop(true);
    }

    public static void onChat(String text) {
        if (!active) return;
        Matcher m = PAY.matcher(text);
        if (!m.find()) return;
        String name = m.group(1);
        double amt;
        try { amt = Double.parseDouble(m.group(2).replace(",", "")); }
        catch (NumberFormatException e) { return; }
        String suf = m.group(3);
        if (suf != null) {
            switch (Character.toLowerCase(suf.charAt(0))) {
                case 'k' -> amt *= 1_000;
                case 'm' -> amt *= 1_000_000;
                case 'b' -> amt *= 1_000_000_000;
            }
        }
        String prevLeader = leaderName();
        if (totalMode) bids.merge(name, amt, Double::sum);
        else bids.merge(name, amt, Math::max);

        String newLeader = leaderName();
        if (!name.equals(prevLeader) && name.equals(newLeader)) {
            say("New highest bid: " + name + " with " + money(bids.get(name)) + "!");
        }
    }

    public static List<Map.Entry<String, Double>> sorted() {
        List<Map.Entry<String, Double>> l = new ArrayList<>(bids.entrySet());
        l.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return l;
    }

    public static String leaderName() {
        var s = sorted();
        return s.isEmpty() ? null : s.get(0).getKey();
    }

    public static String money(double v) {
        if (v >= 1e9) return String.format("$%.2fB", v / 1e9);
        if (v >= 1e6) return String.format("$%.2fM", v / 1e6);
        if (v >= 1e3) return String.format("$%.2fK", v / 1e3);
        return String.format("$%.0f", v);
    }

    public static long parseTime(String s) {
        s = s.trim().toLowerCase();
        if (s.isEmpty()) return -1;
        Matcher m = Pattern.compile("(\\d+)\\s*([hms]?)").matcher(s);
        long total = 0; boolean any = false;
        while (m.find()) {
            any = true;
            long n = Long.parseLong(m.group(1));
            switch (m.group(2)) {
                case "h" -> total += n * 3600;
                case "m" -> total += n * 60;
                default -> total += n;
            }
        }
        return any ? total : -1;
    }

    private static void say(String msg) {
        if (!announce) return;
        var p = Minecraft.getInstance().player;
        if (p != null) p.connection.sendChat(msg);
    }

    // ---- saved settings ----
    private static Path cfg() {
        return FabricLoader.getInstance().getConfigDir().resolve("donutauction.properties");
    }

    public static void load() {
        try (var in = Files.newInputStream(cfg())) {
            Properties p = new Properties();
            p.load(in);
            hudX = Float.parseFloat(p.getProperty("x", "0.01"));
            hudY = Float.parseFloat(p.getProperty("y", "0.01"));
            hudScale = Float.parseFloat(p.getProperty("scale", "1.0"));
            announce = Boolean.parseBoolean(p.getProperty("announce", "true"));
            totalMode = Boolean.parseBoolean(p.getProperty("total", "false"));
        } catch (Exception ignored) { }
    }

    public static void save() {
        try (var out = Files.newOutputStream(cfg())) {
            Properties p = new Properties();
            p.setProperty("x", String.valueOf(hudX));
            p.setProperty("y", String.valueOf(hudY));
            p.setProperty("scale", String.valueOf(hudScale));
            p.setProperty("announce", String.valueOf(announce));
            p.setProperty("total", String.valueOf(totalMode));
            p.store(out, "Donut Auction");
        } catch (Exception ignored) { }
    }
}
