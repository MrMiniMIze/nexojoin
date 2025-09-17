# NexoJoin

NexoJoin is a Spigot/Paper plugin for Minecraft 1.21+ that allows players to choose their own join and leave messages from a GUI menu. Server owners can define as many message styles as they like in the configuration file and gate each one behind a permission node.

## Features

- `/joinmessages` (`/messages`) opens an intuitive menu where players can switch between join and leave message selectors.
- Fully configurable join and leave messages with color codes (`&`), `%player%`, and `%displayname%` placeholders.
- Optional permission node per message so you can grant access through your ranks system.
- Player selections are saved in `plugins/NexoJoin/players.yml`.
- `/joinmessages reload` lets administrators reload the configuration without restarting the server.

## How the plugin works

1. Define the join and leave message styles you want to offer inside `config.yml`, optionally attaching a permission string to each option.
2. Grant the appropriate permissions (for example through LuckPerms or another ranks plugin) so that players only see and select the styles they are entitled to use.
3. Players execute `/joinmessages` to open a two-step GUI: the root menu lets them choose between join or leave selectors, and the selection menu displays the permitted styles with previews and lore.
4. When a player picks a style, NexoJoin stores their selection in `players.yml`. On subsequent joins or quits the plugin re-validates the stored selection, automatically clearing it if they lose permission and falling back to an allowed default instead of an unrestricted option.

These safeguards ensure players cannot exploit the system to force messages they have not been granted, even if the configuration changes while they are offline.

## Permissions

- `nexojoin.command` – allows players to open the selector menu. (default: `true`)
- `nexojoin.reload` – reloads the plugin configuration. (default: `op`)
- Any additional permission strings that you add to message definitions will be required for players to use those specific messages.

## Configuration

The default `config.yml` includes example messages. Add new entries under `join-messages` or `leave-messages`, specifying a unique key, display name, message text, material icon, lore, and optional permission. All text fields support the standard legacy ampersand color/style codes (for example `&6` for gold, `&l` for bold) along with the `%player%` and `%displayname%` placeholders.

```yml
join-messages:
  sparkles:
    display-name: "&dSparkly Arrival"
    message: "&d%player% &7appeared in a flash of light!"
    material: "FIREWORK_ROCKET"
    permission: "nexojoin.join.sparkles"
```

Set the defaults via the `defaults.join` and `defaults.leave` values. Players without a saved selection will be assigned the default as long as they have the permission for it.

### Menu layout

The `menu` section controls the GUI layout:

```yml
menu:
  root:
    title: "&9Nexo Messages"
    size: 27              # Must be a multiple of 9 (min 9, max 54)
    items:
      join:
        slot: 11          # Slot indices follow the standard chest grid (0-53)
        material: "LIME_DYE"
        name: "&aJoin Messages"
        lore:
          - "&7Select your join message."
      leave:
        slot: 15
        material: "RED_DYE"
        name: "&cLeave Messages"
        lore:
          - "&7Select your leave message."
  selection:
    size: 54
    option-slots:         # Optional ordering for message buttons
      - 10
      - 11
      - 12
      - 19
      - 20
      - 21
      - 28
      - 29
      - 30
      - 37
      - 38
      - 39
    join:
      title: "&aJoin Messages"
    leave:
      title: "&cLeave Messages"
    back-item:
      slot: 49
      material: "ARROW"
      name: "&cBack"
      lore:
        - "&7Click to return to the main menu."
    empty:
      slot: 22
      material: "BARRIER"
      name: "&cNo messages available"
      lore:
        - "&7You do not have access to any messages."
```

- `root.size` and `selection.size` are clamped to valid multiples of 9 between 9 and 54 slots.
- Every slot index is validated; if a configured slot is outside the menu size it will be skipped and a warning will be written to console.
- `option-slots` lets you dictate the order and placement of message buttons; if you omit it the plugin fills the first available slots automatically.

## Building

The project uses Maven. Run `mvn package` to build `target/NexoJoin-1.0.0-shaded.jar`.
