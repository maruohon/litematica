package fi.dy.masa.litematica.render.schematic;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;

public class ChunkRenderTaskSchematic implements Comparable<ChunkRenderTaskSchematic>
{
    private final ChunkRendererSchematicVbo chunkRenderer;
    private final ChunkRenderTaskSchematic.Type type;
    private final List<Runnable> listFinishRunnables = Lists.<Runnable>newArrayList();
    private final ReentrantLock lock = new ReentrantLock();
    private final double distanceSq;
    private BufferBuilderCache bufferBuilderCache;
    private ChunkRenderDataSchematic chunkRenderData;
    private ChunkRenderTaskSchematic.Status status = ChunkRenderTaskSchematic.Status.PENDING;
    private boolean finished;

    public ChunkRenderTaskSchematic(ChunkRendererSchematicVbo renderChunkIn, ChunkRenderTaskSchematic.Type typeIn, double distanceSqIn)
    {
        this.chunkRenderer = renderChunkIn;
        this.type = typeIn;
        this.distanceSq = distanceSqIn;
    }

    public ChunkRenderTaskSchematic.Status getStatus()
    {
        return this.status;
    }

    public ChunkRendererSchematicVbo getRenderChunk()
    {
        return this.chunkRenderer;
    }

    public ChunkRenderDataSchematic getChunkRenderData()
    {
        return this.chunkRenderData;
    }

    public void setChunkRenderData(ChunkRenderDataSchematic chunkRenderData)
    {
        this.chunkRenderData = chunkRenderData;
    }

    public BufferBuilderCache getBufferCache()
    {
        return this.bufferBuilderCache;
    }

    public void setRegionRenderCacheBuilder(BufferBuilderCache cache)
    {
        this.bufferBuilderCache = cache;
    }

    public void setStatus(ChunkRenderTaskSchematic.Status statusIn)
    {
        this.lock.lock();

        try
        {
            this.status = statusIn;
        }
        finally
        {
            this.lock.unlock();
        }
    }

    public void finish()
    {
        this.lock.lock();

        try
        {
            if (this.type == ChunkRenderTaskSchematic.Type.REBUILD_CHUNK && this.status != ChunkRenderTaskSchematic.Status.DONE)
            {
                this.chunkRenderer.setNeedsUpdate(false);
            }

            this.finished = true;
            this.status = ChunkRenderTaskSchematic.Status.DONE;

            for (Runnable runnable : this.listFinishRunnables)
            {
                runnable.run();
            }
        }
        finally
        {
            this.lock.unlock();
        }
    }

    public void addFinishRunnable(Runnable runnable)
    {
        this.lock.lock();

        try
        {
            this.listFinishRunnables.add(runnable);

            if (this.finished)
            {
                runnable.run();
            }
        }
        finally
        {
            this.lock.unlock();
        }
    }

    public ReentrantLock getLock()
    {
        return this.lock;
    }

    public ChunkRenderTaskSchematic.Type getType()
    {
        return this.type;
    }

    public boolean isFinished()
    {
        return this.finished;
    }

    public int compareTo(ChunkRenderTaskSchematic other)
    {
        return Doubles.compare(this.distanceSq, other.distanceSq);
    }

    public double getDistanceSq()
    {
        return this.distanceSq;
    }

    public static enum Status
    {
        PENDING,
        COMPILING,
        UPLOADING,
        DONE;
    }

    public static enum Type
    {
        REBUILD_CHUNK,
        RESORT_TRANSPARENCY;
    }
}
