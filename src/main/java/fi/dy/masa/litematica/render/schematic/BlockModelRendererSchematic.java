package fi.dy.masa.litematica.render.schematic;

import java.util.BitSet;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockRenderView;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;

public class BlockModelRendererSchematic
{
    private final Random random = new Random();
    private final BlockColors colorMap;

    public BlockModelRendererSchematic(BlockColors blockColorsIn)
    {
        this.colorMap = blockColorsIn;
    }

    public boolean renderModel(BlockRenderView worldIn, BakedModel modelIn, BlockState stateIn, BlockPos posIn, MatrixStack matrices,
            VertexConsumer vertexConsumer, long rand)
    {
        boolean ao = MinecraftClient.isAmbientOcclusionEnabled() && stateIn.getLuminance() == 0 && modelIn.useAmbientOcclusion();

        Vec3d offset = stateIn.getModelOffset(worldIn, posIn);
        matrices.translate(offset.x, offset.y, offset.z);
        int overlay = OverlayTexture.DEFAULT_UV;

        try
        {
            if (ao)
            {
                return this.renderModelSmooth(worldIn, modelIn, stateIn, posIn, matrices, vertexConsumer, this.random, rand, overlay);
            }
            else
            {
                return this.renderModelFlat(worldIn, modelIn, stateIn, posIn, matrices, vertexConsumer, this.random, rand, overlay);
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

    public boolean renderModelSmooth(BlockRenderView worldIn, BakedModel modelIn, BlockState stateIn, BlockPos posIn, MatrixStack matrices,
            VertexConsumer vertexConsumer, Random random, long seedIn, int overlay)
    {
        boolean renderedSomething = false;
        float[] quadBounds = new float[Direction.values().length * 2];
        BitSet bitset = new BitSet(3);
        AmbientOcclusionCalculator aoFace = new AmbientOcclusionCalculator();

        for (Direction side : Direction.values())
        {
            random.setSeed(seedIn);
            List<BakedQuad> quads = modelIn.getQuads(stateIn, side, random);

            if (quads.isEmpty() == false)
            {
                if (this.shouldRenderModelSide(worldIn, stateIn, posIn, side))
                {
                    this.renderQuadsSmooth(worldIn, stateIn, posIn, matrices, vertexConsumer, quads, quadBounds, bitset, aoFace, overlay);
                    renderedSomething = true;
                }
            }
        }

        random.setSeed(seedIn);
        List<BakedQuad> quads = modelIn.getQuads(stateIn, (Direction) null, random);

        if (quads.isEmpty() == false)
        {
            this.renderQuadsSmooth(worldIn, stateIn, posIn, matrices, vertexConsumer, quads, quadBounds, bitset, aoFace, overlay);
            renderedSomething = true;
        }

        return renderedSomething;
    }

    public boolean renderModelFlat(BlockRenderView worldIn, BakedModel modelIn, BlockState stateIn, BlockPos posIn, MatrixStack matrices,
            VertexConsumer vertexConsumer, Random random, long seedIn, int overlay)
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
                    int light = WorldRenderer.getLightmapCoordinates(worldIn, stateIn, posIn.offset(side));
                    this.renderQuadsFlat(worldIn, stateIn, posIn, light, overlay, false, matrices, vertexConsumer, quads, bitset);
                    renderedSomething = true;
                }
            }
        }

        random.setSeed(seedIn);
        List<BakedQuad> quads = modelIn.getQuads(stateIn, null, random);

        if (quads.isEmpty() == false)
        {
            this.renderQuadsFlat(worldIn, stateIn, posIn, -1, overlay, true, matrices, vertexConsumer, quads, bitset);
            renderedSomething = true;
        }

