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
import fi.dy.masa.litematica.mixin.IMixinCompiledChunk;
import fi.dy.masa.litematica.mixin.IMixinRenderChunk;
import fi.dy.masa.litematica.render.RenderUtils;
import fi.dy.masa.litematica.util.LayerRange;
import fi.dy.masa.litematica.util.SubChunkPos;
import fi.dy.masa.malilib.util.Color4f;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.StructureBoundingBox;

public class RenderChunkSchematicVbo extends RenderChunk
{
    public static int schematicRenderChunksUpdated;

    private final RenderGlobalSchematic renderGlobal;
    private final VertexBufferSchematic[] vertexBufferOverlay = new VertexBufferSchematic[OverlayType.values().length];
    private final Set<TileEntity> setTileEntities = new HashSet<>();
    private final List<StructureBoundingBox> boxes = new ArrayList<>();
    private final EnumSet<OverlayType> existingOverlays = EnumSet.noneOf(OverlayType.class);
    private boolean hasOverlay = false;
    private ChunkCompileTaskGeneratorSchematic compileTask;

    private ChunkCacheSchematic schematicWorldView;
    private ChunkCacheSchematic clientWorldView;

    public RenderChunkSchematicVbo(World worldIn, RenderGlobal renderGlobalIn, int indexIn)
    {
        super(worldIn, renderGlobalIn, indexIn);

        this.renderGlobal = (RenderGlobalSchematic) renderGlobalIn;

        if (OpenGlHelper.useVbo())
        {
            // Delete the vanilla buffers and re-create them using our overridden class
            VertexBuffer[] buffers = ((IMixinRenderChunk) this).getVertexBuffers();

            for (int i = 0; i < buffers.length; ++i)
            {
                if (buffers[i] != null)
                {
                    buffers[i].deleteGlBuffers();
                }

                buffers[i] = new VertexBufferSchematic(DefaultVertexFormats.BLOCK);
            }

            for (int i = 0; i < OverlayType.values().length; ++i)
            {
                this.vertexBufferOverlay[i] = new VertexBufferSchematic(DefaultVertexFormats.POSITION_COLOR);
            }
        }
    }

    public boolean hasOverlay()
    {
        return this.hasOverlay;
    }

    public EnumSet<OverlayType> getOverlayTypes()
    {
        return this.existingOverlays;
    }

