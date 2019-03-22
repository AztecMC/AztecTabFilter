# AztecTabCompleter
Spigot 1.13 plugin that filters Command List Packet (initial tab complete / command suggestions) by whitelist.

Currently in 1.13, no `tab-complete` setting seems to stop all commands from being suggested after typing `/<tab>`, so this plugin exists to enforce filtering suggestions.

*Note:* Commands can only be removed from suggestions by this plugin, if the server does not already suggest it without the plugin, this will not add the suggestion.

## Getting the plugin

### Downloading the Plugin pre-built
You can find released JARs (that can be added to your plugins folder) on the [Releases tab](https://github.com/crashdemons/AztecTabCompleter/releases) of this project.

If you want something a bit more shiny, you can check out our [development builds](https://ci.meme.tips/job/AztecTabCompleter/) available through Jenkins CI.

### Building the Project yourself
We've recently moved to using Maven! If you used build.xml or a Netbeans Project before, you may need to import the project again as a maven project / from existing POM.

[This document](https://github.com/crashdemons/Notes/blob/master/Importing_Maven_Projects.md) may help you import the project in your IDE.

## Using the Plugin
This is intended to be used spigot setting `tab-complete: 0` which enables all suggestions and tab-completes.

### Commands
A couple administrative commands exist to ease use of the plugin

| Command  | Description |
| ------------- | ------------- |
| /aztabreload | Reloads configuration settings from your config file |
| /aztabdump | Toggles whether console-logging of command-suggestion filtering is enabled. |

### Permissions
Using permissions you can define access to different groups of suggestions, bypassing filtering, or accessing administrative commands.

| Permission  | Default Value | Description |
| ------------- | ------------- |  ------------- |
| aztectabcompleter.*  | True for Ops | Inherit all permissions except group behavior (may differ by permissions plugin)  |
| aztectabcompleter.suggest | True | Allows the user to receive suggestions. Without this permission, suggestions are blocked. |
| aztectabcompleter.group.*group-name-here* | Undefined | Enables filtering for this user which is defined by this group-name in the configuration file |
| aztectabcompleter.bypass | True for Ops | Allows the user to bypass filtering, receiving all normal command-suggestions, unmodified. |
| aztectabcompleter.reload | True for Ops | Allows the user to access the /aztabreload command |
| aztectabcompleter.dump | True for Ops | Allows the user to access the /aztabdump command |

### Configuration
The plugin configuration allows you to define whitelist/blacklist filtering of command-suggestions for everyone, or for specific groups, as well as the ability to tweak filtering order and behavior.

| Configuration Option | Default Value | Description |
| ------------- | ------------- |  ------------- |
| visible-commands |  | A list of commands that are allowed to be displayed for everyone |
| invisible-commands |  | A list of command suggestions that are blocked for everyone |
| groups |  | A set of group configurations that have their own allow/block lists with the above options |
| filter-order | [blacklist,group-blacklists,whitelist,group-whitelists] | A list of types of filters and in which order they should be applied. Can have values: `whitelist`, `blacklist`, `group-whitelists`, `group-blacklists` |
| filter-default | DENY_FINAL | The default action that should occur when no filters match the command. Can have values `DENY_FINAL` or `ALLOW_FINAL`. This defaults to `DENY_FINAL` (blocking the suggestion) if it is not understood. | 
| `kick-early-joins` | true | Whether players joining at server startup before the plugin is fully enabled should be kicked (true/false). *This option is experimental and may be removed if it is not necessary.* |
| `kick-message` | Please wait a moment for the server to load! | The message players see when kicked by the above setting. |
