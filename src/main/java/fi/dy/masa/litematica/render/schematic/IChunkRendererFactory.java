package fi.dy.masa.litematica.render.schematic;

import fi.dy.masa.litematica.world.WorldSchematic;

public interface IChunkRendererFactory
{
    ChunkRendererSchematicVbo create(WorldSchematic worldIn, WorldRendererSchematic worldRenderer);
}
