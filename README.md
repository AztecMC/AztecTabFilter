# AztecTabCompleter
Spigot 1.13 plugin that filters Command List Packet (initial tab complete / command suggestions) by whitelist.

This is intended to be used spigot setting `tab-complete: 0` which enables all suggestions.

Currently in 1.13, no `tab-complete` setting seems to stop commands from being entered after typing `/<tab>`, so this plugin exists to enforce filtering suggestions.
