package fi.dy.masa.litematica.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import com.google.common.collect.ImmutableList;

public enum FileType
{
    INVALID                 ("",          false),
    UNKNOWN                 ("",          false),
    JSON                    ("json",      false),
    LITEMATICA_SCHEMATIC    ("litematic", true),
    SCHEMATICA_SCHEMATIC    ("schematic", true),
    SPONGE_SCHEMATIC        ("schem",     true),
    VANILLA_STRUCTURE       ("nbt",       true);

    public static final ImmutableList<FileType> VALUES = ImmutableList.copyOf(values());

    private final String fileNameExtension;
    private final boolean isSchematic;

    FileType(String fileNameExtension, boolean isSchematic)
    {
        this.fileNameExtension = fileNameExtension;
        this.isSchematic = isSchematic;
    }

    public boolean isSchematic()
    {
        return this.isSchematic;
    }

    public static FileType fromFileName(Path file)
    {
        if (Files.isRegularFile(file) && Files.isReadable(file))
        {
            String name = file.getFileName().toString().toLowerCase(Locale.ROOT);

            for (FileType type : VALUES)
            {
                if (name.endsWith("." + type.fileNameExtension))
                {
                    return type;
                }
            }

            return UNKNOWN;
        }
        else
        {
            return INVALID;
        }
    }
}
