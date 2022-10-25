package litematica.render.schematic;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.IRenderChunkFactory;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.world.World;

public class RenderChunkFactoryList implements IRenderChunkFactory
{
    @Override
    public RenderChunk create(World worldIn, RenderGlobal renderGlobalIn, int index)
    {
        return new RenderChunkSchematicList(worldIn, renderGlobalIn, index);
    }
}
