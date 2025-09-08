# Player Watch Meteor Addon 

## Development

### Part 1: Prerequisites (Things to Install First)
Before you even open the project, you need the right foundation.
Java Development Kit (JDK): Your fabric.mod.json specifies Java 21. You must have a JDK for this version (or newer) installed.
To check your version: Open a terminal or Command Prompt and type java -version.
To install: I recommend Eclipse Temurin from Adoptium. It's a trusted, free distribution of Java. Download and install the JDK 21 version.
Visual Studio Code: Make sure you have the latest version installed from the official website.
Git: You need this to clone the addon template.

### Part 2: Essential VS Code Extensions
VS Code needs specific extensions to understand a Java/Gradle project.
Open VS Code.
Go to the Extensions tab on the left sidebar (it looks like four squares).
Search for and install the following two extensions:
Extension Pack for Java (by Microsoft)
This is a bundle of essential Java tools, including language support, debugging, and project management. It's non-negotiable.
Gradle for Java (by Microsoft)
This extension allows VS Code to understand and interact with your build.gradle file. It's the key to making everything work.
After installing, it might ask you to reload VS Code. Do it.

### Step 3: Initial Gradle Sync
When you open a folder with a build.gradle file for the first time, the Gradle extension will activate. You may see a notification in the bottom-right corner asking to import the Gradle project. Click "Import" or "Yes".
VS Code will now download all the project dependencies specified in the build.gradle file. You can watch the progress in the TERMINAL panel. This will take a few minutes.

### Step 4: Generate Minecraft Sources (The Most Critical Step)
This is the step that directly solves your "cannot be resolved" error. We need to tell Gradle to generate the human-readable Minecraft source code for VS Code to use.
Click the Gradle icon on the left sidebar (it looks like an elephant or a "G"). This opens the Gradle Projects view.
You will see your project name (PlayerWatchAddon). Expand it.
Expand Tasks.
Expand fabric.
Find the task named genSources.
Click the play button next to genSources to run the task.
This will run a command in the terminal. It can take several minutes to complete as it downloads Minecraft, applies the mappings, and generates the source code JAR. Be patient.
### Step 5: Clean and Reload the Java Workspace
After genSources is done, we need to force the Java extension to re-read everything and notice the new source code.
Open the Command Palette using Ctrl+Shift+P (or Cmd+Shift+P on Mac).
Type Java: Clean Java Language Server Workspace.
Select that command and press Enter.
A notification will pop up in the bottom-right corner asking you to reload and delete the history. Click "Restart and Delete".
VS Code will reload.

### Step 6: To compile
Open a terminal and type ```.\gradlew build``` to create the jar file
It will be located in ```PlayerWatch-Meteor-Addon\build\libs```
Drop it in the mods folder for fabric

- Use this template to add custom modules, commands, HUDs, and other features to Meteor Client.
- To test, run the `Minecraft Client` configuration in your IDE.
  This will start a Minecraft client with the Meteor Client mod and your addon loaded.
- To build, run the gradle `build` task. This will create a JAR file in the `build/libs` folder.
    - Move the JAR file to the `mods` folder of your Minecraft installation, alongside the Meteor Client mod and run the
      game.

### Updating to newer Minecraft versions

To update this template to a newer Minecraft version, follow these steps:

1. Ensure a Meteor Client snapshot is available for the new Minecraft version.
2. Update `gradle.properties`:
    - Set `minecraft_version`, `yarn_mappings` and `loader_version` to the new version.
    - Update any additional dependencies accordingly.
3. Update Loom:
    - Change the `loom_version` in `build.gradle.kts` to the latest version compatible with the new Minecraft version.
4. Update the Gradle wrapper:
    - You can find the latest Gradle version [here](https://gradle.org/releases/).
    - Run the `./gradlew wrapper --gradle-version <version>; ./gradlew wrapper` command to update the wrapper script.
5. Update your source code:
    - Adjust for Minecraft or Yarn mapping changes: method names, imports, mixins, etc.
    - Check for Meteor Client API changes that may affect your addon by comparing against the
      [master branch](https://github.com/MeteorDevelopment/meteor-client/tree/master).
6. Build and test:
    - Run the gradle `build` task.
    - Confirm the build succeeds and your addon works with the new Minecraft version.

### Project structure

```text
.
│── .github
│   ╰── workflows
│       │── dev_build.yml
│       ╰── pull_request.yml
│── gradle
│   ╰── wrapper
│       │── gradle-wrapper.jar
│       ╰── gradle-wrapper.properties
│── src
│   ╰── main
│       │── java
│       │   ╰── com
│       │       ╰── example
│       │           ╰── addon
│       │               │── commands
│       │               │   ╰── CommandExample
│       │               │── hud
│       │               │   ╰── HudExample
│       │               │── modules
│       │               │   ╰── ModuleExample
│       │               ╰── AddonTemplate
│       ╰── resources
│           │── assets
│           │   ╰── template
│           │       ╰── icon.png
│           │── addon-template.mixins.json
│           ╰── fabric.mod.json
│── .editorconfig
│── .gitignore
│── build.gradle.kts
│── gradle.properties
│── gradlew
│── gradlew.bat
│── LICENSE
│── README.md
╰── settings.gradle.kts
```

This is the default project structure. Each folder/file has a specific purpose.  
Here is a brief explanation of the ones you might need to modify:

- `.github/workflows`: Contains the GitHub Actions configuration files.
- `gradle`: Contains the Gradle wrapper files.  
  Edit the `gradle.properties` file to change the version of the Gradle wrapper.
- `src/main/java/com/example/addon`: Contains the main class of the addon.  
  Here you can register your custom commands, modules, and HUDs.  
  Edit the `getPackage` method to reflect the package of your addon.
- `src/main/resources`: Contains the resources of the addon.
    - `assets`: Contains the assets of the addon.  
      You can add your own assets here, separated in subfolders.
        - `template`: Contains the assets of the template.  
          You can replace the `icon.png` file with your own addon icon.  
          Also, rename this folder to reflect the name of your addon.
    - `addon-template.mixins.json`: Contains the Mixin configuration for the addon.  
      You can add your own mixins in the `client` array.
    - `fabric.mod.json`: Contains the metadata of the addon.  
      Edit the various fields to reflect the metadata of your addon.
- `build.gradle.kts`: Contains the Gradle build script.  
  You can manage the dependencies of the addon here.  
  Remember to keep the `fabric-loom` version up-to-date.
- `gradle.properties.kts`: Contains the properties of the Gradle build.  
  These will be used by the build script.
- `LICENSE`: Contains the license of the addon.  
  You can edit this file to change the license of your addon.
- `README.md`: Contains the documentation of the addon.  
  You can edit this file to reflect the documentation of your addon, and showcase its features.

## Modules
### Player Watch
Will constantly check for players near you that match players on the list, if it detects one it will instantly leave the server.

### Auto Reconnect
Will run a timer and log you back into the server for any reason, such as if you get kicked, if the server restarts and most importantly when the player watch module detects an admin/snitch and disconnects you. Auto reconnect will join back after a delay so player watch can check if the coast is clear and then auto login restarts the bot. So you can fully leave the setup afk.

### Auto Login
Each time the bot joins it will run these commands. More reliable than the auto login found in meteor rejects.

### ProximityAlert
This module will run commands if anyone enters or exits a specified radius. It will ignore people on the friends list.

### CommandLooper
Simply sends a command then waits for a variable amount of time, then sends the next command, then waits, then sends the next command, eventually looping back towards the first command.

## License

This template is available under the CC0 license. Feel free to use it for your own projects.
