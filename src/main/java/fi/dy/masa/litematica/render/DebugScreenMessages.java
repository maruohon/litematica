package fi.dy.masa.litematica.render;

import java.util.List;
import net.minecraft.client.renderer.RenderGlobal;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

public class DebugScreenMessages
{
    public static void addDebugScreenMessages(List<String> list)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null)
        {
            RenderGlobal render = LitematicaRenderer.getInstance().getWorldRenderer();

            /*
               world.getRegularEntityCount(),
               world.getChunkProvider().getLoadedChunks().size(),
               DataManager.getSchematicPlacementManager().getAllTouchedSubChunks().size(),
               DataManager.getSchematicPlacementManager().getLastVisibleSubChunks().size());
             */
            list.add(String.format("§6[Litematica]§r %s", render.getDebugInfoRenders()));
            list.add(String.format("§6[Litematica]§r %s E: %s BE: %d",
                                   render.getDebugInfoEntities(),
                                   world.getDebugLoadedEntities(),
                                   world.loadedTileEntityList.size()));
        }
    }
}
