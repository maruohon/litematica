package fi.dy.masa.litematica.world;

import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.Difficulty;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.render.LitematicaRenderer;

public class SchematicWorldHandler
{
    @Nullable private static WorldSchematic world;

    @Nullable
    public static WorldSchematic getSchematicWorld()
    {
        if (world == null)
        {
            world = createSchematicWorld();
        }

        return world;
    }

    @Nullable
    public static WorldSchematic createSchematicWorld()
    {
        if (MinecraftClient.getInstance().world == null)
        {
            return null;
        }

        ClientWorld.Properties levelInfo = new ClientWorld.Properties(Difficulty.PEACEFUL, false, true);
        RegistryEntryLookup.RegistryLookup lookup = BuiltinRegistries.createWrapperLookup().createRegistryLookup();
        RegistryEntry<DimensionType> entry = lookup.getOrThrow(RegistryKeys.DIMENSION_TYPE).getOrThrow(DimensionTypes.OVERWORLD);
        return new WorldSchematic(levelInfo, entry, MinecraftClient.getInstance()::getProfiler);
    }

    public static void recreateSchematicWorld(boolean remove)
    {
        if (remove)
        {
            Litematica.debugLog("Removing the schematic world...");
            world = null;
        }
        else
        {
            Litematica.debugLog("(Re-)creating the schematic world...");
            // Note: The dimension used here must have no skylight, because the custom Chunks don't have those arrays
            world = createSchematicWorld();
            Litematica.debugLog("Schematic world (re-)created: {}", world);
        }

        LitematicaRenderer.getInstance().onSchematicWorldChanged(world);
    }
}
