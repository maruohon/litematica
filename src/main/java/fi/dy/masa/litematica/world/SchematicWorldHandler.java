package fi.dy.masa.litematica.world;

import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.Difficulty;
import net.minecraft.world.dimension.DimensionType;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.render.LitematicaRenderer;

public class SchematicWorldHandler
{
    @Nullable private static WorldSchematic world;

    @Nullable
    public static WorldSchematic getSchematicWorld()
    {
        return world;
    }

    public static WorldSchematic createSchematicWorld()
    {
        ClientWorld.Properties levelInfo = new ClientWorld.Properties(Difficulty.PEACEFUL, false, true);
        DimensionType dimType = MinecraftClient.getInstance().world.getRegistryManager().getDimensionTypes().get(DimensionType.THE_END_REGISTRY_KEY);
        return new WorldSchematic(levelInfo, dimType, MinecraftClient.getInstance()::getProfiler);
    }

    public static void recreateSchematicWorld(boolean remove)
    {
        if (remove)
        {
            if (Configs.Generic.DEBUG_LOGGING.getBooleanValue())
            {
                Litematica.logger.info("Removing the schematic world...");
            }

            world = null;
        }
        else
        {
            if (Configs.Generic.DEBUG_LOGGING.getBooleanValue())
            {
                Litematica.logger.info("(Re-)creating the schematic world...");
            }

            // Note: The dimension used here must have no skylight, because the custom Chunks don't have those arrays
            world = createSchematicWorld();

            if (Configs.Generic.DEBUG_LOGGING.getBooleanValue())
            {
                Litematica.logger.info("Schematic world created: {}", world);
            }
        }

        LitematicaRenderer.getInstance().onSchematicWorldChanged(world);
    }
}
