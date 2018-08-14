package fi.dy.masa.litematica.interfaces;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;

public interface IMixinRenderGlobal
{
    ChunkRenderDispatcher getChunkRenderDispatcher();
}
