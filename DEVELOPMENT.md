# blert/plugin

This repository contains the RuneLite plugin which collects data from the game
client and sends it to the Blert API.

# Setup

Clone the repository using the following command, or start a new project from
version control in your IDE (e.g. IntelliJ IDEA), using the URL below as the
repository location.

```shell
# Core developers: prefer SSH over HTTPS.
git clone https://github.com/blert-io/plugin.git
```

If building from an IDE, you must have Git installed directly on your system,
not through your IDE, as it is used within the build. Install it from either
[the Git website](https://git-scm.com/downloads) or through your system's
package manager.

## Development RuneLite Client

Follow the instructions in the
[RuneLite Plugin Developer Guide](https://github.com/runelite/plugin-hub/blob/master/README.md)
to set up a development environment. You will use `src/test/java/io/blert/BlertPluginTest.java`
as the entry point to run a development RuneLite client with the Blert plugin
enabled.
