package fi.dy.masa.litematica.render.schematic;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.lwjgl.opengl.GL11;
import com.google.common.collect.Sets;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.render.RenderUtils;
import fi.dy.masa.litematica.util.OverlayType;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.SubChunkPos;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.fluid.IFluidState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class RenderChunkSchematicVbo extends RenderChunk
{
    public static int schematicRenderChunksUpdated;

    private final WorldRendererSchematic worldRenderer;
    private final VertexBuffer[] vertexBufferOverlay = new VertexBuffer[OverlayRenderType.values().length];
    private final Set<TileEntity> setTileEntities = new HashSet<>();
    private final List<IntBoundingBox> boxes = new ArrayList<>();
    private final EnumSet<OverlayRenderType> existingOverlays = EnumSet.noneOf(OverlayRenderType.class);
    private boolean hasOverlay = false;
    private ChunkRenderTaskSchematic compileTask;

    private ChunkCacheSchematic schematicWorldView;
    private ChunkCacheSchematic clientWorldView;

    private CompiledChunkSchematic compiledChunk;
    private Color4f overlayColor;

    public RenderChunkSchematicVbo(World worldIn, WorldRenderer renderGlobalIn)
    {
        super(worldIn, renderGlobalIn);

        this.worldRenderer = (WorldRendererSchematic) renderGlobalIn;

        if (OpenGlHelper.useVbo())
        {
            for (int i = 0; i < OverlayRenderType.values().length; ++i)
            {
                this.vertexBufferOverlay[i] = new VertexBuffer(DefaultVertexFormats.POSITION_COLOR);
            }
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

    public VertexBuffer getOverlayVertexBuffer(OverlayRenderType type)
    {
        //if (GuiBase.isCtrlDown()) System.out.printf("getOverlayVertexBuffer: type: %s, buf: %s\n", type, this.vertexBufferOverlay[type.ordinal()]);
        return this.vertexBufferOverlay[type.ordinal()];
    }

    @Override
    public void deleteGlResources()
    {
        super.deleteGlResources();

        for (int i = 0; i < this.vertexBufferOverlay.length; ++i)
        {
            if (this.vertexBufferOverlay[i] != null)
            {
                this.vertexBufferOverlay[i].deleteGlBuffers();
            }
        }
    }

    public void resortTransparency(float x, float y, float z, ChunkRenderTaskSchematic generator)
    {
        CompiledChunkSchematic compiledChunk = (CompiledChunkSchematic) generator.getCompiledChunk();
        BufferBuilderCache buffers = generator.getBufferCache();
        BufferBuilder.State bufferState = compiledChunk.getBlockBufferState(BlockRenderLayer.TRANSLUCENT);

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
            else */if (compiledChunk.isLayerEmpty(BlockRenderLayer.TRANSLUCENT) == false)
            {
                BufferBuilder buffer = buffers.getWorldRendererByLayer(BlockRenderLayer.TRANSLUCENT);

                this.preRenderBlocks(buffer, this.getPosition());
                buffer.setVertexState(bufferState);
                this.postRenderBlocks(BlockRenderLayer.TRANSLUCENT, x, y, z, buffer, compiledChunk);
            }
        }

        //if (GuiBase.isCtrlDown()) System.out.printf("resortTransparency\n");
        if (Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY.getBooleanValue())
        {
            OverlayRenderType type = OverlayRenderType.QUAD;
            bufferState = compiledChunk.getOverlayBufferState(type);

            if (bufferState != null && compiledChunk.isOverlayTypeEmpty(type) == false)
            {
                BufferBuilder buffer = buffers.getOverlayBuffer(type);

                this.preRenderOverlay(buffer, type.getGlMode());
                buffer.setVertexState(bufferState);
                this.postRenderOverlay(type, x, y, z, buffer, compiledChunk);
            }
        }
    }

    public void rebuildChunk(float x, float y, float z, ChunkRenderTaskSchematic generator)
    {
        this.compiledChunk = new CompiledChunkSchematic();
        generator.getLock().lock();

        try
        {
            if (generator.getStatus() != ChunkRenderTaskSchematic.Status.COMPILING)
            {
                return;
            }

            generator.setCompiledChunk(this.compiledChunk);
        }
        finally
        {
            generator.getLock().unlock();
        }

        //if (GuiBase.isCtrlDown()) System.out.printf("rebuildChunk pos: %s gen: %s\n", this.getPosition(), generator);
        Set<TileEntity> tileEntities = new HashSet<>();
        BlockPos posChunk = this.getPosition();
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

                    for (BlockPos.MutableBlockPos posMutable : BlockPos.getAllInBoxMutable(posFrom, posTo))
                    {
                        this.renderBlocksAndOverlay(posMutable, tileEntities, usedLayers, buffers);
                    }
                }

                for (BlockRenderLayer layerTmp : BlockRenderLayer.values())
                {
                    if (usedLayers[layerTmp.ordinal()])
                    {
                        this.compiledChunk.setLayerUsed(layerTmp);
                    }

                    if (this.compiledChunk.isLayerStarted(layerTmp))
                    {
                        this.postRenderBlocks(layerTmp, x, y, z, buffers.getWorldRendererByLayer(layerTmp), this.compiledChunk);
                    }
                }

                if (this.hasOverlay)
                {
                    //if (GuiBase.isCtrlDown()) System.out.printf("postRenderOverlays\n");
                    for (OverlayRenderType type : this.existingOverlays)
                    {
                        if (this.compiledChunk.isOverlayTypeStarted(type))
                        {
                            this.compiledChunk.setOverlayTypeUsed(type);
                            this.postRenderOverlay(type, x, y, z, buffers.getOverlayBuffer(type), this.compiledChunk);
                        }
                    }
                }
            }
        }

        this.getLockCompileTask().lock();

        try
        {
            Set<TileEntity> set = Sets.newHashSet(tileEntities);
            Set<TileEntity> set1 = Sets.newHashSet(this.setTileEntities);
            set.removeAll(this.setTileEntities);
            set1.removeAll(tileEntities);
            this.setTileEntities.clear();
            this.setTileEntities.addAll(tileEntities);
            this.worldRenderer.updateTileEntities(set1, set);
        }
        finally
        {
            this.getLockCompileTask().unlock();
        }


        this.compiledChunk.setTimeBuilt(this.getWorld().getGameTime());
    }

    protected void renderBlocksAndOverlay(BlockPos pos, Set<TileEntity> tileEntities, boolean[] usedLayers, BufferBuilderCache buffers)
    {
        IBlockState stateSchematic = this.schematicWorldView.getBlockState(pos);
        IBlockState stateClient    = this.clientWorldView.getBlockState(pos);
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
            if (blockSchematic.hasTileEntity())
            {
                this.addTileEntity(pos, this.compiledChunk, tileEntities);
            }

            boolean translucent = Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue();
            // TODO change when the fluids become separate
            IFluidState fluidState = stateSchematic.getFluidState();

            if (fluidState.isEmpty() == false)
            {
                BlockRenderLayer layer = fluidState.getRenderLayer();
                int layerIndex = layer.ordinal();
                BufferBuilder bufferSchematic = buffers.getWorldRendererByLayerId(layerIndex);

                if (this.compiledChunk.isLayerStarted(layer) == false)
                {
                    this.compiledChunk.setLayerStarted(layer);
                    this.preRenderBlocks(bufferSchematic, this.getPosition());
                }

                usedLayers[layerIndex] |= this.worldRenderer.renderFluid(fluidState, pos, this.schematicWorldView, bufferSchematic);
            }

            if (stateSchematic.getRenderType() != EnumBlockRenderType.INVISIBLE)
            {
                BlockRenderLayer layer = translucent ? BlockRenderLayer.TRANSLUCENT : blockSchematic.getRenderLayer();
                int layerIndex = layer.ordinal();
                BufferBuilder bufferSchematic = buffers.getWorldRendererByLayerId(layerIndex);

                if (this.compiledChunk.isLayerStarted(layer) == false)
                {
                    this.compiledChunk.setLayerStarted(layer);
                    this.preRenderBlocks(bufferSchematic, this.getPosition());
                }

                usedLayers[layerIndex] |= this.worldRenderer.renderBlock(stateSchematic, pos, this.schematicWorldView, bufferSchematic);

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

    protected void renderOverlay(OverlayType type, BlockPos pos, IBlockState stateSchematic, boolean missing, BufferBuilderCache buffers)
    {
        if (Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_SIDES.getBooleanValue())
        {
            BufferBuilder bufferOverlayQuads = buffers.getOverlayBuffer(OverlayRenderType.QUAD);

            if (this.compiledChunk.isOverlayTypeStarted(OverlayRenderType.QUAD) == false)
            {
                this.compiledChunk.setOverlayTypeStarted(OverlayRenderType.QUAD);
                this.preRenderOverlay(bufferOverlayQuads, OverlayRenderType.QUAD);
            }

            if (Configs.Visuals.OVERLAY_REDUCED_INNER_SIDES.getBooleanValue())
            {
                BlockPos.PooledMutableBlockPos posMutable = BlockPos.PooledMutableBlockPos.retain();

                for (int i = 0; i < 6; ++i)
                {
                    EnumFacing side = PositionUtils.FACING_ALL[i];
                    posMutable.setPos(pos.getX() + side.getXOffset(), pos.getY() + side.getYOffset(), pos.getZ() + side.getZOffset());
                    IBlockState adjStateSchematic = this.schematicWorldView.getBlockState(posMutable);
                    IBlockState adjStateClient    = this.clientWorldView.getBlockState(posMutable);

                    OverlayType typeAdj = this.getOverlayType(adjStateSchematic, adjStateClient);

                    // Only render the model-based outlines or sides for missing blocks
                    if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_SIDES.getBooleanValue())
                    {
                        IBakedModel bakedModel = this.worldRenderer.getModelForState(stateSchematic);

                        if (type.getRenderPriority() > typeAdj.getRenderPriority() ||
                            stateSchematic.getBlockFaceShape(this.schematicWorldView, pos, side) != BlockFaceShape.SOLID)
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

                posMutable.close();
            }
            else
            {
                // Only render the model-based outlines or sides for missing blocks
                if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_SIDES.getBooleanValue())
                {
                    IBakedModel bakedModel = this.worldRenderer.getModelForState(stateSchematic);
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

            if (this.compiledChunk.isOverlayTypeStarted(OverlayRenderType.OUTLINE) == false)
            {
                this.compiledChunk.setOverlayTypeStarted(OverlayRenderType.OUTLINE);
                this.preRenderOverlay(bufferOverlayOutlines, OverlayRenderType.OUTLINE);
            }

            this.overlayColor = new Color4f(this.overlayColor.r, this.overlayColor.g, this.overlayColor.b, 1f);

            if (Configs.Visuals.OVERLAY_REDUCED_INNER_SIDES.getBooleanValue())
            {
                OverlayType[][][] adjTypes = new OverlayType[3][3][3];
                BlockPos.PooledMutableBlockPos posMutable = BlockPos.PooledMutableBlockPos.retain();

                for (int y = 0; y <= 2; ++y)
                {
                    for (int z = 0; z <= 2; ++z)
                    {
                        for (int x = 0; x <= 2; ++x)
                        {
                            if (x != 1 || y != 1 || z != 1)
                            {
                                posMutable.setPos(pos.getX() + x - 1, pos.getY() + y - 1, pos.getZ() + z - 1);
                                IBlockState adjStateSchematic = this.schematicWorldView.getBlockState(posMutable);
                                IBlockState adjStateClient    = this.clientWorldView.getBlockState(posMutable);
                                adjTypes[x][y][z] = this.getOverlayType(adjStateSchematic, adjStateClient);
                            }
                            else
                            {
                                adjTypes[x][y][z] = type;
                            }
                        }
                    }
                }

                posMutable.close();

                // Only render the model-based outlines or sides for missing blocks
                if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_OUTLINE.getBooleanValue())
                {
                    IBakedModel bakedModel = this.worldRenderer.getModelForState(stateSchematic);

                    // FIXME: how to implement this correctly here... >_>
                    if (stateSchematic.isFullCube())
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
                    IBakedModel bakedModel = this.worldRenderer.getModelForState(stateSchematic);
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

        for (EnumFacing.Axis axis : PositionUtils.AXES_ALL)
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

    protected OverlayType getOverlayType(IBlockState stateSchematic, IBlockState stateClient)
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

    private void addTileEntity(BlockPos pos, CompiledChunk compiledChunk, Set<TileEntity> tileEntities)
    {
        TileEntity te = this.schematicWorldView.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);

        if (te != null)
        {
            TileEntityRenderer<TileEntity> tesr = TileEntityRendererDispatcher.instance.getRenderer(te);

            if (tesr != null)
            {
                compiledChunk.addTileEntity(te);

                if (tesr.isGlobalRenderer(te))
                {
                    tileEntities.add(te);
                }
            }
        }
    }

    private void preRenderBlocks(BufferBuilder buffer, BlockPos pos)
    {
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        buffer.setTranslation(-pos.getX(), -pos.getY(), -pos.getZ());
    }

    private void postRenderBlocks(BlockRenderLayer layer, float x, float y, float z, BufferBuilder buffer, CompiledChunkSchematic compiledChunk)
    {
        if (layer == BlockRenderLayer.TRANSLUCENT && compiledChunk.isLayerEmpty(layer) == false)
        {
            buffer.sortVertexData(x, y, z);
            compiledChunk.setBlockBufferState(layer, buffer.getVertexState());
        }

        buffer.finishDrawing();
    }

    private void preRenderOverlay(BufferBuilder buffer, OverlayRenderType type)
    {
        this.existingOverlays.add(type);
        this.hasOverlay = true;

        BlockPos pos = this.getPosition();
        buffer.begin(type.getGlMode(), DefaultVertexFormats.POSITION_COLOR);
        buffer.setTranslation(-pos.getX(), -pos.getY(), -pos.getZ());
    }

    private void preRenderOverlay(BufferBuilder buffer, int glMode)
    {
        BlockPos pos = this.getPosition();
        buffer.begin(glMode, DefaultVertexFormats.POSITION_COLOR);
        buffer.setTranslation(-pos.getX(), -pos.getY(), -pos.getZ());
    }

    private void postRenderOverlay(OverlayRenderType type, float x, float y, float z, BufferBuilder buffer, CompiledChunkSchematic compiledChunk)
    {
        if (type == OverlayRenderType.QUAD && compiledChunk.isOverlayTypeEmpty(type) == false)
        {
            buffer.sortVertexData(x, y, z);
            compiledChunk.setOverlayBufferState(type, buffer.getVertexState());
        }

        buffer.finishDrawing();
    }

    public ChunkRenderTaskSchematic makeCompileTaskChunkSchematic()
    {
        this.getLockCompileTask().lock();
        ChunkRenderTaskSchematic generator = null;

        try
        {
            //if (GuiBase.isCtrlDown()) System.out.printf("makeCompileTaskChunk()\n");
            this.finishCompileTask();
            this.compileTask = new ChunkRenderTaskSchematic(this, ChunkRenderTaskSchematic.Type.REBUILD_CHUNK, this.getDistanceSq());
            this.rebuildWorldView();
            generator = this.compileTask;
        }
        finally
        {
            this.getLockCompileTask().unlock();
        }

        return generator;
    }

    @Nullable
    public ChunkRenderTaskSchematic makeCompileTaskTransparencySchematic()
    {
        this.getLockCompileTask().lock();

        try
        {
            if (this.compileTask == null || this.compileTask.getStatus() != ChunkRenderTaskSchematic.Status.PENDING)
            {
                if (this.compileTask != null && this.compileTask.getStatus() != ChunkRenderTaskSchematic.Status.DONE)
                {
                    this.compileTask.finish();
                }

                this.compileTask = new ChunkRenderTaskSchematic(this, ChunkRenderTaskSchematic.Type.RESORT_TRANSPARENCY, this.getDistanceSq());
                this.compileTask.setCompiledChunk(this.compiledChunk);

                return this.compileTask;
            }
        }
        finally
        {
            this.getLockCompileTask().unlock();
        }

        return null;
    }

    protected void finishCompileTask()
    {
        this.getLockCompileTask().lock();

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
            this.getLockCompileTask().unlock();
        }
    }

    private void rebuildWorldView()
    {
        synchronized (this.boxes)
        {
            this.schematicWorldView = new ChunkCacheSchematic(this.getWorld(), this.getPosition(), 2);
            this.clientWorldView    = new ChunkCacheSchematic(Minecraft.getInstance().world, this.getPosition(), 2);

            BlockPos pos = this.getPosition();
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
