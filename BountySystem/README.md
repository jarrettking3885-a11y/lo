# BountySystem

A complete, self-contained Bounty Hunting System for a Spigot/Paper 1.21.x SMP plugin.

## Building

```
mvn clean package
```

This sandbox could not reach Maven Central / the Spigot Nexus / JitPack to
download `spigot-api` and `VaultAPI` (only a short domain allowlist is
available here), so the build could **not** be verified end-to-end in this
environment. The code was written carefully against the standard
Spigot/Paper 1.21 API and double-checked for balanced braces/parens and
consistent method signatures, but please run `mvn clean package` yourself
once you copy this into a normal dev machine with internet access, and fix
up any minor API-version mismatches if your server jar differs slightly.

If you don't use Vault on your SMP, delete
`economy/VaultEconomyProvider.java` and the VaultAPI dependency in `pom.xml`,
and write your own class implementing `EconomyProvider` that calls into your
existing Economy/PlayerData system instead.

## Package layout

```
com.smp.bounty
 ├─ BountyPlugin          main class, wiring everything together
 ├─ config/ConfigManager  typed wrapper around config.yml
 ├─ data/
 │   ├─ PlayerBounty       per-player bounty model (serializable)
 │   └─ BountyDataManager  cache + YAML persistence + decay task
 ├─ economy/
 │   ├─ EconomyProvider        integration interface
 │   ├─ VaultEconomyProvider   used automatically if Vault is present
 │   └─ SimpleEconomyProvider  in-memory fallback
 ├─ listeners/
 │   ├─ PlayerDeathListener      core kill/reward logic
 │   └─ PlayerConnectionListener join/quit lifecycle (save, stop tracking)
 ├─ tracking/
 │   ├─ TrackingManager   schedules per-tracker compass update tasks
 │   └─ TrackingSession   holds the BukkitTask for one tracker
 ├─ commands/
 │   ├─ BountyCommand       /bounty, /bounty top, /bounty top gui
 │   ├─ AdminBountyCommand  /adminbounty set|add
 │   └─ TrackCommand        /track <player>
 ├─ gui/
 │   ├─ LeaderboardGUI          inventory leaderboard (/bounty top gui)
 │   └─ LeaderboardGUIListener  blocks item removal from the GUI
 └─ util/ColorUtil        color code + action-bar helper
```

## Integration points for your larger SMP plugin

- **PlayerData**: `BountyDataManager` is fully UUID-keyed and independent of
  any existing PlayerData system - it can run side-by-side, or you can move
  the `bounty`/`killStreak` fields from `PlayerBounty` into your own
  PlayerData object if you'd rather have a single source of truth.
- **Economy**: implement `EconomyProvider` against your real economy and
  return it from `BountyPlugin#setupEconomy()` instead of
  `SimpleEconomyProvider`.
- **Combat**: all kill/reward logic lives in one place,
  `PlayerDeathListener`. Hook in assist-tracking or combat-tag checks there.

## Design notes / how the spec requirements were met

- **Persistence**: single `playerdata.yml` under the plugin's data folder,
  loaded synchronously once on enable, saved asynchronously (`runTaskTimerAsynchronously`)
  every `storage.autosave_interval_seconds` (default 90s), plus an
  immediate async save on every player quit and a synchronous save on
  `onDisable`.
- **No duplicate rewards**: all bounty/economy mutation for a kill happens
  synchronously inside the single `PlayerDeathEvent` handler call - there's
  no async re-entry or second code path that could double-fire.
- **Tracking**: one `runTaskTimer` (main thread, since it must call
  `Player#setCompassTarget`) per active tracker, default every 40 ticks
  (2s), configurable via `tracking.update_interval_ticks`. No per-tick
  scanning of all players. Falls back to the target's last known location
  (recorded on death/quit) when they're offline, and can optionally pause
  tracking across dimensions via `tracking.allow_nether_tracking`.
- **Performance**: no blocking calls on the main thread. Player-name
  resolution deliberately avoids `Bukkit.getOfflinePlayer(String)` (which can
  trigger a blocking Mojang API lookup for never-cached names) - it checks
  online players first, then the plugin's own UUID-keyed cache.
- **Decay**: optional async repeating task that reduces bounty for players
  inactive longer than `bounty.decay_inactivity_minutes`.
- **GUI leaderboard**: `/bounty top gui` opens a simple player-head
  inventory leaderboard; clicks are cancelled so items can't be removed.

## Commands & permissions

| Command | Permission | Description |
|---|---|---|
| `/bounty` | `bounty.use` (default: true) | Shows your own bounty |
| `/bounty <player>` | `bounty.use` | Shows another player's bounty |
| `/bounty top` | `bounty.use` | Top 10 leaderboard in chat |
| `/bounty top gui` | `bounty.use` | Opens the GUI leaderboard |
| `/track <player>` | `bounty.track` (default: true) | Gives a tracking compass |
| `/adminbounty set <player> <value>` | `bounty.admin` (default: op) | Sets a player's bounty |
| `/adminbounty add <player> <value>` | `bounty.admin` (default: op) | Adds to a player's bounty |

## Config (`config.yml`)

See the inline comments in `src/main/resources/config.yml` - every value
from the spec (`kill_gain`, `kill_streak_bonus`, `reward_multiplier`,
`decay_rate`, `update_interval_ticks`, `allow_nether_tracking`) is present,
plus a few extra knobs (`victim_retain_percent`, decay timing, autosave
interval, GUI toggle).
