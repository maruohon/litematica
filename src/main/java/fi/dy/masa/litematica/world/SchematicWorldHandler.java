package fi.dy.masa.litematica.world;

import javax.annotation.Nullable;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicPlacement;
import fi.dy.masa.litematica.data.SchematicPlacementManager;
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
    private WorldClient world;

    public static SchematicWorldHandler getInstance()
    {
        return INSTANCE;
    }

    public void onClientWorldChange(WorldClient worldClient)
    {
        System.out.printf("SchematicWorldHandler#onClientWorldChange(): %s\n", worldClient);
        this.recreateSchematicWorld(worldClient == null);
    }

    @Nullable
    public WorldClient getSchematicWorld()
    {
        return this.world;
    }

    public void loadChunk(int chunkX, int chunkZ, boolean loadChunk)
    {
        if (this.world != null)
        {
            /*
            System.out.printf("loadChunk() 1: %3d, %3d - %s - chunk: %s\n", chunkX, chunkZ, loadChunk ? "LOAD" : "unload", this.world.getChunkFromChunkCoords(chunkX, chunkZ));
            this.world.doPreChunk(chunkX, chunkZ, loadChunk);
            System.out.printf("loadChunk() 2: %3d, %3d - %s - chunk: %s\n", chunkX, chunkZ, loadChunk ? "LOAD" : "unload", this.world.getChunkFromChunkCoords(chunkX, chunkZ));
            */
        }
    }

    private void recreateSchematicWorld(boolean remove)
    {
        System.out.printf("SchematicWorldHandler#recreateSchematicWorld(): remove = %s\n", remove);
        if (remove)
        {
            this.world = null;
        }
        else
        {
            this.world = new WorldClient(null, this.settings, 0, EnumDifficulty.PEACEFUL, this.mc.mcProfiler);

            if (this.mc.player != null)
            {
                int r = this.mc.gameSettings.renderDistanceChunks + 1;
                int centerX = ((int) this.mc.player.posX) >> 4;
                int centerZ = ((int) this.mc.player.posZ) >> 4;

                for (int cz = centerZ - r; cz <= centerZ + r; ++cz)
                {
                    for (int cx = centerX - r; cx <= centerX + r; ++cx)
                    {
                        this.world.getChunkProvider().loadChunk(cx, cz);
                    }
                }
            }
        }

        LitematicaRenderer.getInstance().onSchematicWorldChanged(this.world);
    }

    public void rebuildSchematicWorld(boolean placeActiveSchematics)
    {
        System.out.printf("SchematicWorldHandler#rebuildSchematicWorld(): %s\n", placeActiveSchematics);
        this.recreateSchematicWorld(this.mc.world == null);

        if (placeActiveSchematics && this.world != null)
        {
            SchematicPlacementManager placementManager = DataManager.getInstance(this.mc.world).getSchematicPlacementManager();

            for (SchematicPlacement placement : placementManager.getAllSchematicsPlacements())
            {
                if (placement.getRenderSchematic())
                {
                    placement.getSchematic().placeToWorld(this.world, placement, false);
                }
            }
        }
    }
}
