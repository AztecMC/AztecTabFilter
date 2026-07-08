# Maintenance Changelog

## 2026-07-07

- Pinned the Spigot API dependency to `26.1.1-R0.1-SNAPSHOT`, the newest Spigot API snapshot outside the 45-day window for this run.
- Updated Maven compiler configuration to build with Java release `25`, matching the selected Minecraft API runtime.
- Removed unused stale/HTTP Maven repositories so dependency resolution uses the Spigot repositories and Maven Central.
