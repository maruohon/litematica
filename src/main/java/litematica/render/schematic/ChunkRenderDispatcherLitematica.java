package litematica.render.schematic;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.primitives.Doubles;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.BlockRenderLayer;

import malilib.render.buffer.VertexBuilder;
import malilib.util.MathUtils;
import malilib.util.game.wrap.RenderWrap;
import litematica.Litematica;
import litematica.render.schematic.RenderChunkSchematicVbo.OverlayRenderType;

public class ChunkRenderDispatcherLitematica
{
    private static final Logger LOGGER = Litematica.LOGGER;
    private static final ThreadFactory THREAD_FACTORY = (new ThreadFactoryBuilder()).setNameFormat("Litematica Chunk Batcher %d").setDaemon(true).build();

    private final List<Thread> listWorkerThreads = Lists.newArrayList();
    private final List<ChunkRenderWorkerLitematica> listThreadedWorkers = new ArrayList<>();
    private final PriorityBlockingQueue<ChunkCompileTaskGeneratorSchematic> queueChunkUpdates = Queues.newPriorityBlockingQueue();
    private final BlockingQueue<VertexBuilderCache> queueFreeRenderBuilders;
    private final Queue<ChunkRenderDispatcherLitematica.PendingUpload> queueChunkUploads = Queues.newPriorityQueue();
    private final ChunkRenderWorkerLitematica renderWorker;
    private final int countRenderBuilders;

