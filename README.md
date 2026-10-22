# Donut Auction (Fabric, client-side, MC 1.21.11)

Build: install JDK 21, then run `gradle build` (or `./gradlew build` if you add the wrapper).
Jar: build/libs/donut-auction-1.0.0.jar -> put in .minecraft/mods with Fabric Loader + Fabric API.

Use: press J in-game.
- Pick item name / "Use held item" (shows the item icon on the HUD)
- Presets 30s/1m/2m/5m/10m or custom (90, 1m30s, 2h)
- Winner mode: highest single payment, or total paid
- START. People /pay you; HUD shows the leader. When the timer hits 0 the winner is shown.

Bids are read from chat lines like "Name paid you $1,500" (supports K/M/B).
If DonutSMP words it differently, edit the PAY regex in AuctionState.java.
