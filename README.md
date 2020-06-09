[![Curseforge](http://cf.way2muchnoise.eu/full_litematica_downloads.svg)](https://minecraft.curseforge.com/projects/litematica) [![Curseforge](http://cf.way2muchnoise.eu/versions/For%20MC_litematica_all.svg)](https://minecraft.curseforge.com/projects/litematica)

## Litematica
Litematica is a client-side schematic mod for Minecraft, with also lots of extra functionality
especially for creative mode (such as schematic pasting, area cloning, moving, filling, deletion).

It's primarily developed on MC 1.12.2 for LiteLoader. It has also been ported to Rift on MC 1.13.2,
and for Fabric on MC 1.14 and later. There are also Forge versions for 1.12.2, and Forge ports for 1.14.4+
are also planned, but Forge will need to start shipping the Mixin library before those can happen.

Litematica was started as an alternative for [Schematica](https://minecraft.curseforge.com/projects/schematica),
for players who don't want to have Forge installed on their client, and that's why it was developed for Liteloader.

For compiled builds (= downloads), see:
* CurseForge: http://minecraft.curseforge.com/projects/litematica
* For more up-to-date development builds: https://masa.dy.fi/mcmods/client_mods/
* **Note:** Litematica also requires the malilib library mod! But on the other hand Fabric API is not needed.

## Compiling
* Clone the repository
* Open a command prompt/terminal to the repository directory
* On 1.12.x you will first need to run `gradlew setupDecompWorkspace`
  (unless you have already done it once for another project on the same 1.12.x MC version
  and mappings and the same mod loader, Forge or LiteLoader)
* Run `gradlew build` to build the mod
* The built jar file will be inside `build/libs/`

## YourKit
![](https://www.yourkit.com/images/yklogo.png)

We appreciate YourKit for providing the project developers licenses of its profiler to help us improve performance! 

YourKit supports open source projects with innovative and intelligent tools
for monitoring and profiling Java and .NET applications.
YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/),
[YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/) and
[YourKit YouMonitor](https://www.yourkit.com/youmonitor), tools for profiling Java and .NET applications.