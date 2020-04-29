package fi.dy.masa.litematica.world;

import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.Difficulty;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelGeneratorType;
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
        ClientWorld.class_5271 levelInfo = new ClientWorld.class_5271(0L, Difficulty.PEACEFUL, false, LevelGeneratorType.FLAT.getDefaultOptions());
        return new WorldSchematic(null, levelInfo, DimensionType.THE_END, MinecraftClient.getInstance()::getProfiler);
    }

    public static void recreateSchematicWorld(boolean remove)
    {
        if (remove)
        {
            world = null;
        }
        else
        {
            // Note: The dimension used here must have no skylight, because the custom Chunks don't have those arrays
            world = createSchematicWorld();
        }

        LitematicaRenderer.getInstance().onSchematicWorldChanged(world);
    }
}
