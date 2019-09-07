package fi.dy.masa.litematica.render.schematic;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nullable;
import org.lwjgl.opengl.GL11;
import com.google.common.collect.Sets;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.chunk.WorldChunk;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.render.RenderUtils;
import fi.dy.masa.litematica.util.OverlayType;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.SubChunkPos;

public class ChunkRendererSchematicVbo
{
    public static int schematicRenderChunksUpdated;

    protected volatile WorldSchematic world;
    protected final WorldRendererSchematic worldRenderer;
    protected final ReentrantLock chunkRenderLock;
    protected final ReentrantLock chunkRenderDataLock;
    protected final Set<BlockEntity> setBlockEntities = new HashSet<>();
    protected final BlockPos.Mutable position;
    private net.minecraft.util.math.Box boundingBox;

    protected final GlBuffer[] glBufferBlocks;
    protected final GlBuffer[] glBufferOverlay;
    protected final List<IntBoundingBox> boxes = new ArrayList<>();
    protected final EnumSet<OverlayRenderType> existingOverlays = EnumSet.noneOf(OverlayRenderType.class);

    protected Color4f overlayColor;
    protected boolean hasOverlay = false;

    protected ChunkCacheSchematic schematicWorldView;
    protected ChunkCacheSchematic clientWorldView;

    protected ChunkRenderTaskSchematic compileTask;
    protected ChunkRenderDataSchematic chunkRenderData;

    private boolean needsUpdate;
    private boolean needsImmediateUpdate;

    public ChunkRendererSchematicVbo(WorldSchematic world, WorldRendererSchematic worldRenderer)
    {
        this.world = world;
        this.worldRenderer = (WorldRendererSchematic) worldRenderer;
        this.chunkRenderData = ChunkRenderDataSchematic.EMPTY;
        this.chunkRenderLock = new ReentrantLock();
        this.chunkRenderDataLock = new ReentrantLock();
        this.glBufferBlocks = new GlBuffer[BlockRenderLayer.values().length];
        this.glBufferOverlay = new GlBuffer[OverlayRenderType.values().length];
        this.position = new BlockPos.Mutable();

        for (int i = 0; i < BlockRenderLayer.values().length; ++i)
        {
            this.glBufferBlocks[i] = new GlBuffer(VertexFormats.POSITION_COLOR_UV_LMAP);
        }

        for (int i = 0; i < OverlayRenderType.values().length; ++i)
        {
            this.glBufferOverlay[i] = new GlBuffer(VertexFormats.POSITION_COLOR);
        }
    }

    public boolean hasOverlay()
    {
        return this.hasOverlay;
    }

    public EnumSet<OverlayRenderType> getOverlayTypes()
    {
        return this.existingOverlays;
    }

    public GlBuffer getBlocksGlBufferByLayer(BlockRenderLayer type)
    {
        return this.glBufferBlocks[type.ordinal()];
    }

    public GlBuffer getOverlayGlBuffer(OverlayRenderType type)
    {
        //if (GuiBase.isCtrlDown()) System.out.printf("getOverlayVertexBuffer: type: %s, buf: %s\n", type, this.vertexBufferOverlay[type.ordinal()]);
        return this.glBufferOverlay[type.ordinal()];
    }

    public ChunkRenderDataSchematic getChunkRenderData()
    {
        return this.chunkRenderData;
    }

    public void setChunkRenderData(ChunkRenderDataSchematic data)
    {
        this.chunkRenderDataLock.lock();

        try
        {
            this.chunkRenderData = data;
        }
        finally
        {
            this.chunkRenderDataLock.unlock();
        }
    }

    public BlockPos getPosition()
    {
        return this.position;
    }

    public net.minecraft.util.math.Box getBoundingBox()
    {
        if (this.boundingBox == null)
        {
            int x = this.position.getX();
            int y = this.position.getY();
            int z = this.position.getZ();
            this.boundingBox = new net.minecraft.util.math.Box(x, y, z, x + 16, y + 16, z + 16);
        }

        return this.boundingBox;
    }

    public void setPosition(int x, int y, int z)
    {
        if (x != this.position.getX() || y != this.position.getY() || z != this.position.getZ())
        {
            this.clear();
            this.position.set(x, y, z);
            this.boundingBox = new net.minecraft.util.math.Box(x, y, z, x + 16, y + 16, z + 16);
        }
    }

