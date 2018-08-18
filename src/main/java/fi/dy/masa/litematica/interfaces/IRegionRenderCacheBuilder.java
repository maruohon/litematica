package fi.dy.masa.litematica.interfaces;

import fi.dy.masa.litematica.render.schematic.RenderChunkSchematicVbo.OverlayType;
import net.minecraft.client.renderer.BufferBuilder;

public interface IRegionRenderCacheBuilder
{
    BufferBuilder getOverlayBuffer(OverlayType type);
}
