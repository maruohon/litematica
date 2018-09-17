package fi.dy.masa.litematica.render.schematic;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.primitives.Doubles;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import fi.dy.masa.litematica.LiteModLitematica;
import fi.dy.masa.litematica.interfaces.IRegionRenderCacheBuilder;
import fi.dy.masa.litematica.render.schematic.RenderChunkSchematicVbo.OverlayType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RegionRenderCacheBuilder;
import net.minecraft.client.renderer.VertexBufferUploader;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.MathHelper;

public class ChunkRenderDispatcherLitematica
{
    private static final Logger LOGGER = LiteModLitematica.logger;
    private static final ThreadFactory THREAD_FACTORY = (new ThreadFactoryBuilder()).setNameFormat("Litematica Chunk Batcher %d").setDaemon(true).build();

    private final List<Thread> listWorkerThreads = Lists.<Thread>newArrayList();
    private final List<ChunkRenderWorkerLitematica> listThreadedWorkers = new ArrayList<>();
    private final PriorityBlockingQueue<ChunkCompileTaskGenerator> queueChunkUpdates = Queues.newPriorityBlockingQueue();
    private final BlockingQueue<RegionRenderCacheBuilder> queueFreeRenderBuilders;
    private final WorldVertexBufferUploader worldVertexUploader = new WorldVertexBufferUploader();
    private final VertexBufferUploader vertexUploader = new VertexBufferUploader();
    private final Queue<ChunkRenderDispatcherLitematica.PendingUpload> queueChunkUploads = Queues.newPriorityQueue();
    private final ChunkRenderWorkerLitematica renderWorker;
    private final int countRenderBuilders;

    public ChunkRenderDispatcherLitematica()
    {
        int threadLimitMemory = Math.max(1, (int)((double)Runtime.getRuntime().maxMemory() * 0.3D) / 10485760);
        int threadLimitCPU = Math.max(1, MathHelper.clamp(Runtime.getRuntime().availableProcessors(), 1, threadLimitMemory / 5));
        this.countRenderBuilders = MathHelper.clamp(threadLimitCPU * 10, 1, threadLimitMemory);

        if (threadLimitCPU > 1)
        {
            for (int i = 0; i < threadLimitCPU; ++i)
            {
                ChunkRenderWorkerLitematica worker = new ChunkRenderWorkerLitematica(this);
                Thread thread = THREAD_FACTORY.newThread(worker);
                thread.start();
                this.listThreadedWorkers.add(worker);
                this.listWorkerThreads.add(thread);
            }
        }

        this.queueFreeRenderBuilders = Queues.newArrayBlockingQueue(this.countRenderBuilders);

        for (int i = 0; i < this.countRenderBuilders; ++i)
        {
            this.queueFreeRenderBuilders.add(new RegionRenderCacheBuilder());
        }

        this.renderWorker = new ChunkRenderWorkerLitematica(this, new RegionRenderCacheBuilder());
    }

    public String getDebugInfo()
    {
        return this.listWorkerThreads.isEmpty() ? String.format("pC: %03d, single-threaded", this.queueChunkUpdates.size()) : String.format("pC: %03d, pU: %1d, aB: %1d", this.queueChunkUpdates.size(), this.queueChunkUploads.size(), this.queueFreeRenderBuilders.size());
    }

    public boolean runChunkUploads(long finishTimeNano)
    {
        boolean ranTasks = false;

        while (true)
        {
            boolean processedTask = false;

            if (this.listWorkerThreads.isEmpty())
            {
                ChunkCompileTaskGenerator generator = this.queueChunkUpdates.poll();

                if (generator != null)
                {
                    try
                    {
                        this.renderWorker.processTask(generator);
                        processedTask = true;
                    }
                    catch (InterruptedException var8)
                    {
                        LOGGER.warn("Skipped task due to interrupt");
                    }
                }
            }

            synchronized (this.queueChunkUploads)
            {
                if (!this.queueChunkUploads.isEmpty())
                {
                    (this.queueChunkUploads.poll()).uploadTask.run();
                    processedTask = true;
                    ranTasks = true;
                }
            }

            if (finishTimeNano == 0L || processedTask == false || finishTimeNano < System.nanoTime())
            {
                break;
            }
        }

        return ranTasks;
    }

