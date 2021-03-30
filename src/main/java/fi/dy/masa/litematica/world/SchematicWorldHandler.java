package fi.dy.masa.litematica.world;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.GameType;
import net.minecraft.world.DimensionType;
import net.minecraft.client.gui.screen.BiomeGeneratorTypeScreens;
import net.minecraft.world.WorldSettings;
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
        WorldSettings info = new WorldSettings(0, GameType.CREATIVE, false, false, BiomeGeneratorTypeScreens.FLAT.getDefaultOptions());
        return new WorldSchematic(null, info, DimensionType.THE_END, Minecraft.getInstance()::getProfiler);
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
