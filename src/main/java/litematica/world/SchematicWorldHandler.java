package litematica.world;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import litematica.render.LitematicaRenderer;

public class SchematicWorldHandler
{
    private static final WorldSettings SETTINGS = new WorldSettings(0L, GameType.CREATIVE, false, false, WorldType.FLAT);
    private static final Minecraft MC = Minecraft.getMinecraft();
    @Nullable
    private static WorldSchematic world;

    @Nullable
    public static WorldSchematic getSchematicWorld()
    {
        return world;
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
            world = new WorldSchematic(null, SETTINGS, 1, EnumDifficulty.PEACEFUL, MC.profiler);
            //this.world.addEventListener(LitematicaRenderer.getInstance().getRenderGlobal());
        }

        LitematicaRenderer.getInstance().onSchematicWorldChanged(world);
    }
}
