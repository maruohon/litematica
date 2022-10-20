package fi.dy.masa.litematica.world;

import java.util.OptionalLong;
import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.render.LitematicaRenderer;

public class SchematicWorldHandler
{
    private static final DimensionType END = BuiltinRegistries.DIMENSION_TYPE.entryOf(DimensionTypes.THE_END).value();
    private static final DimensionType DIMENSIONTYPE = new DimensionType(OptionalLong.of(6000L),
                                                                         false, false, false, false,
                                                                         1.0,
                                                                         false, false,
                                                                         -64, 384, 384,
                                                                         BlockTags.INFINIBURN_END,
                                                                         DimensionTypes.OVERWORLD_ID,
                                                                         0.0F,
                                                                         END.monsterSettings());

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
        return new WorldSchematic(levelInfo, BuiltinRegistries.DIMENSION_TYPE.entryOf(DimensionTypes.OVERWORLD), MinecraftClient.getInstance()::getProfiler);
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
