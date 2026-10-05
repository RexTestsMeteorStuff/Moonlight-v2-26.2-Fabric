# Moon Client

A Meteor Client addon for **Minecraft 26.2 (Fabric)** with a starry-night, moon-themed glass menu.
It adds a `moon-gui` module (category **Moon**). Bind it to a key and it opens the Moon menu, which lists
every Meteor module by category with toggle switches and a settings gear.

## Install the jar
1. Install Fabric Loader for 26.2 and Java 25.
2. Put `fabric-api` (26.2), `meteor-client` (26.2) and `moon-client-x.y.z.jar` in your `mods` folder.
3. In game, bind the **moon-gui** module (Meteor GUI, or `.bind moon-gui`).
4. For the frosted-glass blur: Options > Video Settings > *Menu Background Blurriness*.

## Get the .jar from GitHub
1. Create a GitHub repo and push this folder to it.
2. Open the **Actions** tab. The *Build Moon Client* workflow runs on every push.
3. Click the run, then download **moon-client** under *Artifacts* (it contains the jar).
4. For a proper Release: `git tag v0.1.0 && git push --tags` and the jar is attached to a GitHub Release.

## If the build fails
Minecraft 26.x renamed a lot of GUI code. All of it is isolated in two places:
- `MoonScreen.java`, bottom section (`fill`, `text`, `textWidth`) and the method signatures
  `extractRenderState` / `mouseClicked` / `mouseScrolled`.
- `MoonClient.openScreen` (try `mc.setScreenAndShow(screen)` if `mc.gui.setScreen` is not found).

Paste the red lines from the Actions log and they are quick to fix.

## License
Meteor Client is GPL-3.0, so this addon should be too. Add a `LICENSE` file with the GPL-3.0 text.