    public boolean updateChunkLater(RenderChunk chunkRenderer)
    {
        //if (GuiScreen.isCtrlKeyDown()) System.out.printf("updateChunkLater()\n");
        chunkRenderer.getLockCompileTask().lock();
        boolean flag1;

        try
        {
            final ChunkCompileTaskGenerator generator = chunkRenderer.makeCompileTaskChunk();

            generator.addFinishRunnable(new Runnable()
            {
                public void run()
                {
                    ChunkRenderDispatcherLitematica.this.queueChunkUpdates.remove(generator);
                }
            });

            boolean flag = this.queueChunkUpdates.offer(generator);

            if (!flag)
            {
                generator.finish();
            }

            flag1 = flag;
        }
        finally
        {
            chunkRenderer.getLockCompileTask().unlock();
        }

        return flag1;
    }

    public boolean updateChunkNow(RenderChunk chunkRenderer)
    {
        //if (GuiScreen.isCtrlKeyDown()) System.out.printf("updateChunkNow()\n");
        chunkRenderer.getLockCompileTask().lock();
        boolean flag;

        try
        {
            ChunkCompileTaskGenerator generator = chunkRenderer.makeCompileTaskChunk();

            try
            {
                this.renderWorker.processTask(generator);
            }
            catch (InterruptedException e)
            {
            }

            flag = true;
        }
        finally
        {
            chunkRenderer.getLockCompileTask().unlock();
        }

        return flag;
    }

    public void stopChunkUpdates()
    {
        this.clearChunkUpdates();
        List<RegionRenderCacheBuilder> list = Lists.<RegionRenderCacheBuilder>newArrayList();

        while (list.size() != this.countRenderBuilders)
        {
            this.runChunkUploads(Long.MAX_VALUE);

            try
            {
                list.add(this.allocateRenderBuilder());
            }
            catch (InterruptedException e)
            {
            }
        }

        this.queueFreeRenderBuilders.addAll(list);
    }

    public void freeRenderBuilder(RegionRenderCacheBuilder builderCache)
    {
        this.queueFreeRenderBuilders.add(builderCache);
    }

    public RegionRenderCacheBuilder allocateRenderBuilder() throws InterruptedException
    {
        return this.queueFreeRenderBuilders.take();
    }

    public ChunkCompileTaskGenerator getNextChunkUpdate() throws InterruptedException
    {
        return this.queueChunkUpdates.take();
    }

    public boolean updateTransparencyLater(RenderChunk renderChunk)
    {
        //if (GuiScreen.isCtrlKeyDown()) System.out.printf("updateTransparencyLater()\n");
        renderChunk.getLockCompileTask().lock();
        boolean flag;

        try
        {
            final ChunkCompileTaskGenerator chunkcompiletaskgenerator = renderChunk.makeCompileTaskTransparency();

            if (chunkcompiletaskgenerator == null)
            {
                flag = true;
                return flag;
            }

            chunkcompiletaskgenerator.addFinishRunnable(new Runnable()
            {
                @Override
                public void run()
                {
                    ChunkRenderDispatcherLitematica.this.queueChunkUpdates.remove(chunkcompiletaskgenerator);
                }
            });
            flag = this.queueChunkUpdates.offer(chunkcompiletaskgenerator);
        }
        finally
        {
            renderChunk.getLockCompileTask().unlock();
        }

        return flag;
    }

