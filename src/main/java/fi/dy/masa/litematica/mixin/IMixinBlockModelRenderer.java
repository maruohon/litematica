package fi.dy.masa.litematica.mixin;

import java.util.BitSet;
import javax.annotation.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(net.minecraft.client.render.block.BlockModelRenderer.class)
public interface IMixinBlockModelRenderer
{
    @Invoker("getQuadDimensions")
    public void invokeGetQuadDimensions(
            net.minecraft.world.BlockRenderView world,
            net.minecraft.block.BlockState state,
            net.minecraft.util.math.BlockPos pos,
            int[] vertexData,
            net.minecraft.util.math.Direction face,
            @Nullable float[] box, BitSet flags);

    @Invoker("renderQuad")
    public void invokeRenderQuad(
            net.minecraft.world.BlockRenderView world,
            net.minecraft.block.BlockState state,
            net.minecraft.util.math.BlockPos pos,
            net.minecraft.client.render.VertexConsumer vertexConsumer,
            net.minecraft.client.util.math.MatrixStack.Entry matrixEntry,
            net.minecraft.client.render.model.BakedQuad quad,
            float brightness0, float brightness1, float brightness2, float brightness3, int light0, int light1, int light2, int light3, int overlay);
}
