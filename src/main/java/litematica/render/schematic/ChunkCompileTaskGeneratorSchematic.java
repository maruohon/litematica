package litematica.render.schematic;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;

public class ChunkCompileTaskGeneratorSchematic implements Comparable<ChunkCompileTaskGeneratorSchematic>
{
    private final RenderChunkSchematicVbo renderChunk;
    private final ReentrantLock lock = new ReentrantLock();
    private final List<Runnable> listFinishRunnables = Lists.<Runnable>newArrayList();
    private final ChunkCompileTaskGeneratorSchematic.Type type;
    private final double distanceSq;
    private VertexBuilderCache vertexBuilderCache;
    private CompiledChunkSchematic compiledChunk;
    private ChunkCompileTaskGeneratorSchematic.Status status = ChunkCompileTaskGeneratorSchematic.Status.PENDING;
    private boolean finished;

    public ChunkCompileTaskGeneratorSchematic(RenderChunkSchematicVbo renderChunkIn, ChunkCompileTaskGeneratorSchematic.Type typeIn, double distanceSqIn)
    {
        this.renderChunk = renderChunkIn;
        this.type = typeIn;
        this.distanceSq = distanceSqIn;
    }

    public ChunkCompileTaskGeneratorSchematic.Status getStatus()
    {
        return this.status;
    }

    public RenderChunkSchematicVbo getRenderChunk()
    {
        return this.renderChunk;
    }

    public CompiledChunkSchematic getCompiledChunk()
    {
        return this.compiledChunk;
    }

    public void setCompiledChunk(CompiledChunkSchematic compiledChunkIn)
    {
        this.compiledChunk = compiledChunkIn;
    }

    public VertexBuilderCache getBufferCache()
    {
        return this.vertexBuilderCache;
    }

    public void setRegionRenderCacheBuilder(VertexBuilderCache cache)
    {
        this.vertexBuilderCache = cache;
    }

    public void setStatus(ChunkCompileTaskGeneratorSchematic.Status statusIn)
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
            if (this.type == ChunkCompileTaskGeneratorSchematic.Type.REBUILD_CHUNK && this.status != ChunkCompileTaskGeneratorSchematic.Status.DONE)
            {
                this.renderChunk.setNeedsUpdate(false);
            }

            this.finished = true;
            this.status = ChunkCompileTaskGeneratorSchematic.Status.DONE;

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

    public ChunkCompileTaskGeneratorSchematic.Type getType()
    {
        return this.type;
    }

    public boolean isFinished()
    {
        return this.finished;
    }

    public int compareTo(ChunkCompileTaskGeneratorSchematic other)
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
