package fi.dy.masa.litematica.render.schematic;

import java.util.BitSet;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.ExtendedBlockView;

public class BlockModelRendererSchematic extends BlockModelRenderer
{
    private final BlockColors blockColors;
    private final Random random = new Random();

    public BlockModelRendererSchematic(BlockColors blockColorsIn)
    {
        super(blockColorsIn);

        this.blockColors = blockColorsIn;
    }

    public boolean renderModel(ExtendedBlockView worldIn, BakedModel modelIn, BlockState stateIn, BlockPos posIn, BufferBuilder buffer, long rand)
    {
        boolean ao = MinecraftClient.isAmbientOcclusionEnabled() && stateIn.getLuminance() == 0 && modelIn.useAmbientOcclusion();

        try
        {
            if (ao)
            {
                return this.renderModelSmooth(worldIn, modelIn, stateIn, posIn, buffer, this.random, rand);
            }
            else
            {
                return this.renderModelFlat(worldIn, modelIn, stateIn, posIn, buffer, this.random, rand);
            }
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.create(throwable, "Tesselating block model");
            CrashReportSection crashreportcategory = crashreport.addElement("Block model being tesselated");
            CrashReportSection.addBlockInfo(crashreportcategory, posIn, stateIn);
            crashreportcategory.add("Using AO", Boolean.valueOf(ao));
            throw new CrashException(crashreport);
        }
    }

    public boolean renderModelSmooth(ExtendedBlockView worldIn, BakedModel modelIn, BlockState stateIn, BlockPos posIn, BufferBuilder buffer, Random random, long seedIn)
    {
        boolean renderedSomething = false;
        float[] quadBounds = new float[Direction.values().length * 2];
        BitSet bitset = new BitSet(3);
        AmbientOcclusionFace aoFace = new AmbientOcclusionFace();

        for (Direction side : Direction.values())
        {
            random.setSeed(seedIn);
            List<BakedQuad> quads = modelIn.getQuads(stateIn, side, random);

            if (quads.isEmpty() == false)
            {
                if (this.shouldRenderModelSide(worldIn, stateIn, posIn, side))
                {
                    this.renderQuadsSmooth(worldIn, stateIn, posIn, buffer, quads, quadBounds, bitset, aoFace);
                    renderedSomething = true;
                }
            }
        }

        random.setSeed(seedIn);
        List<BakedQuad> quads = modelIn.getQuads(stateIn, (Direction) null, random);

        if (quads.isEmpty() == false)
        {
            this.renderQuadsSmooth(worldIn, stateIn, posIn, buffer, quads, quadBounds, bitset, aoFace);
            renderedSomething = true;
        }

        return renderedSomething;
    }

    public boolean renderModelFlat(ExtendedBlockView worldIn, BakedModel modelIn, BlockState stateIn, BlockPos posIn, BufferBuilder buffer, Random random, long seedIn)
    {
        boolean renderedSomething = false;
        BitSet bitset = new BitSet(3);

        for (Direction side : Direction.values())
        {
            random.setSeed(seedIn);
            List<BakedQuad> quads = modelIn.getQuads(stateIn, side, random);

            if (quads.isEmpty() == false)
            {
                if (this.shouldRenderModelSide(worldIn, stateIn, posIn, side))
                {
                    int lightMapCoords = worldIn.getLightmapIndex(stateIn, posIn.offset(side));
                    this.renderQuadsFlat(worldIn, stateIn, posIn, lightMapCoords, false, buffer, quads, bitset);
                    renderedSomething = true;
                }
            }
        }

        random.setSeed(seedIn);
        List<BakedQuad> quads = modelIn.getQuads(stateIn, null, random);

        if (quads.isEmpty() == false)
        {
            this.renderQuadsFlat(worldIn, stateIn, posIn, -1, true, buffer, quads, bitset);
            renderedSomething = true;
        }

        return renderedSomething;
    }

    private boolean shouldRenderModelSide(ExtendedBlockView worldIn, BlockState stateIn, BlockPos posIn, Direction side)
    {
        return DataManager.getRenderLayerRange().isPositionAtRenderEdgeOnSide(posIn, side) ||
               (Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue() && Configs.Visuals.RENDER_TRANSLUCENT_INNER_SIDES.getBooleanValue()) ||
               Block.shouldDrawSide(stateIn, worldIn, posIn, side);
    }

