package litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.util.BlockRenderLayer;

@Mixin(CompiledChunk.class)
public interface IMixinCompiledChunk
{
    @Invoker
    void invokeSetLayerUsed(BlockRenderLayer layer);
}
