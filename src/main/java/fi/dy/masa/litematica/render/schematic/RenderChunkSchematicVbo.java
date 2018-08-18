package fi.dy.masa.litematica.render.schematic;

import java.util.HashSet;
import java.util.Set;
import org.lwjgl.opengl.GL11;
import com.google.common.collect.Sets;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.interfaces.IRegionRenderCacheBuilder;
import fi.dy.masa.litematica.mixin.IMixinCompiledChunk;
import fi.dy.masa.litematica.mixin.IMixinRenderChunk;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RegionRenderCacheBuilder;
import net.minecraft.client.renderer.RenderGlobal;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class RenderChunkSchematicVbo extends RenderChunk
{
    public static int schematicRenderChunksUpdated;

    private final RenderGlobalSchematic renderGlobal;
    private final Set<TileEntity> setTileEntities = new HashSet<>();
    private final VertexBuffer[] vertexBufferOverlay = new VertexBuffer[2];
    private ChunkCacheSchematic schematicWorldView;
    private ChunkCacheSchematic clientWorldView;
    private BufferBuilder bufferOverlayOutlines;
    private BufferBuilder bufferOverlayQuads;
    private BufferBuilder.State bufferStateOutlines;
    private BufferBuilder.State bufferStateQuads;
    private boolean hasOverlay;

    public RenderChunkSchematicVbo(World worldIn, RenderGlobal renderGlobalIn, int indexIn)
    {
        super(worldIn, renderGlobalIn, indexIn);

        this.renderGlobal = (RenderGlobalSchematic) renderGlobalIn;

        if (OpenGlHelper.useVbo())
        {
            this.vertexBufferOverlay[0] = new VertexBuffer(DefaultVertexFormats.POSITION_COLOR);
            this.vertexBufferOverlay[1] = new VertexBuffer(DefaultVertexFormats.POSITION_COLOR);
        }
    }

    public boolean hasOverlay()
    {
        return this.hasOverlay;
    }

    public BufferBuilder getOverlayBufferBuilder(boolean outlineBuffer)
    {
        if (GuiScreen.isCtrlKeyDown()) System.out.printf("getOverlayBufferBuilder: %s\n", outlineBuffer ? this.bufferOverlayOutlines : this.bufferOverlayQuads);
        return outlineBuffer ? this.bufferOverlayOutlines : this.bufferOverlayQuads;
    }

    public VertexBuffer getOverlayVertexBuffer(boolean outlineBuffer)
    {
        if (GuiScreen.isCtrlKeyDown()) System.out.printf("getOverlayVertexBuffer: %s\n", this.vertexBufferOverlay[outlineBuffer ? 0 : 1]);
        return this.vertexBufferOverlay[outlineBuffer ? 0 : 1];
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

            if (GuiScreen.isCtrlKeyDown()) System.out.printf("resortTransparency\n");
            this.resortTransparencyPreRenderOverlays(generator);
            this.postRenderOverlays(x, y, z, generator);
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

        if (GuiScreen.isCtrlKeyDown()) System.out.printf("rebuildChunk pos: %s gen: %s\n", this.getPosition(), generator);
        VisGraph visGraph = new VisGraph();
        Set<TileEntity> tileEntities = new HashSet<>();
        BlockPos posChunk = this.getPosition();
        final int yMin = DataManager.getLayerMin();
        final int yMax = DataManager.getLayerMax();
        this.hasOverlay = false;

        if (this.schematicWorldView.isEmpty() == false &&
            posChunk.getY() + 15 >= yMin &&
            posChunk.getY()      <= yMax)
        {
            ++schematicRenderChunksUpdated;

            RegionRenderCacheBuilder buffers = generator.getRegionRenderCacheBuilder();
            BufferBuilder bufferOverlayOutlines = ((IRegionRenderCacheBuilder) buffers).getOverlayBuffer(true);
            BufferBuilder bufferOverlayQuads    = ((IRegionRenderCacheBuilder) buffers).getOverlayBuffer(false);

            boolean[] usedLayers = new boolean[BlockRenderLayer.values().length];
            BlockPos posFrom = new BlockPos(posChunk.getX()     , MathHelper.clamp(yMin, posChunk.getY(), posChunk.getY() + 15), posChunk.getZ()     );
            BlockPos posTo   = new BlockPos(posChunk.getX() + 15, MathHelper.clamp(yMax, posChunk.getY(), posChunk.getY() + 15), posChunk.getZ() + 15);

            for (BlockPos.MutableBlockPos posMutable : BlockPos.getAllInBoxMutable(posFrom, posTo))
            {
                IBlockState stateSchematic = this.schematicWorldView.getBlockState(posMutable);
                IBlockState stateClient    = this.clientWorldView.getBlockState(posMutable);
                stateClient = stateClient.getActualState(this.clientWorldView, posMutable);
                Block blockSchematic = stateSchematic.getBlock();
                Block blockClient = stateClient.getBlock();

                if (blockClient == Blocks.AIR)
                {
                    // Both are air
                    if (blockSchematic == Blocks.AIR)
                    {
                        continue;
                    }

                    // Schematic has a block, client has air

                    /*
                    if (iblockstate.isOpaqueCube())
                    {
                        visGraph.setOpaqueCube(blockpos$mutableblockpos);
                    }
                    */

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

                        this.preRenderOverlays(bufferOverlayOutlines, bufferOverlayQuads);

                        usedLayers[layerIndex] |= this.renderGlobal.renderBlock(stateSchematic, posMutable, this.schematicWorldView, bufferSchematic);

                        //RenderUtils.renderBlockOverlay(posMutable, 0.01, new Vec4f(0.5f, 0.9f, 0.9f, 0.5f), bufferOverlayOutlines);
                        float r = 0.5f, g = 1f, b = 1f, a = 0.3f;
                        double o = 0.001;
                        bufferOverlayOutlines.pos(posMutable.getX() - o    , posMutable.getY() + o + 1, posMutable.getZ()    ).color(r, g, b, a).endVertex();
                        bufferOverlayOutlines.pos(posMutable.getX() - o    , posMutable.getY() + o + 1, posMutable.getZ() + 1).color(r, g, b, a).endVertex();
                        bufferOverlayOutlines.pos(posMutable.getX() + o + 1, posMutable.getY() + o + 1, posMutable.getZ() + 1).color(r, g, b, a).endVertex();
                        bufferOverlayOutlines.pos(posMutable.getX() + o + 1, posMutable.getY() + o + 1, posMutable.getZ()    ).color(r, g, b, a).endVertex();
                    }
                }
                else if (stateSchematic != stateClient)
                {
                    this.preRenderOverlays(bufferOverlayOutlines, bufferOverlayQuads);

                    // Extra block
                    if (blockSchematic == Blocks.AIR)
                    {
                        
                    }
                    // Wrong block
                    else if (blockClient != blockSchematic)
                    {
                        
                    }
                    // Wrong state
                    else
                    {
                        
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

    private void preRenderOverlays(BufferBuilder buffer, BlockPos pos)
    {
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        //buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        buffer.setTranslation(-pos.getX(), -pos.getY(), -pos.getZ());
    }

    private void preRenderOverlays(BufferBuilder bufferOverlayOutlines, BufferBuilder bufferOverlayQuads)
    {
        // TODO hook to a config option
        if (this.hasOverlay == false)
        {
            if (GuiScreen.isCtrlKeyDown()) System.out.printf("preRenderOverlays\n");
            this.preRenderOverlays(bufferOverlayOutlines, this.getPosition());
            this.preRenderOverlays(bufferOverlayQuads, this.getPosition());

            // Ugly hack of buffering these here, since we need these in the
            // ChunkRenderDispatcher Mixin, which doesn't have access to the ChunkCompileTaskGenerator
            this.bufferOverlayOutlines = bufferOverlayOutlines;
            this.bufferOverlayQuads = bufferOverlayQuads;

            this.hasOverlay = true;
        }
    }

    private void resortTransparencyPreRenderOverlays(ChunkCompileTaskGenerator generator)
    {
        if (this.hasOverlay)
        {
            if (GuiScreen.isCtrlKeyDown()) System.out.printf("resortTransparencyPreRenderOverlays\n");
            RegionRenderCacheBuilder buffers = generator.getRegionRenderCacheBuilder();

            if (this.bufferStateOutlines != null && this.bufferStateQuads != null)
            {
                BufferBuilder bufferOverlayOutlines = ((IRegionRenderCacheBuilder) buffers).getOverlayBuffer(true);
                BufferBuilder bufferOverlayQuads    = ((IRegionRenderCacheBuilder) buffers).getOverlayBuffer(false);

                this.preRenderOverlays(bufferOverlayOutlines, this.getPosition());
                bufferOverlayOutlines.setVertexState(this.bufferStateOutlines);

                this.preRenderOverlays(bufferOverlayQuads, this.getPosition());
                bufferOverlayQuads.setVertexState(this.bufferStateQuads);
            }
        }
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

    private void postRenderOverlays(float x, float y, float z, ChunkCompileTaskGenerator generator)
    {
        if (this.hasOverlay)
        {
            if (GuiScreen.isCtrlKeyDown()) System.out.printf("postRenderOverlays\n");
            RegionRenderCacheBuilder buffers = generator.getRegionRenderCacheBuilder();
            BufferBuilder bufferOverlayOutlines = ((IRegionRenderCacheBuilder) buffers).getOverlayBuffer(true);
            BufferBuilder bufferOverlayQuads    = ((IRegionRenderCacheBuilder) buffers).getOverlayBuffer(false);

            bufferOverlayOutlines.sortVertexData(x, y, z);
            this.bufferStateOutlines = bufferOverlayOutlines.getVertexState();
            bufferOverlayOutlines.finishDrawing();

            bufferOverlayQuads.sortVertexData(x, y, z);
            this.bufferStateQuads = bufferOverlayQuads.getVertexState();
            bufferOverlayQuads.finishDrawing();
        }
    }

    @Override
    public ChunkCompileTaskGenerator makeCompileTaskChunk()
    {
        this.getLockCompileTask().lock();
        ChunkCompileTaskGenerator chunkcompiletaskgenerator;

        try
        {
            if (GuiScreen.isCtrlKeyDown()) System.out.printf("makeCompileTaskChunk()\n");
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
    }
}
