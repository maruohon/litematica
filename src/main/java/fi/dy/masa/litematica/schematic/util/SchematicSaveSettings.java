package fi.dy.masa.litematica.schematic.util;

import malilib.util.data.BooleanStorageWithDefault;
import malilib.util.data.SimpleBooleanStorageWithDefault;

public class SchematicSaveSettings
{
    public final BooleanStorageWithDefault saveBlocks               = new SimpleBooleanStorageWithDefault(true);
    public final BooleanStorageWithDefault saveBlockEntities        = new SimpleBooleanStorageWithDefault(true);
    public final BooleanStorageWithDefault saveEntities             = new SimpleBooleanStorageWithDefault(true);
    public final BooleanStorageWithDefault saveBlockTicks           = new SimpleBooleanStorageWithDefault(true);

    public final BooleanStorageWithDefault exposedBlocksOnly        = new SimpleBooleanStorageWithDefault(false);
    public final BooleanStorageWithDefault saveFromNormalWorld      = new SimpleBooleanStorageWithDefault(true);
    public final BooleanStorageWithDefault saveFromSchematicWorld   = new SimpleBooleanStorageWithDefault(false);
}