    public ChunkRenderDispatcherLitematica()
    {
        int threadLimitMemory = Math.max(1, (int)((double)Runtime.getRuntime().maxMemory() * 0.15D) / 10485760);
        int threadLimitCPU = Math.max(1, MathUtils.clamp(Runtime.getRuntime().availableProcessors(), 1, threadLimitMemory / 5));
        this.countRenderBuilders = MathUtils.clamp(threadLimitCPU * 8, 1, threadLimitMemory);

        if (threadLimitCPU > 1)
        {
            Litematica.LOGGER.info("Creating {} render threads", threadLimitCPU);

            for (int i = 0; i < threadLimitCPU; ++i)
            {
                ChunkRenderWorkerLitematica worker = new ChunkRenderWorkerLitematica(this);
                Thread thread = THREAD_FACTORY.newThread(worker);
                thread.start();
                this.listThreadedWorkers.add(worker);
                this.listWorkerThreads.add(thread);
            }
        }

        Litematica.LOGGER.info("Using {} total BufferBuilder caches", this.countRenderBuilders + 1);

        this.queueFreeRenderBuilders = Queues.newArrayBlockingQueue(this.countRenderBuilders);

        for (int i = 0; i < this.countRenderBuilders; ++i)
        {
            this.queueFreeRenderBuilders.add(new VertexBuilderCache());
        }

        this.renderWorker = new ChunkRenderWorkerLitematica(this, new VertexBuilderCache());
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
                ChunkCompileTaskGeneratorSchematic generator = this.queueChunkUpdates.poll();

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

    public boolean updateChunkLater(RenderChunkSchematicVbo renderChunk)
    {
        //if (GuiBase.isCtrlDown()) System.out.printf("updateChunkLater()\n");
        renderChunk.getLockCompileTask().lock();
        boolean flag1;

        try
        {
            final ChunkCompileTaskGeneratorSchematic generator = renderChunk.makeCompileTaskChunkSchematic();

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
            renderChunk.getLockCompileTask().unlock();
        }

        return flag1;
    }

    public boolean updateChunkNow(RenderChunkSchematicVbo chunkRenderer)
    {
        //if (GuiBase.isCtrlDown()) System.out.printf("updateChunkNow()\n");
        chunkRenderer.getLockCompileTask().lock();
        boolean flag;

        try
        {
            ChunkCompileTaskGeneratorSchematic generator = chunkRenderer.makeCompileTaskChunkSchematic();

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
        List<VertexBuilderCache> list = new ArrayList<>();

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

    public void freeRenderBuilder(VertexBuilderCache builderCache)
    {
        this.queueFreeRenderBuilders.add(builderCache);
    }

    public VertexBuilderCache allocateRenderBuilder() throws InterruptedException
    {
        return this.queueFreeRenderBuilders.take();
    }

    public ChunkCompileTaskGeneratorSchematic getNextChunkUpdate() throws InterruptedException
    {
        return this.queueChunkUpdates.take();
    }

    public boolean updateTransparencyLater(RenderChunkSchematicVbo renderChunk)
    {
        //if (GuiBase.isCtrlDown()) System.out.printf("updateTransparencyLater()\n");
        renderChunk.getLockCompileTask().lock();
        boolean flag;

        try
        {
            final ChunkCompileTaskGeneratorSchematic generator = renderChunk.makeCompileTaskTransparencySchematic();

            if (generator == null)
            {
                flag = true;
                return flag;
            }

            generator.addFinishRunnable(new Runnable()
            {
                @Override
                public void run()
                {
                    ChunkRenderDispatcherLitematica.this.queueChunkUpdates.remove(generator);
                }
            });
            flag = this.queueChunkUpdates.offer(generator);
        }
        finally
        {
            renderChunk.getLockCompileTask().unlock();
        }

        return flag;
    }

    public ListenableFuture<Object> uploadChunkBlocks(final BlockRenderLayer layer, final VertexBuilder builder,
            final RenderChunkSchematicVbo renderChunk, final CompiledChunk compiledChunk, final double distanceSq)
    {
        if (Minecraft.getMinecraft().isCallingFromMinecraftThread())
        {
            //if (GuiBase.isCtrlDown()) System.out.printf("uploadChunkBlocks()\n");
            if (RenderWrap.useVbo())
            {
                this.uploadVertexBuffer(builder, renderChunk.getVertexBufferByLayer(layer.ordinal()));
            }
            else
            {
                this.uploadDisplayList(builder, ((RenderChunkSchematicList) renderChunk).getDisplayList(layer, compiledChunk), renderChunk);
            }

            return Futures.immediateFuture(null);
        }
        else
        {
            ListenableFutureTask<Object> futureTask = ListenableFutureTask.<Object>create(new Runnable()
            {
                @Override
                public void run()
                {
                    ChunkRenderDispatcherLitematica.this.uploadChunkBlocks(layer, builder, renderChunk, compiledChunk, distanceSq);
                }
            }, null);

            synchronized (this.queueChunkUploads)
            {
                this.queueChunkUploads.add(new ChunkRenderDispatcherLitematica.PendingUpload(futureTask, distanceSq));
                return futureTask;
            }
        }
    }

    public ListenableFuture<Object> uploadChunkOverlay(final OverlayRenderType type, final VertexBuilder builder,
            final RenderChunkSchematicVbo renderChunk, final CompiledChunkSchematic compiledChunk, final double distanceSq)
    {
        if (Minecraft.getMinecraft().isCallingFromMinecraftThread())
        {
            //if (GuiBase.isCtrlDown()) System.out.printf("uploadChunkOverlay()\n");
            if (RenderWrap.useVbo())
            {
                this.uploadVertexBuffer(builder, renderChunk.getOverlayVertexBuffer(type));
            }
            else
            {
                this.uploadDisplayList(builder, ((RenderChunkSchematicList) renderChunk).getOverlayDisplayList(type, compiledChunk), renderChunk);
            }

            return Futures.immediateFuture(null);
        }
        else
        {
            ListenableFutureTask<Object> futureTask = ListenableFutureTask.create(new Runnable()
            {
                @Override
                public void run()
                {
                    ChunkRenderDispatcherLitematica.this.uploadChunkOverlay(type, builder, renderChunk, compiledChunk, distanceSq);
                }
            }, null);

            synchronized (this.queueChunkUploads)
            {
                this.queueChunkUploads.add(new ChunkRenderDispatcherLitematica.PendingUpload(futureTask, distanceSq));
                return futureTask;
            }
        }
    }

    private void uploadDisplayList(VertexBuilder builder, int list, RenderChunk renderChunk)
    {
        /*
        GlStateManager.glNewList(list, GL11.GL_COMPILE);
        GlStateManager.pushMatrix();

        //chunkRenderer.multModelviewMatrix();
        this.displayListUploader.draw(builder);

        GlStateManager.popMatrix();
        GlStateManager.glEndList();
        */
    }

    private void uploadVertexBuffer(VertexBuilder builder, VertexBuffer vertexBuffer)
    {
        builder.reset();
        vertexBuffer.bufferData(builder.getByteBuffer());
    }

    public void clearChunkUpdates()
    {
        while (this.queueChunkUpdates.isEmpty() == false)
        {
            ChunkCompileTaskGeneratorSchematic generator = this.queueChunkUpdates.poll();

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
