package fi.dy.masa.litematica.render.schematic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import javax.annotation.Nullable;
import org.apache.logging.log4j.Logger;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import fi.dy.masa.litematica.LiteModLitematica;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RegionRenderCacheBuilder;
import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.crash.CrashReport;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;

public class ChunkRenderWorkerLitematica implements Runnable
{
    private static final Logger LOGGER = LiteModLitematica.logger;

    private final ChunkRenderDispatcherLitematica chunkRenderDispatcher;
    private final RegionRenderCacheBuilder regionRenderCacheBuilder;
    private boolean shouldRun;

    public ChunkRenderWorkerLitematica(ChunkRenderDispatcherLitematica chunkRenderDispatcherIn)
    {
        this(chunkRenderDispatcherIn, null);
    }

    public ChunkRenderWorkerLitematica(ChunkRenderDispatcherLitematica chunkRenderDispatcherIn, @Nullable RegionRenderCacheBuilder regionRenderCacheBuilderIn)
    {
        this.shouldRun = true;
        this.chunkRenderDispatcher = chunkRenderDispatcherIn;
        this.regionRenderCacheBuilder = regionRenderCacheBuilderIn;
    }

    @Override
    public void run()
    {
        while (this.shouldRun)
        {
            try
            {
                this.processTask(this.chunkRenderDispatcher.getNextChunkUpdate());
            }
            catch (InterruptedException e)
            {
                LOGGER.debug("Stopping chunk worker due to interrupt");
                return;
            }
            catch (Throwable throwable)
            {
                CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Batching chunks");
                Minecraft.getMinecraft().crashed(Minecraft.getMinecraft().addGraphicsAndWorldToCrashReport(crashreport));
                return;
            }
        }
    }

    protected void processTask(final ChunkCompileTaskGenerator generator) throws InterruptedException
    {
        generator.getLock().lock();

        try
        {
            if (generator.getStatus() != ChunkCompileTaskGenerator.Status.PENDING)
            {
                if (generator.isFinished() == false)
                {
                    LOGGER.warn("Chunk render task was {} when I expected it to be pending; ignoring task", (Object)generator.getStatus());
                }

                return;
            }

            generator.setStatus(ChunkCompileTaskGenerator.Status.COMPILING);
        }
        finally
        {
            generator.getLock().unlock();
        }

        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();

        if (entity == null)
        {
            generator.finish();
        }
        else
        {
            generator.setRegionRenderCacheBuilder(this.getRegionRenderCacheBuilder());

            ChunkCompileTaskGenerator.Type generatorType = generator.getType();
            float x = (float) entity.posX;
            float y = (float) entity.posY + entity.getEyeHeight();
            float z = (float) entity.posZ;

            if (generatorType == ChunkCompileTaskGenerator.Type.REBUILD_CHUNK)
            {
                generator.getRenderChunk().rebuildChunk(x, y, z, generator);
            }
            else if (generatorType == ChunkCompileTaskGenerator.Type.RESORT_TRANSPARENCY)
            {
                generator.getRenderChunk().resortTransparency(x, y, z, generator);
            }

            generator.getLock().lock();

            try
            {
                if (generator.getStatus() != ChunkCompileTaskGenerator.Status.COMPILING)
                {
                    if (generator.isFinished() == false)
                    {
                        LOGGER.warn("Chunk render task was {} when I expected it to be compiling; aborting task", (Object)generator.getStatus());
                    }

                    this.freeRenderBuilder(generator);
                    return;
                }

                generator.setStatus(ChunkCompileTaskGenerator.Status.UPLOADING);
            }
            finally
            {
                generator.getLock().unlock();
            }

            final CompiledChunk compiledChunk = generator.getCompiledChunk();
            ArrayList<ListenableFuture<Object>> futuresList = Lists.newArrayList();
            RenderChunkSchematicVbo renderChunk = (RenderChunkSchematicVbo) generator.getRenderChunk();

            if (generatorType == ChunkCompileTaskGenerator.Type.REBUILD_CHUNK)
            {
                //if (GuiScreen.isCtrlKeyDown()) System.out.printf("pre uploadChunk()\n");
                for (BlockRenderLayer layer : BlockRenderLayer.values())
                {
                    if (compiledChunk.isLayerStarted(layer) || renderChunk.hasOverlay())
                    {
                        BufferBuilder buffer = generator.getRegionRenderCacheBuilder().getWorldRendererByLayer(layer);
                        futuresList.add(this.chunkRenderDispatcher.uploadChunk(generator, layer, buffer, renderChunk, compiledChunk, generator.getDistanceSq()));
                    }
                }
            }
            else if (generatorType == ChunkCompileTaskGenerator.Type.RESORT_TRANSPARENCY)
            {
                BufferBuilder buffer = generator.getRegionRenderCacheBuilder().getWorldRendererByLayer(BlockRenderLayer.TRANSLUCENT);
                futuresList.add(this.chunkRenderDispatcher.uploadChunk(generator, BlockRenderLayer.TRANSLUCENT, buffer, renderChunk, compiledChunk, generator.getDistanceSq()));
            }

            final ListenableFuture<List<Object>> listenablefuture = Futures.allAsList(futuresList);

            generator.addFinishRunnable(new Runnable()
            {
                @Override
                public void run()
                {
                    listenablefuture.cancel(false);
                }
            });

            Futures.addCallback(listenablefuture, new FutureCallback<List<Object>>()
            {
                @Override
                public void onSuccess(@Nullable List<Object> list)
                {
                    ChunkRenderWorkerLitematica.this.freeRenderBuilder(generator);

                    generator.getLock().lock();

                    label49:
                    {
                        try
                        {
                            if (generator.getStatus() == ChunkCompileTaskGenerator.Status.UPLOADING)
                            {
                                generator.setStatus(ChunkCompileTaskGenerator.Status.DONE);
                                break label49;
                            }

                            if (generator.isFinished() == false)
                            {
                                ChunkRenderWorkerLitematica.LOGGER.warn("Chunk render task was {} when I expected it to be uploading; aborting task", (Object)generator.getStatus());
                            }
                        }
                        finally
                        {
                            generator.getLock().unlock();
                        }

                        return;
                    }

                    generator.getRenderChunk().setCompiledChunk(compiledChunk);
                }

                @Override
                public void onFailure(Throwable throwable)
                {
                    ChunkRenderWorkerLitematica.this.freeRenderBuilder(generator);

                    if ((throwable instanceof CancellationException) == false && (throwable instanceof InterruptedException) == false)
                    {
                        Minecraft.getMinecraft().crashed(CrashReport.makeCrashReport(throwable, "Rendering Litematica chunk"));
                    }
                }
            });
        }
    }

    private RegionRenderCacheBuilder getRegionRenderCacheBuilder() throws InterruptedException
    {
        return this.regionRenderCacheBuilder != null ? this.regionRenderCacheBuilder : this.chunkRenderDispatcher.allocateRenderBuilder();
    }

    private void freeRenderBuilder(ChunkCompileTaskGenerator generator)
    {
        if (this.regionRenderCacheBuilder == null)
        {
            this.chunkRenderDispatcher.freeRenderBuilder(generator.getRegionRenderCacheBuilder());
        }
    }

    public void notifyToStop()
    {
        this.shouldRun = false;
    }
}
