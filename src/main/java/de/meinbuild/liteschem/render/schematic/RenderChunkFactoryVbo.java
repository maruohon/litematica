package de.meinbuild.liteschem.render.schematic;

import de.meinbuild.liteschem.world.WorldSchematic;

public class RenderChunkFactoryVbo implements IChunkRendererFactory
{
    @Override
    public ChunkRendererSchematicVbo create(WorldSchematic worldIn, WorldRendererSchematic worldRenderer)
    {
        return new ChunkRendererSchematicVbo(worldIn, worldRenderer);
    }
}
