package fi.dy.masa.litematica.world;

import javax.annotation.Nullable;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

public class SchematicWorldHandler
{
    private static final SchematicWorldHandler INSTANCE = new SchematicWorldHandler();

    private final WorldSettings settings = new WorldSettings(0L, GameType.CREATIVE, false, false, WorldType.FLAT);
    private final Minecraft mc = Minecraft.getMinecraft();
    @Nullable
    private WorldSchematic world;

    public static SchematicWorldHandler getInstance()
    {
        return INSTANCE;
    }

    public void onClientWorldChange(WorldClient worldClient)
    {
        this.recreateSchematicWorld(worldClient == null);
    }

    @Nullable
    public WorldSchematic getSchematicWorld()
    {
        return this.world;
    }

    private void recreateSchematicWorld(boolean remove)
    {
        if (remove)
        {
            this.world = null;
        }
        else
        {
            // Note: The dimension used here must have no skylight, because the custom Chunks don't have those arrays
            this.world = new WorldSchematic(null, this.settings, -1, EnumDifficulty.PEACEFUL, this.mc.mcProfiler);
        }

        LitematicaRenderer.getInstance().onSchematicWorldChanged(this.world);
    }
}
