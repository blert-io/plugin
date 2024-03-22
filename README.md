# blert

Blert is an Old School Runescape PvM data tracker and analysis system.

This repository contains the Runelite plugin which collects data from the game client and sends it to the Blert API.

# Setup

This repository uses git submodules. To clone the repository and its submodules, use the following command:

```shell
# Core developers: prefer SSH over HTTPS.
git clone --recurse-submodules https://github.com/blert-io/plugin.git
```

When pulling repository updates, ensure that the submodules are up-to-date:

```shell
git pull origin main --recurse-submodules
```

## Runelite

Follow the instructions in
the [Runelite Plugin Developer Guide](https://github.com/runelite/plugin-hub/blob/master/README.md) to set up a
development environment.