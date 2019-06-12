package fi.dy.masa.litematica.render.schematic;

import fi.dy.masa.litematica.world.WorldSchematic;

public class RenderChunkFactoryList implements IChunkRendererFactory
{
    @Override
    public ChunkRendererSchematicVbo create(WorldSchematic worldIn, WorldRendererSchematic worldRenderer)
    {
        return new ChunkRendererSchematicDisplaylist(worldIn, worldRenderer);
    }
}
