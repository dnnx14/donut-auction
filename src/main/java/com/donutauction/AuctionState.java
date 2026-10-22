package com.donutauction;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.regex.*;

public final class AuctionState {
    // Matches e.g. "Steve paid you $1,500." / "Steve paid you $2.5K" / "$1M"
    // If DonutSMP changes its wording, edit this one line.
    public static final Pattern PAY = Pattern.compile(
        "([\\w.]{3,17}) paid you \\$([\\d,]+(?:\\.\\d+)?)\\s*([kKmMbB])?");

    public static boolean active = false;
    public static long endMs = 0;
    public static String itemLabel = "";
    public static ItemStack stack = ItemStack.EMPTY;
    public static boolean totalMode = false;   // false = highest single payment, true = sum of payments
    public static boolean announce = true;
    public static String lastWinner = null;
    public static double lastWinningBid = 0;

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
}