    private void renderQuadsSmooth(ExtendedBlockView world, BlockState state, BlockPos pos, BufferBuilder buffer, List<BakedQuad> list, float[] quadBounds, BitSet bitSet, AmbientOcclusionFace aoFace)
    {
        Vec3d vec3d = state.getOffsetPos(world, pos);
        double x = (double) pos.getX() + vec3d.x;
        double y = (double) pos.getY() + vec3d.y;
        double z = (double) pos.getZ() + vec3d.z;
        int i = 0;

        for (int j = list.size(); i < j; ++i)
        {
            BakedQuad bakedquad = list.get(i);
            this.fillQuadBounds(world, state, pos, bakedquad.getVertexData(), bakedquad.getFace(), quadBounds, bitSet);
            aoFace.updateVertexBrightness(world, state, pos, bakedquad.getFace(), quadBounds, bitSet);
            buffer.putVertexData(bakedquad.getVertexData());
            buffer.brightness(aoFace.vertexBrightness[0], aoFace.vertexBrightness[1], aoFace.vertexBrightness[2], aoFace.vertexBrightness[3]);

            if (bakedquad.hasColor())
            {
                int k = this.blockColors.getColorMultiplier(state, world, pos, bakedquad.getColorIndex());
                float f = (float)(k >> 16 & 255) / 255.0F;
                float f1 = (float)(k >> 8 & 255) / 255.0F;
                float f2 = (float)(k & 255) / 255.0F;
                buffer.multiplyColor(aoFace.vertexColorMultiplier[0] * f, aoFace.vertexColorMultiplier[0] * f1, aoFace.vertexColorMultiplier[0] * f2, 4);
                buffer.multiplyColor(aoFace.vertexColorMultiplier[1] * f, aoFace.vertexColorMultiplier[1] * f1, aoFace.vertexColorMultiplier[1] * f2, 3);
                buffer.multiplyColor(aoFace.vertexColorMultiplier[2] * f, aoFace.vertexColorMultiplier[2] * f1, aoFace.vertexColorMultiplier[2] * f2, 2);
                buffer.multiplyColor(aoFace.vertexColorMultiplier[3] * f, aoFace.vertexColorMultiplier[3] * f1, aoFace.vertexColorMultiplier[3] * f2, 1);
            }
            else
            {
                buffer.multiplyColor(aoFace.vertexColorMultiplier[0], aoFace.vertexColorMultiplier[0], aoFace.vertexColorMultiplier[0], 4);
                buffer.multiplyColor(aoFace.vertexColorMultiplier[1], aoFace.vertexColorMultiplier[1], aoFace.vertexColorMultiplier[1], 3);
                buffer.multiplyColor(aoFace.vertexColorMultiplier[2], aoFace.vertexColorMultiplier[2], aoFace.vertexColorMultiplier[2], 2);
                buffer.multiplyColor(aoFace.vertexColorMultiplier[3], aoFace.vertexColorMultiplier[3], aoFace.vertexColorMultiplier[3], 1);
            }

            buffer.postPosition(x, y, z);
        }
    }

    private void renderQuadsFlat(ExtendedBlockView world, BlockState state, BlockPos pos, int brightnessIn, boolean ownBrightness, BufferBuilder buffer, List<BakedQuad> list, BitSet bitSet)
    {
        Vec3d vec3d = state.getOffsetPos(world, pos);
        double d0 = (double)pos.getX() + vec3d.x;
        double d1 = (double)pos.getY() + vec3d.y;
        double d2 = (double)pos.getZ() + vec3d.z;
        int i = 0;

        for (int j = list.size(); i < j; ++i)
        {
            BakedQuad bakedquad = list.get(i);

            if (ownBrightness)
            {
                this.fillQuadBounds(world, state, pos, bakedquad.getVertexData(), bakedquad.getFace(), (float[]) null, bitSet);
                BlockPos blockpos = bitSet.get(0) ? pos.offset(bakedquad.getFace()) : pos;
                brightnessIn = world.getLightmapIndex(state, blockpos);
            }

            buffer.putVertexData(bakedquad.getVertexData());
            buffer.brightness(brightnessIn, brightnessIn, brightnessIn, brightnessIn);

            if (bakedquad.hasColor())
            {
                int k = this.blockColors.getColorMultiplier(state, world, pos, bakedquad.getColorIndex());
                float f = (float)(k >> 16 & 255) / 255.0F;
                float f1 = (float)(k >> 8 & 255) / 255.0F;
                float f2 = (float)(k & 255) / 255.0F;
                buffer.multiplyColor(f, f1, f2, 4);
                buffer.multiplyColor(f, f1, f2, 3);
                buffer.multiplyColor(f, f1, f2, 2);
                buffer.multiplyColor(f, f1, f2, 1);
            }

            buffer.postPosition(d0, d1, d2);
        }
    }