    public VertexBufferSchematic getOverlayVertexBuffer(OverlayType type)
    {
        //if (GuiScreen.isCtrlKeyDown()) System.out.printf("getOverlayVertexBuffer: type: %s, buf: %s\n", type, this.vertexBufferOverlay[type.ordinal()]);
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

    public void resortTransparency(float x, float y, float z, ChunkCompileTaskGeneratorSchematic generator)
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

        //if (GuiScreen.isCtrlKeyDown()) System.out.printf("resortTransparency\n");
        if (Configs.Visuals.SCHEMATIC_OVERLAY_ENABLED.getBooleanValue())
        {
            OverlayType type = OverlayType.QUAD;
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

    public void rebuildChunk(float x, float y, float z, ChunkCompileTaskGeneratorSchematic generator)
    {
        CompiledChunkSchematic compiledChunk = new CompiledChunkSchematic();
        generator.getLock().lock();

        try
        {
            if (generator.getStatus() != ChunkCompileTaskGeneratorSchematic.Status.COMPILING)
            {
                return;
            }

            generator.setCompiledChunk(compiledChunk);
        }
        finally
        {
            generator.getLock().unlock();
        }

        //if (GuiScreen.isCtrlKeyDown()) System.out.printf("rebuildChunk pos: %s gen: %s\n", this.getPosition(), generator);
        Set<TileEntity> tileEntities = new HashSet<>();
        BlockPos posChunk = this.getPosition();
        LayerRange range = DataManager.getRenderLayerRange();

        this.existingOverlays.clear();
        this.hasOverlay = false;

        synchronized (this.boxes)
        {
            if (this.schematicWorldView.isEmpty() == false && this.boxes.isEmpty() == false &&
                    range.intersects(new SubChunkPos(posChunk.getX() >> 4, posChunk.getY() >> 4, posChunk.getZ() >> 4)))
                {
                    ++schematicRenderChunksUpdated;

                    final boolean renderColliding = Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue();
                    final boolean translucent = Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue();
                    final boolean overlayEnabled = Configs.Visuals.SCHEMATIC_OVERLAY_ENABLED.getBooleanValue();
                    final boolean overlayOutlinesEnabled = overlayEnabled && Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_OUTLINES.getBooleanValue();
                    final boolean overlaySidesEnabled = overlayEnabled && Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_SIDES.getBooleanValue();
                    final boolean overlayOutlinesModel = Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_OUTLINE.getBooleanValue();
                    final boolean overlaySidesModel = Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_SIDES.getBooleanValue();
                    final boolean overlayTypeMissing = Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_MISSING.getBooleanValue();
                    final boolean overlayTypeExtra = Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_EXTRA.getBooleanValue();
                    final boolean overlayTypeWrongBlock = Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_WRONG_BLOCK.getBooleanValue();
                    final boolean overlayTypeWrongState = Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_WRONG_STATE.getBooleanValue();
                    final Color4f colorMissing = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_MISSING.getColor();
                    final Color4f colorExtra = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_EXTRA.getColor();
                    final Color4f colorWrongBlock = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_WRONG_BLOCK.getColor();
                    final Color4f colorWrongState = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_WRONG_STATE.getColor();
                    boolean[] usedLayers = new boolean[BlockRenderLayer.values().length];
                    BufferBuilderCache buffers = generator.getBufferCache();
                    BufferBuilder bufferOverlayOutlines = buffers.getOverlayBuffer(OverlayType.OUTLINE);
                    BufferBuilder bufferOverlayQuads    = buffers.getOverlayBuffer(OverlayType.QUAD);

                    for (StructureBoundingBox box : this.boxes)
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
                            IBlockState stateSchematic = this.schematicWorldView.getBlockState(posMutable);
                            IBlockState stateClient    = this.clientWorldView.getBlockState(posMutable);
                            stateSchematic = stateSchematic.getActualState(this.schematicWorldView, posMutable);
                            stateClient = stateClient.getActualState(this.clientWorldView, posMutable);
                            Block blockSchematic = stateSchematic.getBlock();
                            Block blockClient = stateClient.getBlock();
                            Color4f overlayColor = null;
                            boolean clientHasAir = blockClient == Blocks.AIR;
                            boolean missing = false;

                            if (clientHasAir && blockSchematic == Blocks.AIR)
                            {
                                continue;
                            }

                            // Schematic has a block, client has air
                            if (clientHasAir || (renderColliding && stateSchematic != stateClient))
                            {
                                if (blockSchematic.hasTileEntity())
                                {
                                    this.addTileEntity(posMutable, compiledChunk, tileEntities);
                                }

                                BlockRenderLayer layer = translucent ? BlockRenderLayer.TRANSLUCENT : blockSchematic.getRenderLayer();
                                int layerIndex = layer.ordinal();

                                if (stateSchematic.getRenderType() != EnumBlockRenderType.INVISIBLE)
                                {
                                    BufferBuilder bufferSchematic = buffers.getWorldRendererByLayerId(layerIndex);

                                    if (compiledChunk.isLayerStarted(layer) == false)
                                    {
                                        compiledChunk.setLayerStarted(layer);
                                        this.preRenderBlocks(bufferSchematic, this.getPosition());
                                    }

                                    usedLayers[layerIndex] |= this.renderGlobal.renderBlock(stateSchematic, posMutable, this.schematicWorldView, bufferSchematic);

                                    if (clientHasAir)
                                    {
                                        if (overlayTypeMissing)
                                        {
                                            overlayColor = colorMissing;
                                        }

                                        missing = true;
                                    }
                                }
                            }

                            if (clientHasAir == false && stateSchematic != stateClient)
                            {
                                // Extra block
                                if (blockSchematic == Blocks.AIR)
                                {
                                    if (overlayTypeExtra)
                                    {
                                        overlayColor = colorExtra;
                                    }
                                }
                                // Wrong block
                                else if (blockClient != blockSchematic)
                                {
                                    if (overlayTypeWrongBlock)
                                    {
                                        overlayColor = colorWrongBlock;
                                    }
                                }
                                // Wrong state
                                else
                                {
                                    if (overlayTypeWrongState)
                                    {
                                        overlayColor = colorWrongState;
                                    }
                                }
                            }

                            if (overlayColor != null)
                            {
                                if (overlaySidesEnabled)
                                {
                                    if (compiledChunk.isOverlayTypeStarted(OverlayType.QUAD) == false)
                                    {
                                        compiledChunk.setOverlayTypeStarted(OverlayType.QUAD);
                                        this.preRenderOverlay(bufferOverlayQuads, OverlayType.QUAD);
                                    }

                                    // Only render the model-based outlines or sides for missing blocks
                                    if (missing && overlaySidesModel)
                                    {
                                        IBakedModel bakedModel = this.renderGlobal.getModelForState(stateSchematic);
                                        RenderUtils.drawBlockModelQuadOverlayBatched(bakedModel, stateSchematic, posMutable, overlayColor, 0, bufferOverlayQuads);
                                    }
                                    else
                                    {
                                        fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxSidesBatchedQuads(posMutable, overlayColor, 0, bufferOverlayQuads);
                                    }
                                }

                                if (overlayOutlinesEnabled)
                                {
                                    if (compiledChunk.isOverlayTypeStarted(OverlayType.OUTLINE) == false)
                                    {
                                        compiledChunk.setOverlayTypeStarted(OverlayType.OUTLINE);
                                        this.preRenderOverlay(bufferOverlayOutlines, OverlayType.OUTLINE);
                                    }

                                    overlayColor = new Color4f(overlayColor.r, overlayColor.g, overlayColor.b, 1f);

                                    // Only render the model-based outlines or sides for missing blocks
                                    if (missing && overlayOutlinesModel)
                                    {
                                        IBakedModel bakedModel = this.renderGlobal.getModelForState(stateSchematic);
                                        RenderUtils.drawBlockModelOutlinesBatched(bakedModel, stateSchematic, posMutable, overlayColor, 0, bufferOverlayOutlines);
                                    }
                                    else
                                    {
                                        fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(posMutable, overlayColor, 0, bufferOverlayOutlines);
                                    }
                                }
                            }
                        }
                    }

                    for (BlockRenderLayer layerTmp : BlockRenderLayer.values())
                    {
                        if (usedLayers[layerTmp.ordinal()])
                        {
                            ((IMixinCompiledChunk) compiledChunk).invokeSetLayerUsed(layerTmp);
                        }

                        if (compiledChunk.isLayerStarted(layerTmp))
                        {
                            this.postRenderBlocks(layerTmp, x, y, z, buffers.getWorldRendererByLayer(layerTmp), compiledChunk);
                        }
                    }

                    if (this.hasOverlay)
                    {
                        //if (GuiScreen.isCtrlKeyDown()) System.out.printf("postRenderOverlays\n");
                        for (OverlayType type : this.existingOverlays)
                        {
                            if (compiledChunk.isOverlayTypeStarted(type))
                            {
                                compiledChunk.setOverlayTypeUsed(type);
                                this.postRenderOverlay(type, x, y, z, buffers.getOverlayBuffer(type), compiledChunk);
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
            this.renderGlobal.updateTileEntities(set1, set);
        }
        finally
        {
            this.getLockCompileTask().unlock();
        }
    }

    private void addTileEntity(BlockPos pos, CompiledChunk compiledChunk, Set<TileEntity> tileEntities)
    {
        TileEntity te = this.schematicWorldView.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);

        if (te != null)
        {
            TileEntitySpecialRenderer<TileEntity> tesr = TileEntityRendererDispatcher.instance.<TileEntity>getRenderer(te);

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

    private void preRenderOverlay(BufferBuilder buffer, OverlayType type)
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

    private void postRenderOverlay(OverlayType type, float x, float y, float z, BufferBuilder buffer, CompiledChunkSchematic compiledChunk)
    {
        if (type == OverlayType.QUAD && compiledChunk.isOverlayTypeEmpty(type) == false)
        {
            buffer.sortVertexData(x, y, z);
            compiledChunk.setOverlayBufferState(type, buffer.getVertexState());
        }

        buffer.finishDrawing();
    }

    public ChunkCompileTaskGeneratorSchematic makeCompileTaskChunkSchematic()
    {
        this.getLockCompileTask().lock();
        ChunkCompileTaskGeneratorSchematic generator = null;

        try
        {
            //if (GuiScreen.isCtrlKeyDown()) System.out.printf("makeCompileTaskChunk()\n");
            this.finishCompileTask();
            this.compileTask = new ChunkCompileTaskGeneratorSchematic(this, ChunkCompileTaskGeneratorSchematic.Type.REBUILD_CHUNK, this.getDistanceSq());
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
    public ChunkCompileTaskGeneratorSchematic makeCompileTaskTransparencySchematic()
    {
        this.getLockCompileTask().lock();

        try
        {
            if (this.compileTask == null || this.compileTask.getStatus() != ChunkCompileTaskGeneratorSchematic.Status.PENDING)
            {
                if (this.compileTask != null && this.compileTask.getStatus() != ChunkCompileTaskGeneratorSchematic.Status.DONE)
                {
                    this.compileTask.finish();
                }

                this.compileTask = new ChunkCompileTaskGeneratorSchematic(this, ChunkCompileTaskGeneratorSchematic.Type.RESORT_TRANSPARENCY, this.getDistanceSq());
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
            if (this.compileTask != null && this.compileTask.getStatus() != ChunkCompileTaskGeneratorSchematic.Status.DONE)
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
            this.clientWorldView    = new ChunkCacheSchematic(Minecraft.getMinecraft().world, this.getPosition(), 2);

            BlockPos pos = this.getPosition();
            SubChunkPos subChunk = new SubChunkPos(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
            this.boxes.clear();
            this.boxes.addAll(DataManager.getSchematicPlacementManager().getTouchedBoxesInSubChunk(subChunk));
        }
    }

    public enum OverlayType
    {
        OUTLINE     (GL11.GL_LINES),
        QUAD        (GL11.GL_QUADS);

        private final int glMode;

        private OverlayType(int glMode)
        {
            this.glMode = glMode;
        }

        public int getGlMode()
        {
            return this.glMode;
        }
    }
}
