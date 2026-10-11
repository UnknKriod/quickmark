# QuickMark

[![Modrinth](https://img.shields.io/modrinth/v/quickmark?label=Download%20on%20Modrinth)](https://modrinth.com/mod/quickmark)
[![GitHub](https://img.shields.io/github/v/release/UnknKriod/quickmark?label=GitHub)](https://github.com/UnknKriod/quickmark)

A Minecraft Fabric mod that brings Fortnite's marker and team HUD system to Minecraft.

## 📥 Installation

### For Players

1.  Install [Fabric Loader](https://fabricmc.net/use/) (0.14.0+) for Minecraft 1.21.7+
2.  Install the [Fabric API](https://modrinth.com/mod/fabric-api) (0.16.14+)
3.  Download the latest version of Quickmark from [Modrinth](https://modrinth.com/mod/quickmark)
4.  Place the `.jar` file into your `mods` folder

### For Servers

*   **Fabric Server:** Simply add the QuickMark mod to the `mods` folder.
*   **Spigot/Paper (Recommended):** For optimal performance and compatibility, install the **[QuickMark Plugin](https://modrinth.com/plugin/quickmark_pl)** into the `plugins` folder. The mod will automatically switch to plugin-based communication.

# Usage

## Marker System

* **Single mouse wheel click** — place a normal marker
* **Double mouse wheel click** — place a danger marker

<img height="75%" width="75%" src="assets/mark.png">

<img height="75%" width="75%" src="assets/danger_mark.png">

<img height="75%" width="75%" src="assets/mark_through_wall.png">

Markers sync automatically across your team and play sound notifications.

## Team Management

**Invite players:**

```
/qm invite <player>
```

**Leave the team:**

```
/qm leave
```

## Accepting Invitations

When you receive an invite, a notification will appear. Press `Y` (default key, customizable in Minecraft's Controls menu) to accept.

## Team HUD

When you're in a team, a HUD appears showing the health of all teammates.


<img height="75%" width="75%" src="assets/invitation.gif">

## GUI

#### Main tab:
<img width="75%" height="75%" alt="main_tab" src="https://github.com/user-attachments/assets/e37a8130-82e7-43d6-aa8b-d72ac8c187a2" />

#### The main tab with teammates:
<img width="75%" height="75%" alt="main_tab_with_teammates" src="https://github.com/user-attachments/assets/2aa8b5ca-598c-4104-acb9-093e945b5e21" />

#### The invitations tab:
<img width="75%" height="75%" alt="invite_tab" src="https://github.com/user-attachments/assets/1dddb5aa-d101-4cea-87c3-e844d1fbd8c4" />

#### The incoming invitations tab:
<img width="75%" height="75%" alt="invites_tab_incoming" src="https://github.com/user-attachments/assets/eb9ce618-b4b6-4d8e-bf97-98dba5a5f2e9" />

#### The outgoing invitations tab:
<img width="75%" height="75%" alt="invites_tab_outgoing" src="https://github.com/user-attachments/assets/5587ebc5-25f3-438d-b190-c0af617cfac2" />

# 🛠️ For Server Administrators

QuickMark uses two communication methods:

1.  **Via Plugin (Recommended)** — Offers the best performance and reliability. [Download the plugin here](https://modrinth.com/plugin/quickmark_pl).
2.  **Via Chat (Fallback)** — If the plugin is not found, the mod uses chat messages prefixed with `quickmark://`.

**⚠️ Crucial for Fallback Mode:** If you are not using the server plugin, ensure that your chat or anti-cheat plugins do **not** filter or remove messages starting with `quickmark://`, as this will break the mod's team and marker synchronization.

## Known Issues

\-

## Plans

* Some overlays from Fortnite (similar to the invitation overlay)

# ⭐ Inspiration

The mod is inspired by Fortnite’s intuitive marker system, designed to enhance team coordination and communication in Minecraft.

---

### 🚀 Contributing

Pull Requests are welcome! Please feel free to submit issues and contributions on our [GitHub repository](https://github.com/UnknKriod/quickmark).

For development setup, clone the repo and run `./gradlew build`.