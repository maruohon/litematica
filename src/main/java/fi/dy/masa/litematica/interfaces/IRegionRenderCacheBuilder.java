package fi.dy.masa.litematica.interfaces;

import net.minecraft.client.renderer.BufferBuilder;

public interface IRegionRenderCacheBuilder
{
    BufferBuilder getOverlayBuffer(boolean outlineBuffer);
}
