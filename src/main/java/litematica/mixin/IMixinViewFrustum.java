package litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.math.BlockPos;

@Mixin(ViewFrustum.class)
public interface IMixinViewFrustum
{
    @Invoker
    RenderChunk invokeGetRenderChunk(BlockPos pos);
}
