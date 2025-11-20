# 📌 Description

QuickMark is a Minecraft mod that adds a marker system from Fortnite

# 📥 Installation

## For Players

1. Install Fabric `0.129.0+1.21.7` or above with Fabric API `0.16.14` or above
2. Download Quickmark from Modrinth
3. Put the file into your `mods` folder

## For Servers

* **Fabric Server**: add the mod to the `mods` folder
* **Spigot/Paper**: install the QuickMark plugin into the `plugins` folder

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

When you receive an invite, press `Y` (default) to accept.

## Team HUD 

When you're in a team, a HUD appears showing the health of all teammates.


<img height="75%" width="75%" src="assets/invitation.gif">

# 🛠️ For Server Administrators

QuickMark uses two communication methods:

1. **Via Plugin** (recommended) — best performance
2. **Via Chat** (fallback) — messages start with `quickmark://`

**Important:** Messages beginning with `quickmark://` are internal mod messages. Do not filter or remove them when no server plugin is installed — it will break the mod's functionality.

## Known Issues

* The marker icon slightly shifts around the center of the screen (about 1 cm) depending on the camera direction.
  When aiming left — the icon moves right, aiming right — it moves left, aiming up — it moves down, aiming down — it moves up.
  When aiming at the center — the icon stays centered.

## Plans

* GUI for inviting and managing players.
* Some overlays from Fortnite (similar to the invitation overlay)
* Add a distance check to set the marker, because now if I want to take a block with the mouse wheel, the marker is set on this block

# ⭐ Inspiration

The mod is inspired by Fortnite’s marker system, providing an intuitive way to coordinate, call out points of interest, and warn teammates — making cooperative gameplay cleaner and more efficient.

---

### 🚀 **Pull Requests are welcome!** 🙌