    public ListenableFuture<Object> uploadChunk(final ChunkCompileTaskGenerator generator, final BlockRenderLayer layer, final BufferBuilder buffer,
            final RenderChunkSchematicVbo renderChunk, final CompiledChunk compiledChunk, final double distanceSq)
    {
        if (Minecraft.getMinecraft().isCallingFromMinecraftThread())
        {
            //if (GuiScreen.isCtrlKeyDown()) System.out.printf("uploadChunk()\n");
            if ((renderChunk instanceof RenderChunkSchematicList) == false)
            {
                this.uploadVertexBuffer(buffer, renderChunk.getVertexBufferByLayer(layer.ordinal()));

                EnumSet<OverlayType> types = renderChunk.getOverlayTypes();

                if (layer != BlockRenderLayer.TRANSLUCENT && types.isEmpty() == false)
                {
                    for (OverlayType type : types)
                    {
                        //if (GuiScreen.isCtrlKeyDown()) System.out.printf("uploadChunk() overlay: %s\n", type);
                        BufferBuilder overlayBuffer = ((IRegionRenderCacheBuilder) generator.getRegionRenderCacheBuilder()).getOverlayBuffer(type);
                        this.uploadVertexBuffer(overlayBuffer, renderChunk.getOverlayVertexBuffer(type));
                    }
                }
            }
            else
            {
                this.uploadDisplayList(buffer, ((RenderChunkSchematicList) renderChunk).getDisplayList(layer, compiledChunk), renderChunk);

                EnumSet<OverlayType> types = renderChunk.getOverlayTypes();

                if (types.isEmpty() == false)
                {
                    for (OverlayType type : types)
                    {
                        BufferBuilder overlayBuffer = ((IRegionRenderCacheBuilder) generator.getRegionRenderCacheBuilder()).getOverlayBuffer(type);
                        this.uploadDisplayList(overlayBuffer, ((RenderChunkSchematicList) renderChunk).getOverlayDisplayList(type), renderChunk);
                    }
                }
            }

            buffer.setTranslation(0.0D, 0.0D, 0.0D);

            return Futures.<Object>immediateFuture(null);
        }
        else
        {
            ListenableFutureTask<Object> futureTask = ListenableFutureTask.<Object>create(new Runnable()
            {
                @Override
                public void run()
                {
                    ChunkRenderDispatcherLitematica.this.uploadChunk(generator, layer, buffer, renderChunk, compiledChunk, distanceSq);
                }
            }, null);

            synchronized (this.queueChunkUploads)
            {
                this.queueChunkUploads.add(new ChunkRenderDispatcherLitematica.PendingUpload(futureTask, distanceSq));
                return futureTask;
            }
        }
    }

    private void uploadDisplayList(BufferBuilder bufferBuilderIn, int list, RenderChunk chunkRenderer)
    {
        GlStateManager.glNewList(list, GL11.GL_COMPILE);
        GlStateManager.pushMatrix();
        chunkRenderer.multModelviewMatrix();
        this.worldVertexUploader.draw(bufferBuilderIn);
        GlStateManager.popMatrix();
        GlStateManager.glEndList();
    }

    private void uploadVertexBuffer(BufferBuilder bufferBuilder, VertexBuffer vertexBufferIn)
    {
        this.vertexUploader.setVertexBuffer(vertexBufferIn);
        this.vertexUploader.draw(bufferBuilder);
    }

    public void clearChunkUpdates()
    {
        while (this.queueChunkUpdates.isEmpty() == false)
        {
            ChunkCompileTaskGenerator generator = this.queueChunkUpdates.poll();

            if (generator != null)
            {
                generator.finish();
            }
        }
    }

    public boolean hasChunkUpdates()
    {
        return this.queueChunkUpdates.isEmpty() && this.queueChunkUploads.isEmpty();
    }

    public void stopWorkerThreads()
    {
        this.clearChunkUpdates();

        for (ChunkRenderWorkerLitematica worker : this.listThreadedWorkers)
        {
            worker.notifyToStop();
        }

        for (Thread thread : this.listWorkerThreads)
        {
            try
            {
                thread.interrupt();
                thread.join();
            }
            catch (InterruptedException interruptedexception)
            {
                LOGGER.warn("Interrupted whilst waiting for worker to die", (Throwable)interruptedexception);
            }
        }

        this.queueFreeRenderBuilders.clear();
    }

    public boolean hasNoFreeRenderBuilders()
    {
        return this.queueFreeRenderBuilders.isEmpty();
    }

    public static class PendingUpload implements Comparable<ChunkRenderDispatcherLitematica.PendingUpload>
    {
        private final ListenableFutureTask<Object> uploadTask;
        private final double distanceSq;

        public PendingUpload(ListenableFutureTask<Object> uploadTaskIn, double distanceSqIn)
        {
            this.uploadTask = uploadTaskIn;
            this.distanceSq = distanceSqIn;
        }

        public int compareTo(ChunkRenderDispatcherLitematica.PendingUpload other)
        {
            return Doubles.compare(this.distanceSq, other.distanceSq);
        }
    }
}
