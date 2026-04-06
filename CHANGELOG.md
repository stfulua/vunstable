# Changelog

All notable changes to the vUnstable plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] - 2026-04-06

### 🎉 Open Source Release

**vUnstable is now Open Source!** 🚀

We're excited to announce that vUnstable is now open source and available for the community to contribute, learn from, and improve. The source code is hosted on GitHub and we welcome pull requests, bug reports, and feature suggestions.

- **Repository**: https://github.com/vprolabs/vunstable
- **License**: vProLabs General License (see LICENSE file)
- **Support**: discord.gg/SNzUYWbc5Q

### 🚀 Added

- **Folia Support** - Full compatibility with Folia's regionized threading model
  - Added `folia-supported: true` to plugin.yml for Folia recognition
  - Added `TaskScheduler` abstraction layer for cross-platform scheduling
  - Implemented `BukkitSchedulerManager` for standard Paper/Spigot servers
  - Implemented `FoliaSchedulerManager` for Folia servers using `GlobalRegionScheduler`, `AsyncScheduler`, `RegionScheduler`, and `Entity.getScheduler()`
  - Automatic Folia detection at runtime

- **Unified Ground-Based Sync** - Both Folia and Bukkit now use ground-based synchronization
  - TNT waits for ALL entities to touch ground, then explodes immediately
  - 4-second global timeout from spawn (not per-TNT) as failsafe
  - Single summary message shows landed vs failsafe counts (no console spam)
  - Concurrent tracking via `ConcurrentHashMap` for thread safety

- **Nuke Queue System** - Fire multiple nukes in sequence
  - Configurable `nuke.max-concurrent` - how many nukes run simultaneously (default: 1)
  - Configurable `nuke.queue-size` - how many nukes can be queued (default: 3)
  - Completely silent - no player messages about queue status
  - Auto-processes queue when active nukes complete
  - Queue is FIFO (First In, First Out)

- **Modrinth Auto-Updater** - Automated update notifications
  - Scans for new versions every 2 hours (144000 ticks)
  - Notifies console when updates are available
  - Notifies online admins with clickable download links
  - Manual update check via `/vu update` command
  - Project ID: `dhEhlFIx`

### 🔧 Changed

- **Streamlined Debug Logging** - SystemInfoLogger now only logs essential crash/debug data
  - Removed: OS details, Java home, full JVM args, uptime, per-world entity counts
  - Kept: Plugin version, Server version, Java version, Memory usage, TPS, Basic world names
  - Reduces log noise while keeping critical debugging info

- **Refactored Game Logic** - All rod implementations now use Folia-compatible scheduling
  - `NukeRod` - Uses region-aware scheduling for TNT spawning, tracking, and explosions
  - `StabRod` - Updated to use TaskScheduler abstraction
  - `EntityListener` - Ground detection and entity operations use entity-specific region threads
  - `AdminNotifier` - Notification delays use scheduler abstraction
  - `UpdateListener` - Update notifications use scheduler abstraction
  - `AdminErrorNotifier` - Error notifications use scheduler abstraction

- **Platform-Specific Optimizations** - Automatic adjustments based on server type
  - Folia: Reduced TNT count (1000 vs 2000) for region performance
  - Folia: Slower spawn rate (50/tick vs 200/tick) for stability
  - Bukkit/Paper: Full performance (2000 TNT, 400/tick with optimization)

### ✅ Tested Environments

- **Folia 1.21.11** - All features working: Stab, Nuke, Queue
- **Paper 1.21.11** - All features working: Stab, Nuke, Queue
- **Bukkit/Spigot** - Full backward compatibility maintained

### 🛡️ Security & Compatibility

- **Thread Safety** - All entity operations now execute on the correct region thread for Folia compatibility
- **Backward Compatibility** - Full support maintained for standard Paper/Spigot servers
- **No Breaking Changes** - Existing configurations and commands work without modification

### 📦 Build System

- Added compiler flags for deprecation warnings (`-Xlint:deprecation`)
- Added Gradle warning mode (`--warning-mode all`)
- Updated to Java 21 for Paper 1.21.x compatibility