    private void fillQuadBounds(ExtendedBlockView world, BlockState stateIn, BlockPos pos, int[] vertexData, Direction face, @Nullable float[] quadBounds, BitSet boundsFlags)
    {
        float f = 32.0F;
        float f1 = 32.0F;
        float f2 = 32.0F;
        float f3 = -32.0F;
        float f4 = -32.0F;
        float f5 = -32.0F;

        for (int i = 0; i < 4; ++i)
        {
            float f6 = Float.intBitsToFloat(vertexData[i * 7]);
            float f7 = Float.intBitsToFloat(vertexData[i * 7 + 1]);
            float f8 = Float.intBitsToFloat(vertexData[i * 7 + 2]);
            f = Math.min(f, f6);
            f1 = Math.min(f1, f7);
            f2 = Math.min(f2, f8);
            f3 = Math.max(f3, f6);
            f4 = Math.max(f4, f7);
            f5 = Math.max(f5, f8);
        }

        if (quadBounds != null)
        {
            quadBounds[Direction.WEST.getId()] = f;
            quadBounds[Direction.EAST.getId()] = f3;
            quadBounds[Direction.DOWN.getId()] = f1;
            quadBounds[Direction.UP.getId()] = f4;
            quadBounds[Direction.NORTH.getId()] = f2;
            quadBounds[Direction.SOUTH.getId()] = f5;
            int j = Direction.values().length;
            quadBounds[Direction.WEST.getId() + j] = 1.0F - f;
            quadBounds[Direction.EAST.getId() + j] = 1.0F - f3;
            quadBounds[Direction.DOWN.getId() + j] = 1.0F - f1;
            quadBounds[Direction.UP.getId() + j] = 1.0F - f4;
            quadBounds[Direction.NORTH.getId() + j] = 1.0F - f2;
            quadBounds[Direction.SOUTH.getId() + j] = 1.0F - f5;
        }

        switch (face)
        {
            case DOWN:
                boundsFlags.set(1, f >= 1.0E-4F || f2 >= 1.0E-4F || f3 <= 0.9999F || f5 <= 0.9999F);
                boundsFlags.set(0, (f1 < 1.0E-4F || Block.isShapeFullCube(stateIn.getCollisionShape(world, pos))) && f1 == f4);
                break;
            case UP:
                boundsFlags.set(1, f >= 1.0E-4F || f2 >= 1.0E-4F || f3 <= 0.9999F || f5 <= 0.9999F);
                boundsFlags.set(0, (f4 > 0.9999F || Block.isShapeFullCube(stateIn.getCollisionShape(world, pos))) && f1 == f4);
                break;
            case NORTH:
                boundsFlags.set(1, f >= 1.0E-4F || f1 >= 1.0E-4F || f3 <= 0.9999F || f4 <= 0.9999F);
                boundsFlags.set(0, (f2 < 1.0E-4F || Block.isShapeFullCube(stateIn.getCollisionShape(world, pos))) && f2 == f5);
                break;
            case SOUTH:
                boundsFlags.set(1, f >= 1.0E-4F || f1 >= 1.0E-4F || f3 <= 0.9999F || f4 <= 0.9999F);
                boundsFlags.set(0, (f5 > 0.9999F || Block.isShapeFullCube(stateIn.getCollisionShape(world, pos))) && f2 == f5);
                break;
            case WEST:
                boundsFlags.set(1, f1 >= 1.0E-4F || f2 >= 1.0E-4F || f4 <= 0.9999F || f5 <= 0.9999F);
                boundsFlags.set(0, (f < 1.0E-4F || Block.isShapeFullCube(stateIn.getCollisionShape(world, pos))) && f == f3);
                break;
            case EAST:
                boundsFlags.set(1, f1 >= 1.0E-4F || f2 >= 1.0E-4F || f4 <= 0.9999F || f5 <= 0.9999F);
                boundsFlags.set(0, (f3 > 0.9999F || Block.isShapeFullCube(stateIn.getCollisionShape(world, pos))) && f == f3);
        }
    }

    class AmbientOcclusionFace
    {
        private final float[] vertexColorMultiplier = new float[4];
        private final int[] vertexBrightness = new int[4];

