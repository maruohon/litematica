package fi.dy.masa.litematica.world;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.DimensionType;
import net.minecraft.world.Difficulty;
import net.minecraft.client.world.ClientWorld.ClientWorldInfo;
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
        ClientWorldInfo levelInfo = new ClientWorldInfo(Difficulty.PEACEFUL, false, true);
        DimensionType dimType = Minecraft.getInstance().world.getRegistryManager().getDimensionTypes().get(DimensionType.THE_END_REGISTRY_KEY);
        return new WorldSchematic(levelInfo, dimType, Minecraft.getInstance()::getProfiler);
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
