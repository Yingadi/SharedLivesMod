# Shared Lives

A cooperative hardcore mod for Minecraft 1.21.1 (NeoForge).

Instead of each death being permanent, all players share a **pool of lives**. Every death costs one life from the pool. When the pool reaches zero, the game is over for everyone — all players are moved to spectator mode. As long as lives remain, players respawn normally.

---

## Features

- Shared life pool across all players on the server
- First-join setup screen for the host/OP: choose starting life count and enable/disable the mod
- Title + sound broadcast on every death so everyone knows what happened
- Live count shown in the tab list footer at all times
- `/sl` command to check lives at any time
- OP commands to add/remove lives mid-game and re-open the setup screen
- Works on singleplayer, LAN, and dedicated servers

---

## Requirements

| | |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.x |

---

## Configuration

The mod creates a server-side config at `saves/<world>/serverconfig/sharedlives-server.toml` (singleplayer) or `world/serverconfig/sharedlives-server.toml` (dedicated server).

| Option | Default | Description |
|---|---|---|
| `enabled` | `true` | Master switch. Set to `false` to play vanilla hardcore. |
| `initialLives` | `5` | Default lives shown in the setup screen (1–1000). |
| `showPlayerHearts` | `true` | Enables the scoreboard heart display on world setup. |

---

## Commands

Both `/sharedlives` and `/sl` work as aliases.

| Command | Permission | Description |
|---|---|---|
| `/sl` | Everyone | Shows current lives remaining (or GAME OVER). |
| `/sl add <n>` | OP level 2 | Adds `n` lives to the pool (use negative to subtract). Broadcasts to all players. |
| `/sl setup` | OP level 2 | Re-opens the setup screen if the world hasn't been configured yet. |

---

## How it works

- **Death:** One life is subtracted from the shared pool. A title and sound play for all players. If lives hit 0, everyone is moved to spectator (game over).
- **Respawn:** Players respawn normally as long as lives remain — hardcore's permanent death is overridden.
- **Game over:** All players are set to spectator. Delete the world or use `/sl add <n>` to restore lives and keep playing.

---

## License

[MIT](LICENSE) — © 2026 Yinga