        public void updateVertexBrightness(ExtendedBlockView worldIn, BlockState state, BlockPos centerPos, Direction direction, float[] faceShape, BitSet shapeState)
        {
            /*
            BlockPos blockpos = shapeState.get(0) ? centerPos.offset(direction) : centerPos;
            BlockPos.PooledMutableBlockPos blockpos$pooledmutableblockpos = BlockPos.PooledMutableBlockPos.retain();
            BlockPos.PooledMutableBlockPos blockpos$pooledmutableblockpos1 = BlockPos.PooledMutableBlockPos.retain(blockpos).move(neighborInfo.corners[0]);
            BlockPos.PooledMutableBlockPos blockpos$pooledmutableblockpos2 = BlockPos.PooledMutableBlockPos.retain(blockpos).move(neighborInfo.corners[1]);
            BlockPos.PooledMutableBlockPos blockpos$pooledmutableblockpos3 = BlockPos.PooledMutableBlockPos.retain(blockpos).move(neighborInfo.corners[2]);
            BlockPos.PooledMutableBlockPos blockpos$pooledmutableblockpos4 = BlockPos.PooledMutableBlockPos.retain(blockpos).move(neighborInfo.corners[3]);
            int i = state.getPackedLightmapCoords(worldIn, blockpos$pooledmutableblockpos1);
            int j = state.getPackedLightmapCoords(worldIn, blockpos$pooledmutableblockpos2);
            int k = state.getPackedLightmapCoords(worldIn, blockpos$pooledmutableblockpos3);
            int l = state.getPackedLightmapCoords(worldIn, blockpos$pooledmutableblockpos4);
            float f = worldIn.getBlockState(blockpos$pooledmutableblockpos1).getAmbientOcclusionLightValue();
            float f1 = worldIn.getBlockState(blockpos$pooledmutableblockpos2).getAmbientOcclusionLightValue();
            float f2 = worldIn.getBlockState(blockpos$pooledmutableblockpos3).getAmbientOcclusionLightValue();
            float f3 = worldIn.getBlockState(blockpos$pooledmutableblockpos4).getAmbientOcclusionLightValue();
            boolean flag = worldIn.getBlockState(blockpos$pooledmutableblockpos.setPos(blockpos$pooledmutableblockpos1).move(direction)).isTranslucent();
            boolean flag1 = worldIn.getBlockState(blockpos$pooledmutableblockpos.setPos(blockpos$pooledmutableblockpos2).move(direction)).isTranslucent();
            boolean flag2 = worldIn.getBlockState(blockpos$pooledmutableblockpos.setPos(blockpos$pooledmutableblockpos3).move(direction)).isTranslucent();
            boolean flag3 = worldIn.getBlockState(blockpos$pooledmutableblockpos.setPos(blockpos$pooledmutableblockpos4).move(direction)).isTranslucent();
            float f4;
            int i1;

            if (!flag2 && !flag)
            {
                f4 = f;
                i1 = i;
            }
            else
            {
                BlockPos blockpos1 = blockpos$pooledmutableblockpos.setPos(blockpos$pooledmutableblockpos1).move(neighborInfo.corners[2]);
                f4 = worldIn.getBlockState(blockpos1).getAmbientOcclusionLightValue();
                i1 = state.getPackedLightmapCoords(worldIn, blockpos1);
            }

            float f5;
            int j1;

            if (!flag3 && !flag)
            {
                f5 = f;
                j1 = i;
            }
            else
            {
                BlockPos blockpos2 = blockpos$pooledmutableblockpos.setPos(blockpos$pooledmutableblockpos1).move(neighborInfo.corners[3]);
                f5 = worldIn.getBlockState(blockpos2).getAmbientOcclusionLightValue();
                j1 = state.getPackedLightmapCoords(worldIn, blockpos2);
            }

            float f6;
            int k1;

            if (!flag2 && !flag1)
            {
                f6 = f1;
                k1 = j;
            }
            else
            {
                BlockPos blockpos3 = blockpos$pooledmutableblockpos.setPos(blockpos$pooledmutableblockpos2).move(neighborInfo.corners[2]);
                f6 = worldIn.getBlockState(blockpos3).getAmbientOcclusionLightValue();
                k1 = state.getPackedLightmapCoords(worldIn, blockpos3);
            }

            float f7;
            int l1;

            if (!flag3 && !flag1)
            {
                f7 = f1;
                l1 = j;
            }
            else
            {
                BlockPos blockpos4 = blockpos$pooledmutableblockpos.setPos(blockpos$pooledmutableblockpos2).move(neighborInfo.corners[3]);
                f7 = worldIn.getBlockState(blockpos4).getAmbientOcclusionLightValue();
                l1 = state.getPackedLightmapCoords(worldIn, blockpos4);
            }

            int i3 = state.getPackedLightmapCoords(worldIn, centerPos);

            if (shapeState.get(0) || !worldIn.getBlockState(centerPos.offset(direction)).isOpaqueCube())
            {
                i3 = state.getPackedLightmapCoords(worldIn, centerPos.offset(direction));
            }

            float f8 = shapeState.get(0) ? worldIn.getBlockState(blockpos).getAmbientOcclusionLightValue() : worldIn.getBlockState(centerPos).getAmbientOcclusionLightValue();
            VertexTranslations vertexTranslations = VertexTranslations.getVertexTranslations(direction);
            blockpos$pooledmutableblockpos.release();
            blockpos$pooledmutableblockpos1.release();
            blockpos$pooledmutableblockpos2.release();
            blockpos$pooledmutableblockpos3.release();
            blockpos$pooledmutableblockpos4.release();
            */

            EnumNeighborInfo neighborInfo = EnumNeighborInfo.getNeighbourInfo(direction);
            VertexTranslations vertexTranslations = VertexTranslations.getVertexTranslations(direction);
            float f, f1, f2, f3, f4, f5, f6, f7, f8;
            f = f1 = f2 = f3 = f4 = f5 = f6 = f7 = f8 = 1.0F;
            int i, j, k, l, i1, i3, j1, k1, l1;
            i = j = k = l = i1 = i3 = j1 = k1 = l1 = ((15 << 20) | (15 << 4));

            if (shapeState.get(1) && neighborInfo.doNonCubicWeight)
            {
                float f29 = (f3 + f + f5 + f8) * 0.25F;
                float f30 = (f2 + f + f4 + f8) * 0.25F;
                float f31 = (f2 + f1 + f6 + f8) * 0.25F;
                float f32 = (f3 + f1 + f7 + f8) * 0.25F;
                float f13 = faceShape[neighborInfo.vert0Weights[0].shape] * faceShape[neighborInfo.vert0Weights[1].shape];
                float f14 = faceShape[neighborInfo.vert0Weights[2].shape] * faceShape[neighborInfo.vert0Weights[3].shape];
                float f15 = faceShape[neighborInfo.vert0Weights[4].shape] * faceShape[neighborInfo.vert0Weights[5].shape];
                float f16 = faceShape[neighborInfo.vert0Weights[6].shape] * faceShape[neighborInfo.vert0Weights[7].shape];
                float f17 = faceShape[neighborInfo.vert1Weights[0].shape] * faceShape[neighborInfo.vert1Weights[1].shape];
                float f18 = faceShape[neighborInfo.vert1Weights[2].shape] * faceShape[neighborInfo.vert1Weights[3].shape];
                float f19 = faceShape[neighborInfo.vert1Weights[4].shape] * faceShape[neighborInfo.vert1Weights[5].shape];
                float f20 = faceShape[neighborInfo.vert1Weights[6].shape] * faceShape[neighborInfo.vert1Weights[7].shape];
                float f21 = faceShape[neighborInfo.vert2Weights[0].shape] * faceShape[neighborInfo.vert2Weights[1].shape];
                float f22 = faceShape[neighborInfo.vert2Weights[2].shape] * faceShape[neighborInfo.vert2Weights[3].shape];
                float f23 = faceShape[neighborInfo.vert2Weights[4].shape] * faceShape[neighborInfo.vert2Weights[5].shape];
                float f24 = faceShape[neighborInfo.vert2Weights[6].shape] * faceShape[neighborInfo.vert2Weights[7].shape];
                float f25 = faceShape[neighborInfo.vert3Weights[0].shape] * faceShape[neighborInfo.vert3Weights[1].shape];
                float f26 = faceShape[neighborInfo.vert3Weights[2].shape] * faceShape[neighborInfo.vert3Weights[3].shape];
                float f27 = faceShape[neighborInfo.vert3Weights[4].shape] * faceShape[neighborInfo.vert3Weights[5].shape];
                float f28 = faceShape[neighborInfo.vert3Weights[6].shape] * faceShape[neighborInfo.vert3Weights[7].shape];
                this.vertexColorMultiplier[vertexTranslations.vert0] = f29 * f13 + f30 * f14 + f31 * f15 + f32 * f16;
                this.vertexColorMultiplier[vertexTranslations.vert1] = f29 * f17 + f30 * f18 + f31 * f19 + f32 * f20;
                this.vertexColorMultiplier[vertexTranslations.vert2] = f29 * f21 + f30 * f22 + f31 * f23 + f32 * f24;
                this.vertexColorMultiplier[vertexTranslations.vert3] = f29 * f25 + f30 * f26 + f31 * f27 + f32 * f28;
                int i2 = this.getAoBrightness(l, i, j1, i3);
                int j2 = this.getAoBrightness(k, i, i1, i3);
                int k2 = this.getAoBrightness(k, j, k1, i3);
                int l2 = this.getAoBrightness(l, j, l1, i3);
                this.vertexBrightness[vertexTranslations.vert0] = this.getVertexBrightness(i2, j2, k2, l2, f13, f14, f15, f16);
                this.vertexBrightness[vertexTranslations.vert1] = this.getVertexBrightness(i2, j2, k2, l2, f17, f18, f19, f20);
                this.vertexBrightness[vertexTranslations.vert2] = this.getVertexBrightness(i2, j2, k2, l2, f21, f22, f23, f24);
                this.vertexBrightness[vertexTranslations.vert3] = this.getVertexBrightness(i2, j2, k2, l2, f25, f26, f27, f28);
            }
            else
            {
                float f9 = (f3 + f + f5 + f8) * 0.25F;
                float f10 = (f2 + f + f4 + f8) * 0.25F;
                float f11 = (f2 + f1 + f6 + f8) * 0.25F;
                float f12 = (f3 + f1 + f7 + f8) * 0.25F;
                this.vertexBrightness[vertexTranslations.vert0] = this.getAoBrightness(l, i, j1, i3);
                this.vertexBrightness[vertexTranslations.vert1] = this.getAoBrightness(k, i, i1, i3);
                this.vertexBrightness[vertexTranslations.vert2] = this.getAoBrightness(k, j, k1, i3);
                this.vertexBrightness[vertexTranslations.vert3] = this.getAoBrightness(l, j, l1, i3);
                this.vertexColorMultiplier[vertexTranslations.vert0] = f9;
                this.vertexColorMultiplier[vertexTranslations.vert1] = f10;
                this.vertexColorMultiplier[vertexTranslations.vert2] = f11;
                this.vertexColorMultiplier[vertexTranslations.vert3] = f12;
            }
        }