    protected double getDistanceSq()
    {
        PlayerEntity player = MinecraftClient.getInstance().player;

        double x = this.position.getX() + 8.0D - player.x;
        double y = this.position.getY() + 8.0D - player.y;
        double z = this.position.getZ() + 8.0D - player.z;

        return x * x + y * y + z * z;
    }

    public void deleteGlResources()
    {
        this.clear();
        this.world = null;

        for (int i = 0; i < this.glBufferBlocks.length; ++i)
        {
            if (this.glBufferBlocks[i] != null)
            {
                this.glBufferBlocks[i].delete();
            }
        }

        for (int i = 0; i < this.glBufferOverlay.length; ++i)
        {
            if (this.glBufferOverlay[i] != null)
            {
                this.glBufferOverlay[i].delete();
            }
        }
    }

    public void resortTransparency(float x, float y, float z, ChunkRenderTaskSchematic generator)
    {
        ChunkRenderDataSchematic data = (ChunkRenderDataSchematic) generator.getChunkRenderData();
        BufferBuilderCache buffers = generator.getBufferCache();
        BufferBuilder.State bufferState = data.getBlockBufferState(BlockRenderLayer.TRANSLUCENT);

        if (bufferState != null)
        {
            /*if (Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue())
            {
                RegionRenderCacheBuilder buffers = generator.getRegionRenderCacheBuilder();

                for (BlockRenderLayer layer : BlockRenderLayer.values())
                {
                    BufferBuilder buffer = buffers.getWorldRendererByLayer(layer);

                    this.preRenderBlocks(buffer, this.getPosition());
                    buffer.setVertexState(compiledChunk.getState());
                    this.postRenderBlocks(layer, x, y, z, buffer, compiledChunk);
                }
            }
            else */if (data.isBlockLayerEmpty(BlockRenderLayer.TRANSLUCENT) == false)
            {
                BufferBuilder buffer = buffers.getBlockBufferByLayer(BlockRenderLayer.TRANSLUCENT);

                this.preRenderBlocks(buffer, this.position);
                buffer.restoreState(bufferState);
                this.postRenderBlocks(BlockRenderLayer.TRANSLUCENT, x, y, z, buffer, data);
            }
        }

        //if (GuiBase.isCtrlDown()) System.out.printf("resortTransparency\n");
        if (Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY.getBooleanValue())
        {
            OverlayRenderType type = OverlayRenderType.QUAD;
            bufferState = data.getOverlayBufferState(type);

            if (bufferState != null && data.isOverlayTypeEmpty(type) == false)
            {
                BufferBuilder buffer = buffers.getOverlayBuffer(type);

                this.preRenderOverlay(buffer, type.getGlMode());
                buffer.restoreState(bufferState);
                this.postRenderOverlay(type, x, y, z, buffer, data);
            }
        }
    }

