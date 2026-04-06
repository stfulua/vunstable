# vUnstable by vProLabs

**The Ultimate Orbital Strike Cannon plugin for Paper/Folia 1.21.x**, 2000 TNT with intelligent auto-optimization, synchronized explosions, and zero server freeze.

> Report bugs on [Discord](https://discord.gg/SNzUYWbc5Q )

---

## ✨ What's New in v1.2.0

### 🚀 Folia Support
- **Full Folia Compatibility**: Complete support for Folia's regionized threading model
- **Cross-Platform Scheduler**: Unified `TaskScheduler` abstraction works on both Bukkit and Folia
- **Ground-Based Sync**: TNT waits for ALL entities to land before synchronized explosion (both platforms)
- **4-Second Failsafe**: Global timeout from spawn ensures explosions happen even if TNT glitches

### 📋 Nuke Queue System
- **Fire Multiple Nukes**: Queue up to 3 nukes while one is active
- **FIFO Processing**: First-in-first-out queue for fair execution
- **Silent Operation**: No player chat spam, console-only logging
- **Configurable**: Adjust `max-concurrent` and `queue-size` in config

### 🔄 Modrinth Auto-Updater
- **Automatic Checks**: Scans for updates every 2 hours
- **Admin Notifications**: Online admins get notified with clickable download links
- **Console Alerts**: Server console shows update availability
- **Manual Check**: `/vu update` command for on-demand checks

### 💥 Improved Synchronized Explosions
- **Smart Ground Detection**: All TNT freeze on ground contact
- **Instant Detonation**: When last TNT lands, all explode simultaneously
- **Summary Messages**: Single clean log showing landed vs failsafe counts
- **No Spam**: Eliminated per-TNT debug output

---

## 🎯 The Rods

**Nuke Rod** (Orbital Strike Cannon)
- **2000 Real TNT Entities** via NMS (bypasses Paper spawn limits)
- **10 Concentric Rings** (radius 5→50 blocks)
- **100x100 Block Destruction Area**
- **5-Tick Deployment** (400 TNT/tick with optimization)
- **Ground-Based Sync**: All TNT explode together when last one lands
- **4-Second Failsafe**: Global timeout ensures explosion even on glitches
- **Zero Block Drops** (configurable)
- **Queue System**: Fire multiple nukes in sequence

**Stab Rod** (Vertical Shaft Borer)
- **INSTANT Mode**: Teleports to target depth (no falling)
- **FALL Mode**: Traditional falling TNT from sky (configurable)
- **Configurable Depth**: Default 100 blocks (sky to bedrock)
- **0.5 Second Fuse**: Near-instant explosion (10 ticks)
- **Visual Effects**: Enderman teleport particles (INSTANT mode)

---

## 🚀 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/vu give nuke [player]` | Give nuke rod | `vunstable.use` (self), `vunstable.give.others` (others) |
| `/vu give stab [player]` | Give stab rod | `vunstable.use` (self), `vunstable.give.others` (others) |
| `/vu update` | Check for updates on Modrinth | `vunstable.admin` |
| `/vu status` | Check optimization status | `vunstable.admin` |
| `/vu reload` | Reload configuration | `vunstable.admin` |

*Alias: `/vunstable` = `/vu`*

---

## ⚙️ Configuration

**`config.yml`:**
```yaml
# Auto-optimize spigot.yml max-tnt-per-tick for Nuke Rod
auto-optimize-spigot: true

nuke:
  total-tnt: 2000          # Total TNT to spawn (Folia: auto-reduces to 1000)
  rings: 10                # Number of concentric rings
  min-radius: 5            # Inner ring radius
  max-radius: 50           # Outer ring radius
  spawn-height: 67         # Spawn height above target
  velocity-y: -3.0         # Downward velocity
  spawn-rate-per-tick: 200 # Spawn rate (Folia auto-reduces to 50)
  sync-explosions: true    # Enable synchronized timing
  base-delay-ticks: 20     # Buffer before detonation
  max-concurrent: 1        # Max simultaneous nukes
  queue-size: 3            # Max queued nukes
  
  # Rod customization
  rod-name: "Nuke"
  rod-enchanted: true

stab:
  depth: 100               # Digging depth
  velocity: -10.0          # Fall velocity (FALL mode)
  fuse-ticks: 10           # 0.5 seconds
  spawn-mode: INSTANT      # INSTANT or FALL
  teleport-effect: true    # Visual effect for INSTANT mode
  
  # Rod customization
  rod-name: "Stab"
  rod-enchanted: true

performance:
  async-spawning: true
  particles-enabled: false
  max-tnt-per-tick: 200

# Block drops control
remove-block-drops: true   # Prevent item lag from explosions
```

**Auto-Optimization:**
- Plugin attempts to set `max-tnt-per-tick: 5000` in `spigot.yml`
- Required for 5-tick nuke deployment (400 TNT/tick)
- Falls back to slower rates if file is read-only
- Console notifies admins on join if manual optimization needed

---

## 🔒 Permissions

- `vunstable.use` — Get rods for yourself (default: op)
- `vunstable.give.others` — Give rods to other players (default: op)  
- `vunstable.admin` — Update checks, status, debug, reload (default: op)

---

## 🛠️ Technical Features

- **Zero Dependencies**: Runs standalone
- **NMS MethodHandles**: Faster than Bukkit reflection
- **Async Spawning**: Queue-based engine (no freeze)
- **Cross-Platform**: Works on Paper, Spigot, and Folia
- **Region-Aware**: Proper thread scheduling for Folia compatibility
- **Console-only Logging**: No chat spam
- **Rate Limiting**: 1 nuke at a time (configurable), command cooldowns
- **Inventory Management**: Drops at feet if full

---

## ✅ Supported Platforms

| Platform | Version | Status |
|----------|---------|--------|
| Paper | 1.21.x | ✅ Full Support |
| Spigot | 1.21.x | ✅ Full Support |
| Folia | 1.21.x | ✅ Full Support |
| Bukkit | 1.21.x | ✅ Full Support |

---

## 🌐 Links

- 🌐 Website: https://vprolabs.xyz 
- ☕ Support: https://ko-fi.com/v4bi 
- 💬 Discord: https://discord.gg/SNzUYWbc5Q 
- 📦 Modrinth: https://modrinth.com/project/dhEhlFIx
- 📂 GitHub: https://github.com/vprolabs/vunstable

---

**Version**: 1.2.0  
**Made with 🔥 by vProLabs Team**