        /**
         * Get ambient occlusion brightness
         */
        private int getAoBrightness(int br1, int br2, int br3, int br4)
        {
            if (br1 == 0)
            {
                br1 = br4;
            }

            if (br2 == 0)
            {
                br2 = br4;
            }

            if (br3 == 0)
            {
                br3 = br4;
            }

            return br1 + br2 + br3 + br4 >> 2 & 16711935;
        }

        private int getVertexBrightness(int p_178203_1_, int p_178203_2_, int p_178203_3_, int p_178203_4_, float p_178203_5_, float p_178203_6_, float p_178203_7_, float p_178203_8_)
        {
            int i = (int)((float)(p_178203_1_ >> 16 & 255) * p_178203_5_ + (float)(p_178203_2_ >> 16 & 255) * p_178203_6_ + (float)(p_178203_3_ >> 16 & 255) * p_178203_7_ + (float)(p_178203_4_ >> 16 & 255) * p_178203_8_) & 255;
            int j = (int)((float)(p_178203_1_ & 255) * p_178203_5_ + (float)(p_178203_2_ & 255) * p_178203_6_ + (float)(p_178203_3_ & 255) * p_178203_7_ + (float)(p_178203_4_ & 255) * p_178203_8_) & 255;
            return i << 16 | j;
        }
    }

