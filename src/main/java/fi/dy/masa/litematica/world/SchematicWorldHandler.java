package fi.dy.masa.litematica.world;

import java.util.OptionalLong;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld.ClientWorldInfo;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.DimensionType;
import net.minecraft.world.Difficulty;
import net.minecraft.world.biome.DefaultBiomeMagnifier;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.render.LitematicaRenderer;

public class SchematicWorldHandler
{
    @Nullable private static WorldSchematic world;
    public static final DimensionType DIMENSIONTYPE = new PublicDimensionType(OptionalLong.of(6000L), false, false, false, false,
                                                                              1.0D, false, false, false, false, true, 256,
                                                                              DefaultBiomeMagnifier.INSTANCE,
                                                                              BlockTags.INFINIBURN_END.getId(),
                                                                              DimensionType.THE_END_ID, 0.0F);

    @Nullable
    public static WorldSchematic getSchematicWorld()
    {
        return world;
    }

    public static WorldSchematic createSchematicWorld()
    {
        ClientWorldInfo levelInfo = new ClientWorldInfo(Difficulty.PEACEFUL, false, true);
        return new WorldSchematic(levelInfo, DIMENSIONTYPE, Minecraft.getInstance()::getProfiler);
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
