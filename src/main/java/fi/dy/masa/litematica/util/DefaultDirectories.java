package fi.dy.masa.litematica.util;

import java.io.File;
import fi.dy.masa.malilib.util.FileUtils;

public class DefaultDirectories
{
    public static File getDefaultSchematicDirectory()
    {
        return FileUtils.getCanonicalFileIfPossible(new File(FileUtils.getMinecraftDirectory(), "schematics"));
    }
}
