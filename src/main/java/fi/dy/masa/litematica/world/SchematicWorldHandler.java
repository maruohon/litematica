package fi.dy.masa.litematica.world;

import java.util.OptionalLong;
import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.tag.BlockTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.biome.source.DirectBiomeAccessType;
import net.minecraft.world.dimension.DimensionType;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.render.LitematicaRenderer;

public class SchematicWorldHandler
{
    @Nullable private static WorldSchematic world;
    public static final DimensionType DIMENSIONTYPE = DimensionType.create(OptionalLong.of(6000L), false, false, false, false, 1.0,
                                                                           false, false, false, false, false, -64, 384, 384,
                                                                           DirectBiomeAccessType.INSTANCE, BlockTags.INFINIBURN_END.getId(),
                                                                           DimensionType.OVERWORLD_ID, 0.0F);

    @Nullable
    public static WorldSchematic getSchematicWorld()
    {
        return world;
    }

    public static WorldSchematic createSchematicWorld()
    {
        ClientWorld.Properties levelInfo = new ClientWorld.Properties(Difficulty.PEACEFUL, false, true);
        return new WorldSchematic(levelInfo, DIMENSIONTYPE, MinecraftClient.getInstance()::getProfiler);
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
