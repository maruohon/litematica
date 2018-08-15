package fi.dy.masa.litematica.render.schematic;

import java.util.HashSet;
import java.util.Set;
import org.lwjgl.opengl.GL11;
import com.google.common.collect.Sets;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.mixin.IMixinCompiledChunk;
import fi.dy.masa.litematica.mixin.IMixinRenderChunk;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
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
    private ChunkCacheSchematic schematicWorldView;
    private ChunkCacheSchematic clientWorldView;

    public RenderChunkSchematicVbo(World worldIn, RenderGlobal renderGlobalIn, int indexIn)
    {
        super(worldIn, renderGlobalIn, indexIn);

        this.renderGlobal = (RenderGlobalSchematic) renderGlobalIn;
    }

    @Override
    public void resortTransparency(float x, float y, float z, ChunkCompileTaskGenerator generator)
    {
        CompiledChunk compiledchunk = generator.getCompiledChunk();

        if (compiledchunk.getState() != null)
        {
            if (Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue())
            {
                for (BlockRenderLayer layer : BlockRenderLayer.values())
                {
                    BufferBuilder buffer = generator.getRegionRenderCacheBuilder().getWorldRendererByLayer(layer);

                    this.preRenderBlocks(buffer, this.getPosition());
                    buffer.setVertexState(compiledchunk.getState());
                    this.postRenderBlocks(layer, x, y, z, buffer, compiledchunk);
                }
            }
            else if (compiledchunk.isLayerEmpty(BlockRenderLayer.TRANSLUCENT) == false)
            {
                BufferBuilder buffer = generator.getRegionRenderCacheBuilder().getWorldRendererByLayer(BlockRenderLayer.TRANSLUCENT);

                this.preRenderBlocks(buffer, this.getPosition());
                buffer.setVertexState(compiledchunk.getState());
                this.postRenderBlocks(BlockRenderLayer.TRANSLUCENT, x, y, z, buffer, compiledchunk);
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

        VisGraph visGraph = new VisGraph();
        HashSet<TileEntity> tileEntities = Sets.newHashSet();
        BlockPos pos = this.getPosition();
        final int yMin = DataManager.getLayerMin();
        final int yMax = DataManager.getLayerMax();

        if (this.schematicWorldView.isEmpty() == false &&
            pos.getY() + 15 >= yMin &&
            pos.getY()      <= yMax)
        {
            ++schematicRenderChunksUpdated;
            boolean[] usedLayers = new boolean[BlockRenderLayer.values().length];
            BlockPos posFrom = new BlockPos(pos.getX()     , MathHelper.clamp(yMin, pos.getY(), pos.getY() + 15), pos.getZ()     );
            BlockPos posTo   = new BlockPos(pos.getX() + 15, MathHelper.clamp(yMax, pos.getY(), pos.getY() + 15), pos.getZ() + 15);

            for (BlockPos.MutableBlockPos posMutable : BlockPos.getAllInBoxMutable(posFrom, posTo))
            {
                IBlockState stateClient = this.clientWorldView.getBlockState(posMutable);

                if (stateClient.getBlock() != Blocks.AIR)
                {
                    continue;
                }

                IBlockState state = this.schematicWorldView.getBlockState(posMutable);
                Block block = state.getBlock();

                /*
                if (iblockstate.isOpaqueCube())
                {
                    visGraph.setOpaqueCube(blockpos$mutableblockpos);
                }
                */

                if (block.hasTileEntity())
                {
                    TileEntity tileentity = this.schematicWorldView.getTileEntity(posMutable, Chunk.EnumCreateEntityType.CHECK);

                    if (tileentity != null)
                    {
                        TileEntitySpecialRenderer<TileEntity> tileentityspecialrenderer = TileEntityRendererDispatcher.instance.<TileEntity>getRenderer(tileentity);

                        if (tileentityspecialrenderer != null)
                        {
                            compiledChunk.addTileEntity(tileentity);

                            if (tileentityspecialrenderer.isGlobalRenderer(tileentity))
                            {
                                tileEntities.add(tileentity);
                            }
                        }
                    }
                }

                BlockRenderLayer layer = Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue() ? BlockRenderLayer.TRANSLUCENT : block.getBlockLayer();
                int layerIndex = layer.ordinal();

                if (state.getRenderType() != EnumBlockRenderType.INVISIBLE)
                {
                    BufferBuilder buffer = generator.getRegionRenderCacheBuilder().getWorldRendererByLayerId(layerIndex);

                    if (compiledChunk.isLayerStarted(layer) == false)
                    {
                        compiledChunk.setLayerStarted(layer);
                        this.preRenderBlocks(buffer, this.getPosition());
                    }

                    usedLayers[layerIndex] |= this.renderGlobal.renderBlock(state, posMutable, this.schematicWorldView, buffer);
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

    private void preRenderBlocks(BufferBuilder bufferBuilderIn, BlockPos pos)
    {
        bufferBuilderIn.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        bufferBuilderIn.setTranslation(-pos.getX(), -pos.getY(), -pos.getZ());
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

    @Override
    public ChunkCompileTaskGenerator makeCompileTaskChunk()
    {
        this.getLockCompileTask().lock();
        ChunkCompileTaskGenerator chunkcompiletaskgenerator;

        try
        {
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
