package fi.dy.masa.litematica.util;

import java.io.File;

public enum FileType
{
    INVALID,
    UNKNOWN,
    JSON,
    LITEMATICA_SCHEMATIC,
    SCHEMATICA_SCHEMATIC,
    SPONGE_SCHEMATIC,
    VANILLA_STRUCTURE;

    public static FileType fromFile(File file)
    {
        if (file.isFile() && file.canRead())
        {
            String name = file.getName();

            if (name.endsWith(".litematic"))
            {
                return LITEMATICA_SCHEMATIC;
            }
            else if (name.endsWith(".schematic"))
            {
                return SCHEMATICA_SCHEMATIC;
            }
            else if (name.endsWith(".nbt"))
            {
                return VANILLA_STRUCTURE;
            }
            else if (name.endsWith(".schem"))
            {
                return SPONGE_SCHEMATIC;
            }
            else if (name.endsWith(".json"))
            {
                return JSON;
            }

            return UNKNOWN;
        }
        else
        {
            return INVALID;
        }
    }
}
