package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
import net.minecraft.client.renderer.chunk.RenderChunk;

@Mixin(RenderChunk.class)
public interface IMixinRenderChunk
{
    @Accessor
    ChunkCompileTaskGenerator getCompileTask();

    @Accessor
    void setCompileTask(ChunkCompileTaskGenerator compileTask);
}
