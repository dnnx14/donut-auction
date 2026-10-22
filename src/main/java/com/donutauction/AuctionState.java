package com.donutauction;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.*;

public final class AuctionState {
    // If DonutSMP words payments differently, edit this one line.
    public static final Pattern PAY = Pattern.compile(
        "(?i)([\\w.]{3,17})\\s+(?:has\\s+)?(?:paid|sent)\\s+you\\s+\\$?\\s*([\\d,]+(?:\\.\\d+)?)\\s*([kmbt])?");
    private static String lastKey = "";
    private static long lastKeyAt = 0;

    public static boolean active = false;
    public static long endedAt = 0;
    public static final long RESULT_SHOW_MS = 30_000;  // winner box auto-hides after 30s
    public static boolean ended = false;   // true after an auction finishes, until the next one starts
    public static long endMs = 0;
    public static String itemLabel = "";
    public static String timeText = "1m";
    public static String minText = "";
    public static String worthText = "";
    public static double minBid = 0;   // bids below this don't count
    public static double worth = 0;    // shown on HUD / announced
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
        minBid = Math.max(0, parseMoney(minText));
        worth = Math.max(0, parseMoney(worthText));
        bids.clear();
        lastWinner = null;
        endMs = System.currentTimeMillis() + seconds * 1000L;
        active = true;
        ended = false;
    }

    public static void stop(boolean announceResult) {
        if (!active) return;
        active = false;
        ended = true;
        endedAt = System.currentTimeMillis();
        List<Map.Entry<String, Double>> top = sorted();
        String what = itemLabel.isBlank() ? "the item" : itemLabel;
        if (top.isEmpty()) {
            local("AUCTION ENDED: no bids for " + what + ".");
            return;
        }
        lastWinner = top.get(0).getKey();
        lastWinningBid = top.get(0).getValue();
        // always shown to you locally, even if chat announcing is off
        local("AUCTION WINNER: " + lastWinner + " with " + money(lastWinningBid) + " for " + what + "!");
    }

    private static void local(String msg) {
        var p = Minecraft.getInstance().player;
        if (p != null) p.displayClientMessage(Component.literal(msg), false);
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        if (active && now >= endMs) stop(true);
        if (!active && ended && now - endedAt > RESULT_SHOW_MS) dismiss();
    }

    /** Hides the result/winner box. */
    public static void dismiss() {
        ended = false;
        lastWinner = null;
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
                case 't' -> amt *= 1_000_000_000_000.0;
            }
        }
        // the same payment can show up in chat AND the action bar: count it once
        long now = System.currentTimeMillis();
        String key = name + "|" + amt;
        if (key.equals(lastKey) && now - lastKeyAt < 1500) return;
        lastKey = key;
        lastKeyAt = now;

        if (totalMode) bids.merge(name, amt, Double::sum);
        else bids.merge(name, amt, Math::max);

    }

    public static List<Map.Entry<String, Double>> sorted() {
        List<Map.Entry<String, Double>> l = new ArrayList<>();
        for (var e : bids.entrySet()) if (e.getValue() >= minBid) l.add(e);
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

    /** "1500", "1,500", "$1.5k", "2m", "1b". Blank = 0, invalid = -1. */
    public static double parseMoney(String s) {
        s = s.trim().replace("$", "").replace(",", "");
        if (s.isEmpty()) return 0;
        Matcher m = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*([kKmMbB])?").matcher(s);
        if (!m.matches()) return -1;
        double v = Double.parseDouble(m.group(1));
        if (m.group(2) != null) {
            switch (Character.toLowerCase(m.group(2).charAt(0))) {
                case 'k' -> v *= 1_000;
                case 'm' -> v *= 1_000_000;
                case 'b' -> v *= 1_000_000_000;
            }
        }
        return v;
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
