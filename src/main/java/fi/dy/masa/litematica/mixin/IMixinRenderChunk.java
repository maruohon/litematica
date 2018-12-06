package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.renderer.chunk.ChunkRenderTask;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.vertex.VertexBuffer;

@Mixin(RenderChunk.class)
public interface IMixinRenderChunk
{
    @Accessor
    ChunkRenderTask getCompileTask();

    @Accessor
    void setCompileTask(ChunkRenderTask compileTask);

    @Accessor
    VertexBuffer[] getVertexBuffers();
}
