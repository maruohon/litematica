package fi.dy.masa.litematica.render.schematic;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.lwjgl.opengl.GL11;
import com.google.common.collect.Sets;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.interfaces.IRegionRenderCacheBuilder;
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
import net.minecraft.client.renderer.RegionRenderCacheBuilder;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.chunk.VisGraph;
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
    private final VertexBuffer[] vertexBufferOverlay = new VertexBuffer[OverlayType.values().length];
    private final BufferBuilder.State bufferStates[] = new BufferBuilder.State[OverlayType.values().length];
    private final EnumSet<OverlayType> existingOverlays = EnumSet.noneOf(OverlayType.class);
    private final Set<TileEntity> setTileEntities = new HashSet<>();
    private final List<StructureBoundingBox> boxes = new ArrayList<>();

    private ChunkCacheSchematic schematicWorldView;
    private ChunkCacheSchematic clientWorldView;

    public RenderChunkSchematicVbo(World worldIn, RenderGlobal renderGlobalIn, int indexIn)
    {
        super(worldIn, renderGlobalIn, indexIn);

        this.renderGlobal = (RenderGlobalSchematic) renderGlobalIn;

        if (OpenGlHelper.useVbo())
        {
            for (int i = 0; i < OverlayType.values().length; ++i)
            {
                this.vertexBufferOverlay[i] = new VertexBuffer(DefaultVertexFormats.POSITION_COLOR);
            }
        }
    }

    public boolean hasOverlay()
    {
        return this.existingOverlays.isEmpty() == false;
    }

    public EnumSet<OverlayType> getOverlayTypes()
    {
        return this.existingOverlays;
    }

    public VertexBuffer getOverlayVertexBuffer(OverlayType type)
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

    @Override
    public void resortTransparency(float x, float y, float z, ChunkCompileTaskGenerator generator)
    {
        CompiledChunk compiledChunk = generator.getCompiledChunk();

        if (compiledChunk.getState() != null)
        {
            if (Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue())
            {
                for (BlockRenderLayer layer : BlockRenderLayer.values())
                {
                    BufferBuilder buffer = generator.getRegionRenderCacheBuilder().getWorldRendererByLayer(layer);

                    this.preRenderBlocks(buffer, this.getPosition());
                    buffer.setVertexState(compiledChunk.getState());
                    this.postRenderBlocks(layer, x, y, z, buffer, compiledChunk);
                }
            }
            else if (compiledChunk.isLayerEmpty(BlockRenderLayer.TRANSLUCENT) == false)
            {
                BufferBuilder buffer = generator.getRegionRenderCacheBuilder().getWorldRendererByLayer(BlockRenderLayer.TRANSLUCENT);

                this.preRenderBlocks(buffer, this.getPosition());
                buffer.setVertexState(compiledChunk.getState());
                this.postRenderBlocks(BlockRenderLayer.TRANSLUCENT, x, y, z, buffer, compiledChunk);
            }

            //if (GuiScreen.isCtrlKeyDown()) System.out.printf("resortTransparency\n");
            if (Configs.Visuals.SCHEMATIC_OVERLAY_ENABLED.getBooleanValue())
            {
                this.resortTransparencyPreRenderOverlays(generator);
                this.postRenderOverlays(x, y, z, generator);
            }
        }
    }

    @Override
    public void rebuildChunk(float x, float y, float z, ChunkCompileTaskGenerator generator)
    {
        CompiledChunk compiledChunk = new CompiledChunk();
        generator.getLock().lock();

        try
        {
            if (generator.getStatus() != ChunkCompileTaskGenerator.Status.COMPILING)
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
        VisGraph visGraph = new VisGraph();
        Set<TileEntity> tileEntities = new HashSet<>();
        BlockPos posChunk = this.getPosition();
        LayerRange range = DataManager.getRenderLayerRange();
        final boolean renderColliding = Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue();

        this.existingOverlays.clear();

        if (this.schematicWorldView.isEmpty() == false && this.boxes.isEmpty() == false &&
            range.intersects(new SubChunkPos(posChunk.getX() >> 4, posChunk.getY() >> 4, posChunk.getZ() >> 4)))
        {
            ++schematicRenderChunksUpdated;

            boolean[] usedLayers = new boolean[BlockRenderLayer.values().length];
            RegionRenderCacheBuilder buffers = generator.getRegionRenderCacheBuilder();
            BufferBuilder bufferOverlayOutlines = ((IRegionRenderCacheBuilder) buffers).getOverlayBuffer(OverlayType.OUTLINE);
            BufferBuilder bufferOverlayQuads    = ((IRegionRenderCacheBuilder) buffers).getOverlayBuffer(OverlayType.QUAD);

            //if (GuiScreen.isCtrlKeyDown()) System.out.printf("rebuildChunk OL start pos: %s gen: %s\n", this.getPosition(), generator);
            this.preRenderOverlayIfNotStarted(bufferOverlayOutlines, OverlayType.OUTLINE);
            this.preRenderOverlayIfNotStarted(bufferOverlayQuads, OverlayType.QUAD);

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

                        BlockRenderLayer layer = Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue() ? BlockRenderLayer.TRANSLUCENT : blockSchematic.getBlockLayer();
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
                                if (Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_MISSING.getBooleanValue())
                                {
                                    overlayColor = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_MISSING.getColor();
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
                            if (Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_EXTRA.getBooleanValue())
                            {
                                overlayColor = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_EXTRA.getColor();
                            }
                        }
                        // Wrong block
                        else if (blockClient != blockSchematic)
                        {
                            if (Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_WRONG_BLOCK.getBooleanValue())
                            {
                                overlayColor = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_WRONG_BLOCK.getColor();
                            }
                        }
                        // Wrong state
                        else
                        {
                            if (Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_WRONG_STATE.getBooleanValue())
                            {
                                overlayColor = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_WRONG_STATE.getColor();
                            }
                        }
                    }

                    if (overlayColor != null && Configs.Visuals.SCHEMATIC_OVERLAY_ENABLED.getBooleanValue())
                    {
                        if (Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_SIDES.getBooleanValue())
                        {
                            // Only render the model-based outlines or sides for missing blocks
                            if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_SIDES.getBooleanValue())
                            {
                                IBakedModel bakedModel = this.renderGlobal.getModelForState(stateSchematic);
                                RenderUtils.drawBlockModelQuadOverlayBatched(bakedModel, stateSchematic, posMutable, overlayColor, 0, bufferOverlayQuads);
                            }
                            else
                            {
                                RenderUtils.drawBlockBoundingBoxSidesBatched(posMutable, overlayColor, 0, bufferOverlayQuads);
                            }
                        }

                        if (Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_OUTLINES.getBooleanValue())
                        {
                            overlayColor = new Color4f(overlayColor.r, overlayColor.g, overlayColor.b, 1f);

                            // Only render the model-based outlines or sides for missing blocks
                            if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_OUTLINE.getBooleanValue())
                            {
                                IBakedModel bakedModel = this.renderGlobal.getModelForState(stateSchematic);
                                RenderUtils.drawBlockModelOutlinesBatched(bakedModel, stateSchematic, posMutable, overlayColor, 0, bufferOverlayOutlines);
                            }
                            else
                            {
                                RenderUtils.drawBlockBoundingBoxOutlinesBatched(posMutable, overlayColor, 0, bufferOverlayOutlines);
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
                    this.postRenderBlocks(layerTmp, x, y, z, generator.getRegionRenderCacheBuilder().getWorldRendererByLayer(layerTmp), compiledChunk);
                }
            }

            this.postRenderOverlays(x, y, z, generator);
        }

        compiledChunk.setVisibility(visGraph.computeVisibility());
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

    private void postRenderBlocks(BlockRenderLayer layer, float x, float y, float z, BufferBuilder bufferBuilderIn, CompiledChunk compiledChunkIn)
    {
        if (layer == BlockRenderLayer.TRANSLUCENT && compiledChunkIn.isLayerEmpty(layer) == false)
        {
            bufferBuilderIn.sortVertexData(x, y, z);
            compiledChunkIn.setState(bufferBuilderIn.getVertexState());
        }

        bufferBuilderIn.finishDrawing();
    }

    private void preRenderOverlayIfNotStarted(BufferBuilder buffer, OverlayType type)
    {
        if (this.existingOverlays.contains(type) == false)
        {
            this.preRenderOverlay(buffer, type);
        }
    }

    private void preRenderOverlay(BufferBuilder buffer, OverlayType type)
    {
        this.existingOverlays.add(type);

        BlockPos pos = this.getPosition();
        buffer.begin(type.getGlMode(), DefaultVertexFormats.POSITION_COLOR);
        buffer.setTranslation(-pos.getX(), -pos.getY(), -pos.getZ());
    }

    private void resortTransparencyPreRenderOverlays(ChunkCompileTaskGenerator generator)
    {
        if (this.hasOverlay())
        {
            //if (GuiScreen.isCtrlKeyDown()) System.out.printf("resortTransparencyPreRenderOverlays\n");
            RegionRenderCacheBuilder buffers = generator.getRegionRenderCacheBuilder();

            for (OverlayType type : this.existingOverlays)
            {
                BufferBuilder.State state = this.bufferStates[type.ordinal()];

                if (state != null)
                {
                    BufferBuilder buffer = ((IRegionRenderCacheBuilder) buffers).getOverlayBuffer(type);
                    this.preRenderOverlay(buffer, type);
                    buffer.setVertexState(state);
                }
            }
        }
    }

    private void postRenderOverlays(float x, float y, float z, ChunkCompileTaskGenerator generator)
    {
        if (this.hasOverlay())
        {
            //if (GuiScreen.isCtrlKeyDown()) System.out.printf("postRenderOverlays\n");
            RegionRenderCacheBuilder buffers = generator.getRegionRenderCacheBuilder();

            for (OverlayType type : this.existingOverlays)
            {
                BufferBuilder buffer = ((IRegionRenderCacheBuilder) buffers).getOverlayBuffer(type);

                buffer.sortVertexData(x, y, z);
                this.bufferStates[type.ordinal()] = buffer.getVertexState();
                buffer.finishDrawing();
            }
        }
    }

    @Override
    public ChunkCompileTaskGenerator makeCompileTaskChunk()
    {
        this.getLockCompileTask().lock();
        ChunkCompileTaskGenerator chunkcompiletaskgenerator;

        try
        {
            //if (GuiScreen.isCtrlKeyDown()) System.out.printf("makeCompileTaskChunk()\n");
            this.finishCompileTask();
            ((IMixinRenderChunk) this).setCompileTask(new ChunkCompileTaskGenerator(this, ChunkCompileTaskGenerator.Type.REBUILD_CHUNK, this.getDistanceSq()));
            this.rebuildWorldView();
            chunkcompiletaskgenerator = ((IMixinRenderChunk) this).getCompileTask();
        }
        finally
        {
            this.getLockCompileTask().unlock();
        }

        return chunkcompiletaskgenerator;
    }

    private void rebuildWorldView()
    {
        this.schematicWorldView = new ChunkCacheSchematic(this.getWorld(), this.getPosition(), 2);
        this.clientWorldView    = new ChunkCacheSchematic(Minecraft.getMinecraft().world, this.getPosition(), 2);

        this.boxes.clear();
        BlockPos pos = this.getPosition();
        SubChunkPos subChunk = new SubChunkPos(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
        this.boxes.addAll(DataManager.getInstance().getSchematicPlacementManager().getTouchedBoxesInSubChunk(subChunk));
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