    public void rebuildChunk(float x, float y, float z, ChunkRenderTaskSchematic generator)
    {
        this.chunkRenderData = new ChunkRenderDataSchematic();
        generator.getLock().lock();

        try
        {
            if (generator.getStatus() != ChunkRenderTaskSchematic.Status.COMPILING)
            {
                return;
            }

            generator.setChunkRenderData(this.chunkRenderData);
        }
        finally
        {
            generator.getLock().unlock();
        }

        //if (GuiBase.isCtrlDown()) System.out.printf("rebuildChunk pos: %s gen: %s\n", this.getPosition(), generator);
        Set<BlockEntity> tileEntities = new HashSet<>();
        BlockPos posChunk = this.position;
        LayerRange range = DataManager.getRenderLayerRange();

        this.existingOverlays.clear();
        this.hasOverlay = false;

        synchronized (this.boxes)
        {
            if (this.boxes.isEmpty() == false &&
                (this.schematicWorldView.isEmpty() == false || this.clientWorldView.isEmpty() == false) &&
                 range.intersects(new SubChunkPos(posChunk.getX() >> 4, posChunk.getY() >> 4, posChunk.getZ() >> 4)))
            {
                ++schematicRenderChunksUpdated;

                boolean[] usedLayers = new boolean[BlockRenderLayer.values().length];
                BufferBuilderCache buffers = generator.getBufferCache();

                for (IntBoundingBox box : this.boxes)
                {
                    box = range.getClampedRenderBoundingBox(box);

                    // The rendered layer(s) don't intersect this sub-volume
                    if (box == null)
                    {
                        continue;
                    }

                    BlockPos posFrom = new BlockPos(box.minX, box.minY, box.minZ);
                    BlockPos posTo   = new BlockPos(box.maxX, box.maxY, box.maxZ);

                    for (BlockPos posMutable : BlockPos.Mutable.iterate(posFrom, posTo))
                    {
                        this.renderBlocksAndOverlay(posMutable, tileEntities, usedLayers, buffers);
                    }
                }

                for (BlockRenderLayer layerTmp : BlockRenderLayer.values())
                {
                    if (usedLayers[layerTmp.ordinal()])
                    {
                        this.chunkRenderData.setBlockLayerUsed(layerTmp);
                    }

                    if (this.chunkRenderData.isBlockLayerStarted(layerTmp))
                    {
                        this.postRenderBlocks(layerTmp, x, y, z, buffers.getBlockBufferByLayer(layerTmp), this.chunkRenderData);
                    }
                }

                if (this.hasOverlay)
                {
                    //if (GuiBase.isCtrlDown()) System.out.printf("postRenderOverlays\n");
                    for (OverlayRenderType type : this.existingOverlays)
                    {
                        if (this.chunkRenderData.isOverlayTypeStarted(type))
                        {
                            this.chunkRenderData.setOverlayTypeUsed(type);
                            this.postRenderOverlay(type, x, y, z, buffers.getOverlayBuffer(type), this.chunkRenderData);
                        }
                    }
                }
            }
        }

        this.chunkRenderLock.lock();

        try
        {
            Set<BlockEntity> set = Sets.newHashSet(tileEntities);
            Set<BlockEntity> set1 = Sets.newHashSet(this.setBlockEntities);
            set.removeAll(this.setBlockEntities);
            set1.removeAll(tileEntities);
            this.setBlockEntities.clear();
            this.setBlockEntities.addAll(tileEntities);
            this.worldRenderer.updateBlockEntities(set1, set);
        }
        finally
        {
            this.chunkRenderLock.unlock();
        }


        this.chunkRenderData.setTimeBuilt(this.world.getTime());
    }

    protected void renderBlocksAndOverlay(BlockPos pos, Set<BlockEntity> tileEntities, boolean[] usedLayers, BufferBuilderCache buffers)
    {
        BlockState stateSchematic = this.schematicWorldView.getBlockState(pos);
        BlockState stateClient    = this.clientWorldView.getBlockState(pos);
        Block blockSchematic = stateSchematic.getBlock();
        boolean clientHasAir = stateClient.isAir();
        boolean schematicHasAir = stateSchematic.isAir();
        boolean missing = false;

        if (clientHasAir && schematicHasAir)
        {
            return;
        }

        this.overlayColor = null;

        // Schematic has a block, client has air
        if (clientHasAir || (stateSchematic != stateClient && Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue()))
        {
            if (blockSchematic.hasBlockEntity())
            {
                this.addBlockEntity(pos, this.chunkRenderData, tileEntities);
            }

            boolean translucent = Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue();
            // TODO change when the fluids become separate
            FluidState fluidState = stateSchematic.getFluidState();

            if (fluidState.isEmpty() == false)
            {
                BlockRenderLayer layer = fluidState.getRenderLayer();
                int layerIndex = layer.ordinal();
                BufferBuilder bufferSchematic = buffers.getBlockBufferByLayerId(layerIndex);

                if (this.chunkRenderData.isBlockLayerStarted(layer) == false)
                {
                    this.chunkRenderData.setBlockLayerStarted(layer);
                    this.preRenderBlocks(bufferSchematic, this.position);
                }

                usedLayers[layerIndex] |= this.worldRenderer.renderFluid(this.schematicWorldView, fluidState, pos, bufferSchematic);
            }

            if (stateSchematic.getRenderType() != BlockRenderType.INVISIBLE)
            {
                BlockRenderLayer layer = translucent ? BlockRenderLayer.TRANSLUCENT : blockSchematic.getRenderLayer();
                int layerIndex = layer.ordinal();
                BufferBuilder bufferSchematic = buffers.getBlockBufferByLayerId(layerIndex);

                if (this.chunkRenderData.isBlockLayerStarted(layer) == false)
                {
                    this.chunkRenderData.setBlockLayerStarted(layer);
                    this.preRenderBlocks(bufferSchematic, this.position);
                }

                usedLayers[layerIndex] |= this.worldRenderer.renderBlock(this.schematicWorldView, stateSchematic, pos, bufferSchematic);

                if (clientHasAir)
                {
                    missing = true;
                }
            }
        }

        if (Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY.getBooleanValue())
        {
            OverlayType type = this.getOverlayType(stateSchematic, stateClient);

            this.overlayColor = this.getOverlayColor(type);

            if (this.overlayColor != null)
            {
                this.renderOverlay(type, pos, stateSchematic, missing, buffers);
            }
        }
    }

