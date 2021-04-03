package fi.dy.masa.litematica.mixin;

import java.util.BitSet;
import javax.annotation.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(net.minecraft.client.renderer.BlockModelRenderer.class)
public interface IMixinBlockModelRenderer
{
    @Invoker("getQuadDimensions")
    public void invokeGetQuadDimensions(
            net.minecraft.world.IBlockDisplayReader world,
            net.minecraft.block.BlockState state,
            net.minecraft.util.math.BlockPos pos,
            int[] vertexData,
            net.minecraft.util.Direction face,
            @Nullable float[] box, BitSet flags);

    @Invoker("renderQuad")
    public void invokeRenderQuad(
            net.minecraft.world.IBlockDisplayReader world,
            net.minecraft.block.BlockState state,
            net.minecraft.util.math.BlockPos pos,
            com.mojang.blaze3d.vertex.IVertexBuilder vertexConsumer,
            com.mojang.blaze3d.matrix.MatrixStack.Entry matrixEntry,
            net.minecraft.client.renderer.model.BakedQuad quad,
            float brightness0, float brightness1, float brightness2, float brightness3, int light0, int light1, int light2, int light3, int overlay);
}
