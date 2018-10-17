# AztecTabCompleter
Spigot 1.13 plugin that filters Command List Packet (initial tab complete / command suggestions) by whitelist.

This is intended to be used spigot setting `tab-complete: 0` which enables all suggestions and tab-completes.

Currently in 1.13, no `tab-complete` setting seems to stop all commands from being suggested after typing `/<tab>`, so this plugin exists to enforce filtering suggestions.

*Note:* Commands can only be removed from suggestions by this plugin, if the server does not already suggest it without the plugin, this will not add the suggestion.