        return renderedSomething;
    }

    private boolean shouldRenderModelSide(BlockRenderView worldIn, BlockState stateIn, BlockPos posIn, Direction side)
    {
        return DataManager.getRenderLayerRange().isPositionAtRenderEdgeOnSide(posIn, side) ||
               (Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue() && Configs.Visuals.RENDER_TRANSLUCENT_INNER_SIDES.getBooleanValue()) ||
               Block.shouldDrawSide(stateIn, worldIn, posIn, side);
    }

    private void renderQuadsSmooth(BlockRenderView world, BlockState state, BlockPos pos, MatrixStack matrices,
            VertexConsumer vertexConsumer, List<BakedQuad> list, float[] box, BitSet flags, AmbientOcclusionCalculator ambientOcclusionCalculator, int overlay)
    {
        final int size = list.size();

        for (int i = 0; i < size; ++i)
        {
            BakedQuad bakedQuad = list.get(i);

            this.getQuadDimensions(world, state, pos, bakedQuad.getVertexData(), bakedQuad.getFace(), box, flags);
            ambientOcclusionCalculator.apply(world, state, pos, bakedQuad.getFace(), box, flags, bakedQuad.hasShade());

            this.renderQuad(world, state, pos, vertexConsumer, matrices.peek(), bakedQuad,
                    ambientOcclusionCalculator.brightness[0],
                    ambientOcclusionCalculator.brightness[1],
                    ambientOcclusionCalculator.brightness[2],
                    ambientOcclusionCalculator.brightness[3],
                    ambientOcclusionCalculator.light[0],
                    ambientOcclusionCalculator.light[1],
                    ambientOcclusionCalculator.light[2],
                    ambientOcclusionCalculator.light[3], overlay);
        }
    }

    private void renderQuadsFlat(BlockRenderView world, BlockState state, BlockPos pos,
            int light, int overlay, boolean useWorldLight, MatrixStack matrices, VertexConsumer vertexConsumer, List<BakedQuad> list, BitSet flags)
    {
        final int size = list.size();

        for (int i = 0; i < size; ++i)
        {
            BakedQuad bakedQuad = list.get(i);

            if (useWorldLight)
            {
                this.getQuadDimensions(world, state, pos, bakedQuad.getVertexData(), bakedQuad.getFace(), null, flags);
                BlockPos blockPos = flags.get(0) ? pos.offset(bakedQuad.getFace()) : pos;
                light = WorldRenderer.getLightmapCoordinates(world, state, blockPos);
            }

            this.renderQuad(world, state, pos, vertexConsumer, matrices.peek(), bakedQuad, 1.0F, 1.0F, 1.0F, 1.0F, light, light, light, light, overlay);
        }
    }

    private void renderQuad(BlockRenderView world, BlockState state, BlockPos pos, VertexConsumer vertexConsumer, MatrixStack.Entry matrixEntry,
            BakedQuad quad, float brightness0, float brightness1, float brightness2, float brightness3,
            int light0, int light1, int light2, int light3, int overlay)
    {
        float r;
        float g;
        float b;

        if (quad.hasColor())
        {
            int color = this.colorMap.getColor(state, world, pos, quad.getColorIndex());
            r = (float) (color >> 16 & 0xFF) / 255.0F;
            g = (float) (color >>  8 & 0xFF) / 255.0F;
            b = (float) (color       & 0xFF) / 255.0F;
        }
        else
        {
            r = 1.0F;
            g = 1.0F;
            b = 1.0F;
        }

        vertexConsumer.quad(matrixEntry, quad, new float[]{ brightness0, brightness1, brightness2, brightness3 },
                            r, g, b, new int[]{ light0, light1, light2, light3 }, overlay, true);
    }

    private void getQuadDimensions(BlockRenderView world, BlockState state, BlockPos pos, int[] vertexData, Direction face, @Nullable float[] box, BitSet flags)
    {
        float minX = 32.0F;
        float minY = 32.0F;
        float minZ = 32.0F;
        float maxX = -32.0F;
        float maxY = -32.0F;
        float maxZ = -32.0F;
        final int vertexSize = vertexData.length / 4;

        for (int index = 0; index < 4; ++index)
        {
            float x = Float.intBitsToFloat(vertexData[index * vertexSize    ]);
            float y = Float.intBitsToFloat(vertexData[index * vertexSize + 1]);
            float z = Float.intBitsToFloat(vertexData[index * vertexSize + 2]);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        if (box != null)
        {
            box[Direction.WEST.getId()]  = minX;
            box[Direction.EAST.getId()]  = maxX;
            box[Direction.DOWN.getId()]  = minY;
            box[Direction.UP.getId()]    = maxY;
            box[Direction.NORTH.getId()] = minZ;
            box[Direction.SOUTH.getId()] = maxZ;

            box[Direction.WEST.getId()  + 6] = 1.0F - minX;
            box[Direction.EAST.getId()  + 6] = 1.0F - maxX;
            box[Direction.DOWN.getId()  + 6] = 1.0F - minY;
            box[Direction.UP.getId()    + 6] = 1.0F - maxY;
            box[Direction.NORTH.getId() + 6] = 1.0F - minZ;
            box[Direction.SOUTH.getId() + 6] = 1.0F - maxZ;
        }

        float min = 1.0E-4F;
        float max = 0.9999F;

        switch (face)
        {
            case DOWN:
                flags.set(1, minX >= min || minZ >= min || maxX <= max || maxZ <= max);
                flags.set(0, minY == maxY && (minY < min || state.isFullCube(world, pos)));
                break;
            case UP:
                flags.set(1, minX >= min || minZ >= min || maxX <= max || maxZ <= max);
                flags.set(0, minY == maxY && (maxY > max || state.isFullCube(world, pos)));
                break;
            case NORTH:
                flags.set(1, minX >= min || minY >= min || maxX <= max || maxY <= max);
                flags.set(0, minZ == maxZ && (minZ < min || state.isFullCube(world, pos)));
                break;
            case SOUTH:
                flags.set(1, minX >= min || minY >= min || maxX <= max || maxY <= max);
                flags.set(0, minZ == maxZ && (maxZ > max || state.isFullCube(world, pos)));
                break;
            case WEST:
                flags.set(1, minY >= min || minZ >= min || maxY <= max || maxZ <= max);
                flags.set(0, minX == maxX && (minX < min || state.isFullCube(world, pos)));
                break;
            case EAST:
                flags.set(1, minY >= min || minZ >= min || maxY <= max || maxZ <= max);
                flags.set(0, minX == maxX && (maxX > max || state.isFullCube(world, pos)));
        }
    }

    /*
    private void fillQuadBounds(BlockRenderView world, BlockState stateIn, BlockPos pos, int[] vertexData, Direction face, @Nullable float[] quadBounds, BitSet boundsFlags)
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
    */

    class AmbientOcclusionCalculator
    {
        private final float[] brightness = new float[4];
        private final int[] light = new int[4];

        public void apply(BlockRenderView world, BlockState state, BlockPos pos, Direction direction, float[] box, BitSet shapeState, boolean hasShade)
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
            int i, j, k, l, i1, i3, j1, k1, l1;
            i = j = k = l = i1 = i3 = j1 = k1 = l1 = ((15 << 20) | (15 << 4));
            float b1 = 1.0F;
            float b2 = 1.0F;
            float b3 = 1.0F;
            float b4 = 1.0F;

            if (shapeState.get(1) && neighborInfo.doNonCubicWeight)
            {
                float f13 = box[neighborInfo.vert0Weights[0].shape] * box[neighborInfo.vert0Weights[1].shape];
                float f14 = box[neighborInfo.vert0Weights[2].shape] * box[neighborInfo.vert0Weights[3].shape];
                float f15 = box[neighborInfo.vert0Weights[4].shape] * box[neighborInfo.vert0Weights[5].shape];
                float f16 = box[neighborInfo.vert0Weights[6].shape] * box[neighborInfo.vert0Weights[7].shape];
                float f17 = box[neighborInfo.vert1Weights[0].shape] * box[neighborInfo.vert1Weights[1].shape];
                float f18 = box[neighborInfo.vert1Weights[2].shape] * box[neighborInfo.vert1Weights[3].shape];
                float f19 = box[neighborInfo.vert1Weights[4].shape] * box[neighborInfo.vert1Weights[5].shape];
                float f20 = box[neighborInfo.vert1Weights[6].shape] * box[neighborInfo.vert1Weights[7].shape];
                float f21 = box[neighborInfo.vert2Weights[0].shape] * box[neighborInfo.vert2Weights[1].shape];
                float f22 = box[neighborInfo.vert2Weights[2].shape] * box[neighborInfo.vert2Weights[3].shape];
                float f23 = box[neighborInfo.vert2Weights[4].shape] * box[neighborInfo.vert2Weights[5].shape];
                float f24 = box[neighborInfo.vert2Weights[6].shape] * box[neighborInfo.vert2Weights[7].shape];
                float f25 = box[neighborInfo.vert3Weights[0].shape] * box[neighborInfo.vert3Weights[1].shape];
                float f26 = box[neighborInfo.vert3Weights[2].shape] * box[neighborInfo.vert3Weights[3].shape];
                float f27 = box[neighborInfo.vert3Weights[4].shape] * box[neighborInfo.vert3Weights[5].shape];
                float f28 = box[neighborInfo.vert3Weights[6].shape] * box[neighborInfo.vert3Weights[7].shape];
                this.brightness[vertexTranslations.vert0] = b1 * f13 + b2 * f14 + b3 * f15 + b4 * f16;
                this.brightness[vertexTranslations.vert1] = b1 * f17 + b2 * f18 + b3 * f19 + b4 * f20;
                this.brightness[vertexTranslations.vert2] = b1 * f21 + b2 * f22 + b3 * f23 + b4 * f24;
                this.brightness[vertexTranslations.vert3] = b1 * f25 + b2 * f26 + b3 * f27 + b4 * f28;
                int i2 = this.getAoBrightness(l, i, j1, i3);
                int j2 = this.getAoBrightness(k, i, i1, i3);
                int k2 = this.getAoBrightness(k, j, k1, i3);
                int l2 = this.getAoBrightness(l, j, l1, i3);
                this.light[vertexTranslations.vert0] = this.getVertexBrightness(i2, j2, k2, l2, f13, f14, f15, f16);
                this.light[vertexTranslations.vert1] = this.getVertexBrightness(i2, j2, k2, l2, f17, f18, f19, f20);
                this.light[vertexTranslations.vert2] = this.getVertexBrightness(i2, j2, k2, l2, f21, f22, f23, f24);
                this.light[vertexTranslations.vert3] = this.getVertexBrightness(i2, j2, k2, l2, f25, f26, f27, f28);
            }
            else
            {
                this.light[vertexTranslations.vert0] = this.getAoBrightness(l, i, j1, i3);
                this.light[vertexTranslations.vert1] = this.getAoBrightness(k, i, i1, i3);
                this.light[vertexTranslations.vert2] = this.getAoBrightness(k, j, k1, i3);
                this.light[vertexTranslations.vert3] = this.getAoBrightness(l, j, l1, i3);
                this.brightness[vertexTranslations.vert0] = b1;
                this.brightness[vertexTranslations.vert1] = b2;
                this.brightness[vertexTranslations.vert2] = b3;
                this.brightness[vertexTranslations.vert3] = b4;
            }

            float b = world.getBrightness(direction, hasShade);

            for (int index = 0; index < this.brightness.length; ++index)
            {
                this.brightness[index] *= b;
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