    public static enum EnumNeighborInfo
    {
        DOWN(new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH}, 0.5F, true, new Orientation[]{Orientation.FLIP_WEST, Orientation.SOUTH, Orientation.FLIP_WEST, Orientation.FLIP_SOUTH, Orientation.WEST, Orientation.FLIP_SOUTH, Orientation.WEST, Orientation.SOUTH}, new Orientation[]{Orientation.FLIP_WEST, Orientation.NORTH, Orientation.FLIP_WEST, Orientation.FLIP_NORTH, Orientation.WEST, Orientation.FLIP_NORTH, Orientation.WEST, Orientation.NORTH}, new Orientation[]{Orientation.FLIP_EAST, Orientation.NORTH, Orientation.FLIP_EAST, Orientation.FLIP_NORTH, Orientation.EAST, Orientation.FLIP_NORTH, Orientation.EAST, Orientation.NORTH}, new Orientation[]{Orientation.FLIP_EAST, Orientation.SOUTH, Orientation.FLIP_EAST, Orientation.FLIP_SOUTH, Orientation.EAST, Orientation.FLIP_SOUTH, Orientation.EAST, Orientation.SOUTH}),
        UP(new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}, 1.0F, true, new Orientation[]{Orientation.EAST, Orientation.SOUTH, Orientation.EAST, Orientation.FLIP_SOUTH, Orientation.FLIP_EAST, Orientation.FLIP_SOUTH, Orientation.FLIP_EAST, Orientation.SOUTH}, new Orientation[]{Orientation.EAST, Orientation.NORTH, Orientation.EAST, Orientation.FLIP_NORTH, Orientation.FLIP_EAST, Orientation.FLIP_NORTH, Orientation.FLIP_EAST, Orientation.NORTH}, new Orientation[]{Orientation.WEST, Orientation.NORTH, Orientation.WEST, Orientation.FLIP_NORTH, Orientation.FLIP_WEST, Orientation.FLIP_NORTH, Orientation.FLIP_WEST, Orientation.NORTH}, new Orientation[]{Orientation.WEST, Orientation.SOUTH, Orientation.WEST, Orientation.FLIP_SOUTH, Orientation.FLIP_WEST, Orientation.FLIP_SOUTH, Orientation.FLIP_WEST, Orientation.SOUTH}),
        NORTH(new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST}, 0.8F, true, new Orientation[]{Orientation.UP, Orientation.FLIP_WEST, Orientation.UP, Orientation.WEST, Orientation.FLIP_UP, Orientation.WEST, Orientation.FLIP_UP, Orientation.FLIP_WEST}, new Orientation[]{Orientation.UP, Orientation.FLIP_EAST, Orientation.UP, Orientation.EAST, Orientation.FLIP_UP, Orientation.EAST, Orientation.FLIP_UP, Orientation.FLIP_EAST}, new Orientation[]{Orientation.DOWN, Orientation.FLIP_EAST, Orientation.DOWN, Orientation.EAST, Orientation.FLIP_DOWN, Orientation.EAST, Orientation.FLIP_DOWN, Orientation.FLIP_EAST}, new Orientation[]{Orientation.DOWN, Orientation.FLIP_WEST, Orientation.DOWN, Orientation.WEST, Orientation.FLIP_DOWN, Orientation.WEST, Orientation.FLIP_DOWN, Orientation.FLIP_WEST}),
        SOUTH(new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP}, 0.8F, true, new Orientation[]{Orientation.UP, Orientation.FLIP_WEST, Orientation.FLIP_UP, Orientation.FLIP_WEST, Orientation.FLIP_UP, Orientation.WEST, Orientation.UP, Orientation.WEST}, new Orientation[]{Orientation.DOWN, Orientation.FLIP_WEST, Orientation.FLIP_DOWN, Orientation.FLIP_WEST, Orientation.FLIP_DOWN, Orientation.WEST, Orientation.DOWN, Orientation.WEST}, new Orientation[]{Orientation.DOWN, Orientation.FLIP_EAST, Orientation.FLIP_DOWN, Orientation.FLIP_EAST, Orientation.FLIP_DOWN, Orientation.EAST, Orientation.DOWN, Orientation.EAST}, new Orientation[]{Orientation.UP, Orientation.FLIP_EAST, Orientation.FLIP_UP, Orientation.FLIP_EAST, Orientation.FLIP_UP, Orientation.EAST, Orientation.UP, Orientation.EAST}),
        WEST(new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH}, 0.6F, true, new Orientation[]{Orientation.UP, Orientation.SOUTH, Orientation.UP, Orientation.FLIP_SOUTH, Orientation.FLIP_UP, Orientation.FLIP_SOUTH, Orientation.FLIP_UP, Orientation.SOUTH}, new Orientation[]{Orientation.UP, Orientation.NORTH, Orientation.UP, Orientation.FLIP_NORTH, Orientation.FLIP_UP, Orientation.FLIP_NORTH, Orientation.FLIP_UP, Orientation.NORTH}, new Orientation[]{Orientation.DOWN, Orientation.NORTH, Orientation.DOWN, Orientation.FLIP_NORTH, Orientation.FLIP_DOWN, Orientation.FLIP_NORTH, Orientation.FLIP_DOWN, Orientation.NORTH}, new Orientation[]{Orientation.DOWN, Orientation.SOUTH, Orientation.DOWN, Orientation.FLIP_SOUTH, Orientation.FLIP_DOWN, Orientation.FLIP_SOUTH, Orientation.FLIP_DOWN, Orientation.SOUTH}),
        EAST(new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH}, 0.6F, true, new Orientation[]{Orientation.FLIP_DOWN, Orientation.SOUTH, Orientation.FLIP_DOWN, Orientation.FLIP_SOUTH, Orientation.DOWN, Orientation.FLIP_SOUTH, Orientation.DOWN, Orientation.SOUTH}, new Orientation[]{Orientation.FLIP_DOWN, Orientation.NORTH, Orientation.FLIP_DOWN, Orientation.FLIP_NORTH, Orientation.DOWN, Orientation.FLIP_NORTH, Orientation.DOWN, Orientation.NORTH}, new Orientation[]{Orientation.FLIP_UP, Orientation.NORTH, Orientation.FLIP_UP, Orientation.FLIP_NORTH, Orientation.UP, Orientation.FLIP_NORTH, Orientation.UP, Orientation.NORTH}, new Orientation[]{Orientation.FLIP_UP, Orientation.SOUTH, Orientation.FLIP_UP, Orientation.FLIP_SOUTH, Orientation.UP, Orientation.FLIP_SOUTH, Orientation.UP, Orientation.SOUTH});

        //private final Direction[] corners;
        //private final float shadeWeight;
        private final boolean doNonCubicWeight;
        private final Orientation[] vert0Weights;
        private final Orientation[] vert1Weights;
        private final Orientation[] vert2Weights;
        private final Orientation[] vert3Weights;
        private static final EnumNeighborInfo[] VALUES = new EnumNeighborInfo[6];

        private EnumNeighborInfo(Direction[] p_i46236_3_, float p_i46236_4_, boolean p_i46236_5_, Orientation[] p_i46236_6_, Orientation[] p_i46236_7_, Orientation[] p_i46236_8_, Orientation[] p_i46236_9_)
        {
            //this.corners = p_i46236_3_;
            //this.shadeWeight = p_i46236_4_;
            this.doNonCubicWeight = p_i46236_5_;
            this.vert0Weights = p_i46236_6_;
            this.vert1Weights = p_i46236_7_;
            this.vert2Weights = p_i46236_8_;
            this.vert3Weights = p_i46236_9_;
        }

        public static EnumNeighborInfo getNeighbourInfo(Direction p_178273_0_)
        {
            return VALUES[p_178273_0_.getId()];
        }

        static
        {
            VALUES[Direction.DOWN.getId()] = DOWN;
            VALUES[Direction.UP.getId()] = UP;
            VALUES[Direction.NORTH.getId()] = NORTH;
            VALUES[Direction.SOUTH.getId()] = SOUTH;
            VALUES[Direction.WEST.getId()] = WEST;
            VALUES[Direction.EAST.getId()] = EAST;
        }
    }

    public static enum Orientation
    {
        DOWN(Direction.DOWN, false),
        UP(Direction.UP, false),
        NORTH(Direction.NORTH, false),
        SOUTH(Direction.SOUTH, false),
        WEST(Direction.WEST, false),
        EAST(Direction.EAST, false),
        FLIP_DOWN(Direction.DOWN, true),
        FLIP_UP(Direction.UP, true),
        FLIP_NORTH(Direction.NORTH, true),
        FLIP_SOUTH(Direction.SOUTH, true),
        FLIP_WEST(Direction.WEST, true),
        FLIP_EAST(Direction.EAST, true);

        private final int shape;

        private Orientation(Direction p_i46233_3_, boolean p_i46233_4_)
        {
            this.shape = p_i46233_3_.getId() + (p_i46233_4_ ? Direction.values().length : 0);
        }
    }

    static enum VertexTranslations
    {
        DOWN(0, 1, 2, 3),
        UP(2, 3, 0, 1),
        NORTH(3, 0, 1, 2),
        SOUTH(0, 1, 2, 3),
        WEST(3, 0, 1, 2),
        EAST(1, 2, 3, 0);

        private final int vert0;
        private final int vert1;
        private final int vert2;
        private final int vert3;
        private static final VertexTranslations[] VALUES = new VertexTranslations[6];

        private VertexTranslations(int p_i46234_3_, int p_i46234_4_, int p_i46234_5_, int p_i46234_6_)
        {
            this.vert0 = p_i46234_3_;
            this.vert1 = p_i46234_4_;
            this.vert2 = p_i46234_5_;
            this.vert3 = p_i46234_6_;
        }

        public static VertexTranslations getVertexTranslations(Direction p_178184_0_)
        {
            return VALUES[p_178184_0_.getId()];
        }

        static
        {
            VALUES[Direction.DOWN.getId()] = DOWN;
            VALUES[Direction.UP.getId()] = UP;
            VALUES[Direction.NORTH.getId()] = NORTH;
            VALUES[Direction.SOUTH.getId()] = SOUTH;
            VALUES[Direction.WEST.getId()] = WEST;
            VALUES[Direction.EAST.getId()] = EAST;
        }
    }
}