    protected void renderOverlay(OverlayType type, BlockPos pos, BlockState stateSchematic, boolean missing, BufferBuilderCache buffers)
    {
        if (Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_SIDES.getBooleanValue())
        {
            BufferBuilder bufferOverlayQuads = buffers.getOverlayBuffer(OverlayRenderType.QUAD);

            if (this.chunkRenderData.isOverlayTypeStarted(OverlayRenderType.QUAD) == false)
            {
                this.chunkRenderData.setOverlayTypeStarted(OverlayRenderType.QUAD);
                this.preRenderOverlay(bufferOverlayQuads, OverlayRenderType.QUAD);
            }

            if (Configs.Visuals.OVERLAY_REDUCED_INNER_SIDES.getBooleanValue())
            {
                BlockPos.Mutable posMutable = new BlockPos.Mutable();

                for (int i = 0; i < 6; ++i)
                {
                    Direction side = PositionUtils.FACING_ALL[i];
                    posMutable.set(pos.getX() + side.getOffsetX(), pos.getY() + side.getOffsetY(), pos.getZ() + side.getOffsetZ());
                    BlockState adjStateSchematic = this.schematicWorldView.getBlockState(posMutable);
                    BlockState adjStateClient    = this.clientWorldView.getBlockState(posMutable);

                    OverlayType typeAdj = this.getOverlayType(adjStateSchematic, adjStateClient);

                    // Only render the model-based outlines or sides for missing blocks
                    if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_SIDES.getBooleanValue())
                    {
                        BakedModel bakedModel = this.worldRenderer.getModelForState(stateSchematic);

                        if (type.getRenderPriority() > typeAdj.getRenderPriority() ||
                            Block.isFaceFullSquare(stateSchematic.getCollisionShape(this.schematicWorldView, pos), side) == false)
                        {
                            RenderUtils.drawBlockModelQuadOverlayBatched(bakedModel, stateSchematic, pos, side, this.overlayColor, 0, bufferOverlayQuads);
                        }
                    }
                    else
                    {
                        if (type.getRenderPriority() > typeAdj.getRenderPriority())
                        {
                            RenderUtils.drawBlockBoxSideBatchedQuads(pos, side, this.overlayColor, 0, bufferOverlayQuads);
                        }
                    }
                }
            }
            else
            {
                // Only render the model-based outlines or sides for missing blocks
                if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_SIDES.getBooleanValue())
                {
                    BakedModel bakedModel = this.worldRenderer.getModelForState(stateSchematic);
                    RenderUtils.drawBlockModelQuadOverlayBatched(bakedModel, stateSchematic, pos, this.overlayColor, 0, bufferOverlayQuads);
                }
                else
                {
                    fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxSidesBatchedQuads(pos, this.overlayColor, 0, bufferOverlayQuads);
                }
            }
        }

        if (Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_OUTLINES.getBooleanValue())
        {
            BufferBuilder bufferOverlayOutlines = buffers.getOverlayBuffer(OverlayRenderType.OUTLINE);

            if (this.chunkRenderData.isOverlayTypeStarted(OverlayRenderType.OUTLINE) == false)
            {
                this.chunkRenderData.setOverlayTypeStarted(OverlayRenderType.OUTLINE);
                this.preRenderOverlay(bufferOverlayOutlines, OverlayRenderType.OUTLINE);
            }

            this.overlayColor = new Color4f(this.overlayColor.r, this.overlayColor.g, this.overlayColor.b, 1f);

            if (Configs.Visuals.OVERLAY_REDUCED_INNER_SIDES.getBooleanValue())
            {
                OverlayType[][][] adjTypes = new OverlayType[3][3][3];
                BlockPos.Mutable posMutable = new BlockPos.Mutable();

                for (int y = 0; y <= 2; ++y)
                {
                    for (int z = 0; z <= 2; ++z)
                    {
                        for (int x = 0; x <= 2; ++x)
                        {
                            if (x != 1 || y != 1 || z != 1)
                            {
                                posMutable.set(pos.getX() + x - 1, pos.getY() + y - 1, pos.getZ() + z - 1);
                                BlockState adjStateSchematic = this.schematicWorldView.getBlockState(posMutable);
                                BlockState adjStateClient    = this.clientWorldView.getBlockState(posMutable);
                                adjTypes[x][y][z] = this.getOverlayType(adjStateSchematic, adjStateClient);
                            }
                            else
                            {
                                adjTypes[x][y][z] = type;
                            }
                        }
                    }
                }

                // Only render the model-based outlines or sides for missing blocks
                if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_OUTLINE.getBooleanValue())
                {
                    BakedModel bakedModel = this.worldRenderer.getModelForState(stateSchematic);

                    // FIXME: how to implement this correctly here... >_>
                    if (stateSchematic.isOpaque())
                    {
                        this.renderOverlayReducedEdges(pos, adjTypes, type, bufferOverlayOutlines);
                    }
                    else
                    {
                        RenderUtils.drawBlockModelOutlinesBatched(bakedModel, stateSchematic, pos, this.overlayColor, 0, bufferOverlayOutlines);
                    }
                }
                else
                {
                    this.renderOverlayReducedEdges(pos, adjTypes, type, bufferOverlayOutlines);
                }
            }
            else
            {
                // Only render the model-based outlines or sides for missing blocks
                if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_OUTLINE.getBooleanValue())
                {
                    BakedModel bakedModel = this.worldRenderer.getModelForState(stateSchematic);
                    RenderUtils.drawBlockModelOutlinesBatched(bakedModel, stateSchematic, pos, this.overlayColor, 0, bufferOverlayOutlines);
                }
                else
                {
                    fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(pos, this.overlayColor, 0, bufferOverlayOutlines);
                }
            }
        }
    }

    protected void renderOverlayReducedEdges(BlockPos pos, OverlayType[][][] adjTypes, OverlayType typeSelf, BufferBuilder bufferOverlayOutlines)
    {
        OverlayType[] neighborTypes = new OverlayType[4];
        Vec3i[] neighborPositions = new Vec3i[4];
        int lines = 0;

        for (Direction.Axis axis : PositionUtils.AXES_ALL)
        {
            for (int corner = 0; corner < 4; ++corner)
            {
                Vec3i[] offsets = PositionUtils.getEdgeNeighborOffsets(axis, corner);
                int index = -1;
                boolean hasCurrent = false;

                // Find the position(s) around a given edge line that have the shared greatest rendering priority
                for (int i = 0; i < 4; ++i)
                {
                    Vec3i offset = offsets[i];
                    OverlayType type = adjTypes[offset.getX() + 1][offset.getY() + 1][offset.getZ() + 1];

                    // type NONE
                    if (type == OverlayType.NONE)
                    {
                        continue;
                    }

                    // First entry, or sharing at least the current highest found priority
                    if (index == -1 || type.getRenderPriority() >= neighborTypes[index - 1].getRenderPriority())
                    {
                        // Actually a new highest priority, add it as the first entry and rewind the index
                        if (index < 0 || type.getRenderPriority() > neighborTypes[index - 1].getRenderPriority())
                        {
                            index = 0;
                        }
                        // else: Same priority as a previous entry, append this position

                        //System.out.printf("plop 0 axis: %s, corner: %d, i: %d, index: %d, type: %s\n", axis, corner, i, index, type);
                        neighborPositions[index] = new Vec3i(pos.getX() + offset.getX(), pos.getY() + offset.getY(), pos.getZ() + offset.getZ());
                        neighborTypes[index] = type;
                        // The self position is the first (offset = [0, 0, 0]) in the arrays
                        hasCurrent |= (i == 0);
                        ++index;
                    }
                }

                //System.out.printf("plop 1 index: %d, pos: %s\n", index, pos);
                // Found something to render, and the current block is among the highest priority for this edge
                if (index > 0 && hasCurrent)
                {
                    Vec3i posTmp = new Vec3i(pos.getX(), pos.getY(), pos.getZ());
                    int ind = -1;

                    for (int i = 0; i < index; ++i)
                    {
                        Vec3i tmp = neighborPositions[i];
                        //System.out.printf("posTmp: %s, tmp: %s\n", posTmp, tmp);

                        // Just prioritize the position to render a shared highest priority edge by the coordinates
                        if (tmp.getX() <= posTmp.getX() && tmp.getY() <= posTmp.getY() && tmp.getZ() <= posTmp.getZ())
                        {
                            posTmp = tmp;
                            ind = i;
                        }
                    }

                    // The current position is the one that should render this edge
                    if (posTmp.getX() == pos.getX() && posTmp.getY() == pos.getY() && posTmp.getZ() == pos.getZ())
                    {
                        //System.out.printf("plop 2 index: %d, ind: %d, pos: %s, off: %s\n", index, ind, pos, posTmp);
                        RenderUtils.drawBlockBoxEdgeBatchedLines(pos, axis, corner, this.overlayColor, bufferOverlayOutlines);
                        lines++;
                    }
                }
            }
        }
        //System.out.printf("typeSelf: %s, pos: %s, lines: %d\n", typeSelf, pos, lines);
    }

    protected OverlayType getOverlayType(BlockState stateSchematic, BlockState stateClient)
    {
        if (stateSchematic == stateClient)
        {
            return OverlayType.NONE;
        }
        else
        {
            boolean clientHasAir = stateClient.isAir();
            boolean schematicHasAir = stateSchematic.isAir();

            if (schematicHasAir)
            {
                return clientHasAir ? OverlayType.NONE : OverlayType.EXTRA;
            }
            else
            {
                if (clientHasAir)
                {
                    return OverlayType.MISSING;
                }
                // Wrong block
                else if (stateSchematic.getBlock() != stateClient.getBlock())
                {
                    return OverlayType.WRONG_BLOCK;
                }
                // Wrong state
                else
                {
                    return OverlayType.WRONG_STATE;
                }
            }
        }
    }

    @Nullable
    protected Color4f getOverlayColor(OverlayType overlayType)
    {
        Color4f overlayColor = null;

        switch (overlayType)
        {
            case MISSING:
                if (Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_MISSING.getBooleanValue())
                {
                    overlayColor = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_MISSING.getColor();
                }
                break;
            case EXTRA:
                if (Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_EXTRA.getBooleanValue())
                {
                    overlayColor = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_EXTRA.getColor();
                }
                break;
            case WRONG_BLOCK:
                if (Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_WRONG_BLOCK.getBooleanValue())
                {
                    overlayColor = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_WRONG_BLOCK.getColor();
                }
                break;
            case WRONG_STATE:
                if (Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_WRONG_STATE.getBooleanValue())
                {
                    overlayColor = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_WRONG_STATE.getColor();
                }
                break;
            default:
        }

        return overlayColor;
    }

    private void addBlockEntity(BlockPos pos, ChunkRenderDataSchematic chunkRenderData, Set<BlockEntity> blockEntities)
    {
        BlockEntity te = this.schematicWorldView.getBlockEntity(pos, WorldChunk.CreationType.CHECK);

        if (te != null)
        {
            BlockEntityRenderer<BlockEntity> tesr = BlockEntityRenderDispatcher.INSTANCE.get(te);

            if (tesr != null)
            {
                chunkRenderData.addBlockEntity(te);

                if (tesr.method_3563(te)) // isGlobalRenderer
                {
                    blockEntities.add(te);
                }
            }
        }
    }

    private void preRenderBlocks(BufferBuilder buffer, BlockPos pos)
    {
        buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR_UV_LMAP);
        buffer.setOffset(-pos.getX(), -pos.getY(), -pos.getZ());
    }

    private void postRenderBlocks(BlockRenderLayer layer, float x, float y, float z, BufferBuilder buffer, ChunkRenderDataSchematic chunkRenderData)
    {
        if (layer == BlockRenderLayer.TRANSLUCENT && chunkRenderData.isBlockLayerEmpty(layer) == false)
        {
            buffer.sortQuads(x, y, z);
            chunkRenderData.setBlockBufferState(layer, buffer.toBufferState());
        }

        buffer.end();
    }

    private void preRenderOverlay(BufferBuilder buffer, OverlayRenderType type)
    {
        this.existingOverlays.add(type);
        this.hasOverlay = true;

        BlockPos pos = this.position;
        buffer.begin(type.getGlMode(), VertexFormats.POSITION_COLOR);
        buffer.setOffset(-pos.getX(), -pos.getY(), -pos.getZ());
    }

    private void preRenderOverlay(BufferBuilder buffer, int glMode)
    {
        BlockPos pos = this.position;
        buffer.begin(glMode, VertexFormats.POSITION_COLOR);
        buffer.setOffset(-pos.getX(), -pos.getY(), -pos.getZ());
    }

    private void postRenderOverlay(OverlayRenderType type, float x, float y, float z, BufferBuilder buffer, ChunkRenderDataSchematic chunkRenderData)
    {
        if (type == OverlayRenderType.QUAD && chunkRenderData.isOverlayTypeEmpty(type) == false)
        {
            buffer.sortQuads(x, y, z);
            chunkRenderData.setOverlayBufferState(type, buffer.toBufferState());
        }

        buffer.end();
    }

    public ChunkRenderTaskSchematic makeCompileTaskChunkSchematic()
    {
        this.chunkRenderLock.lock();
        ChunkRenderTaskSchematic generator = null;

        try
        {
            //if (GuiBase.isCtrlDown()) System.out.printf("makeCompileTaskChunk()\n");
            this.finishCompileTask();
            this.rebuildWorldView();
            this.compileTask = new ChunkRenderTaskSchematic(this, ChunkRenderTaskSchematic.Type.REBUILD_CHUNK, this.getDistanceSq());
            generator = this.compileTask;
        }
        finally
        {
            this.chunkRenderLock.unlock();
        }

        return generator;
    }

    @Nullable
    public ChunkRenderTaskSchematic makeCompileTaskTransparencySchematic()
    {
        this.chunkRenderLock.lock();

        try
        {
            if (this.compileTask == null || this.compileTask.getStatus() != ChunkRenderTaskSchematic.Status.PENDING)
            {
                if (this.compileTask != null && this.compileTask.getStatus() != ChunkRenderTaskSchematic.Status.DONE)
                {
                    this.compileTask.finish();
                }

                this.compileTask = new ChunkRenderTaskSchematic(this, ChunkRenderTaskSchematic.Type.RESORT_TRANSPARENCY, this.getDistanceSq());
                this.compileTask.setChunkRenderData(this.chunkRenderData);

                return this.compileTask;
            }
        }
        finally
        {
            this.chunkRenderLock.unlock();
        }

        return null;
    }

    protected void finishCompileTask()
    {
        this.chunkRenderLock.lock();

        try
        {
            if (this.compileTask != null && this.compileTask.getStatus() != ChunkRenderTaskSchematic.Status.DONE)
            {
                this.compileTask.finish();
                this.compileTask = null;
            }
        }
        finally
        {
            this.chunkRenderLock.unlock();
        }
    }

    public ReentrantLock getLockCompileTask()
    {
        return this.chunkRenderLock;
    }

    public void clear()
    {
        this.finishCompileTask();
        this.chunkRenderData = ChunkRenderDataSchematic.EMPTY;
        this.needsUpdate = true;
    }

    public void setNeedsUpdate(boolean immediate)
    {
        if (this.needsUpdate)
        {
            immediate |= this.needsImmediateUpdate;
        }

        this.needsUpdate = true;
        this.needsImmediateUpdate = immediate;
    }

    public void clearNeedsUpdate()
    {
        this.needsUpdate = false;
        this.needsImmediateUpdate = false;
    }

    public boolean needsUpdate()
    {
        return this.needsUpdate;
    }

    public boolean needsImmediateUpdate()
    {
        return this.needsUpdate && this.needsImmediateUpdate;
    }

    private void rebuildWorldView()
    {
        synchronized (this.boxes)
        {
            this.schematicWorldView = new ChunkCacheSchematic(this.world, this.position, 2);
            this.clientWorldView    = new ChunkCacheSchematic(MinecraftClient.getInstance().world, this.position, 2);

            BlockPos pos = this.position;
            SubChunkPos subChunk = new SubChunkPos(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
            this.boxes.clear();
            this.boxes.addAll(DataManager.getSchematicPlacementManager().getTouchedBoxesInSubChunk(subChunk));
        }
    }

    public enum OverlayRenderType
    {
        OUTLINE     (GL11.GL_LINES),
        QUAD        (GL11.GL_QUADS);

        private final int glMode;

        private OverlayRenderType(int glMode)
        {
            this.glMode = glMode;
        }

        public int getGlMode()
        {
            return this.glMode;
        }
    }
}
