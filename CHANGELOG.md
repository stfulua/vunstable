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

**Thank you for using vUnstable!** 💥
