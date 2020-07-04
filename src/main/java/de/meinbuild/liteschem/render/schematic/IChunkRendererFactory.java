package de.meinbuild.liteschem.render.schematic;

import de.meinbuild.liteschem.world.WorldSchematic;

public interface IChunkRendererFactory
{
    ChunkRendererSchematicVbo create(WorldSchematic worldIn, WorldRendererSchematic worldRenderer);
}
