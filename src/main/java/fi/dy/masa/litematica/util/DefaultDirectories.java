package fi.dy.masa.litematica.util;

import java.nio.file.Path;
import fi.dy.masa.malilib.util.FileUtils;

public class DefaultDirectories
{
    public static Path getDefaultSchematicDirectory()
    {
        return FileUtils.getMinecraftDirectory().resolve("schematics");
    }
}
