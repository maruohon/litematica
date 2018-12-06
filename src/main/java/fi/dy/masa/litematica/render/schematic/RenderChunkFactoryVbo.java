package fi.dy.masa.litematica.render.schematic;

import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.chunk.IRenderChunkFactory;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.world.World;

public class RenderChunkFactoryVbo implements IRenderChunkFactory
{
    @Override
    public RenderChunk create(World worldIn, WorldRenderer worldRenderer)
    {
        return new RenderChunkSchematicVbo(worldIn, worldRenderer);
    }
}
