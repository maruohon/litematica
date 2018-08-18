package fi.dy.masa.litematica.interfaces;

import com.google.common.util.concurrent.ListenableFuture;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.chunk.RenderChunk;

public interface IChunkRenderDispatcher
{
    ListenableFuture<Object> uploadChunkOverlay(BufferBuilder buffer, RenderChunk renderChunk, boolean outlineBuffer, double distanceSq);
}