---

## [1.1.2] - 2026-03-06

### 🚀 Added

- **AsyncSpawnEngine** - High-performance async TNT spawning with rate limiting
  - Uses MethodHandles for fast NMS reflection
  - Queue-based spawning (200 TNT per tick max)
  - Metadata tagging for Nuke/Stab identification
  - Proper yield setting for block damage
  - Fallback to Bukkit API if NMS fails

- **SpigotOptimizer** - Automatic server optimization
  - Auto-detects spigot.yml configuration
  - Auto-adjusts max-tnt-per-tick for optimal Nuke performance
  - Admin notifications for unoptimized servers

- **AdminNotifier** - Admin optimization alerts
  - Notifies admins on join about server optimization status
  - Shows current vs required max-tnt-per-tick values
  - Provides configuration instructions

- **UpdateChecker** - Automatic update checking
  - Checks Modrinth API for plugin updates
  - Async HTTP requests (non-blocking)
  - 1-hour result caching
  - Graceful error handling

- **UpdateListener** - Update notifications
  - Notifies admins on join about available updates
  - Clickable download links
  - Manual update check command (`/vu update`)

- **AdminErrorNotifier** - Error notifications
  - Notifies admins of recent errors when they join
  - Shows error count and details
  - Discord support link

- **SystemInfoLogger** - Diagnostic logging
  - Generates comprehensive system information log
  - Server, Java, OS, memory, and plugin details
  - Useful for support and debugging

- **ErrorHandler** - Centralized error handling
  - Logs errors with full context
  - Tracks recent errors for admin notification
  - Player and location context

### 🛠️ Commands

- `/vu give <nuke|stab> [player]` - Give destruction rods
- `/vu update` - Check for plugin updates
- `/vu status` - Check optimization status
- `/vu reload` - Reload configuration

### 🎮 Rods

- **Nuke Rod** - The Orbital Strike Cannon
  - Spawns 2000 TNT in 10 rings with synchronized detonation
  - Reliable UUID tracking to prevent frozen TNT
  - Configurable rings, radius, height, and fuse
  - Synchronized explosion mode

- **Stab Rod** - The Bedrock Digger
  - Creates a vertical shaft from surface to depth
  - INSTANT mode: TNT spawns directly at target depth
  - FALL mode: Traditional falling TNT from sky
  - Configurable depth and fuse

### ⚙️ Configuration

- `nuke.rings` - Number of concentric rings (default: 10)
- `nuke.total-tnt` - Total TNT to spawn (default: 2000)
- `nuke.min-radius` - Inner ring radius (default: 5.0)
- `nuke.max-radius` - Outer ring radius (default: 50.0)
- `nuke.spawn-height` - Spawn height above target (default: 100)
- `nuke.velocity-y` - Downward velocity (default: -10.0)
- `nuke.fuse-ticks` - TNT fuse duration (default: 600)
- `nuke.sync-explosions` - Synchronize ring explosions (default: true)
- `nuke.base-delay-ticks` - Base explosion delay (default: 40)
- `stab.depth` - Digging depth (default: 64)
- `stab.fuse-ticks` - Stab TNT fuse (default: 80)
- `stab.spawn-mode` - INSTANT or FALL (default: INSTANT)
- `stab.teleport-effect` - Visual effect for instant mode (default: true)
- `auto-optimize-spigot` - Auto-optimize spigot.yml (default: true)
- `remove-block-drops` - Remove block drops from explosions (default: true)

---

## [1.0.0] - 2026-02-XX

### 🎉 Initial Release

- Initial release of vUnstable plugin
- Basic Nuke and Stab rod functionality
- Configuration system
- Permission system

---

## Support

- **Discord**: https://discord.gg/SNzUYWbc5Q
- **Website**: https://vprolabs.xyz
- **Modrinth**: https://modrinth.com/project/dhEhlFIx
- **Issues**: https://github.com/vprolabs/vunstable/issues

## Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

**Thank you for using vUnstable!** 💥
