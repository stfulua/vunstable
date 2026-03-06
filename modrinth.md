# vUnstable by vProLabs

The Ultimate Orbital Strike Cannon plugin for Paper 1.21.x - 2000 Real TNT with zero lag.

&gt; If you find any bugs, report them on our [discord](https://discord.gg/SNzUYWbc5Q)!

## ✨ Full Features

**Orbital Nuke Rod**
- 2000 Real TNT Entities via NMS (bypasses Paper limits)
- 10 Concentric Rings (radius 5→50 blocks)
- 100x100 Block Destruction Area
- 2 Tick Delay Between Rings (ultra-fast spawn)
- Velocity -3.0 (fast 3-second fall)
- Instant Detonation (2s fuse after landing)
- Zero Block Drops (no item lag)

**Bedrock Stabber Rod**
- 100 Block Deep Shaft (sky to bedrock)
- Always Spawns From Sky (Y = surface + 100)
- Works Underground (still drops from sky)
- High Velocity Fall (-10 Y velocity)
- Instant Vertical Destruction

**Performance**
- NMS Reflection (bypasses all spawn limits)
- No external dependencies
- Console-only logging (no chat spam)
- Optimized for low-end hardware

---

## 🆕 v1.1.1 - Quality of Life Update

### Automatic Updates
- **Update Notifier**: Checks Modrinth automatically for new versions
- **Admin Alerts**: Notifies ops/admins on join when update is available
- **Manual Check**: `/vu update` command to force check

### Block Drops Control
- **New Config**: `remove-block-drops: true` (default)
  - `true`: TNT explosions destroy blocks but drop no items (lag-free)
  - `false`: Vanilla block drops enabled
- Fixes edge cases where drops weren't being removed properly

---

## 🚀 Commands

| Command | Aliases | Description |
|---------|---------|-------------|
| `/vunstable get nuke` | `/vu get nuke` | Obtain the orbital nuke rod |
| `/vunstable get stab` | `/vu get stab` | Obtain the bedrock stabber |
| `/vunstable status` | `/vu status` | Check optimization status |
| `/vunstable update` | `/vu update` | Check for plugin updates |

---

## 🔒 Permissions

- `vunstable.use` - Access to /vu command (default: op)
- `vunstable.admin` - Access to status and update commands (default: op)

---

## 📝 Configuration

No configuration files needed. Plugin works out of the box with optimized defaults.

All actions are logged to console for moderation purposes.

---

## 🌐 Links

- Website: https://vprolabs.xyz
- Support: https://ko-fi.com/v4bi
- Discord: https://discord.gg/SNzUYWbc5Q

---

Made with ❤️ by vProLabs Team
