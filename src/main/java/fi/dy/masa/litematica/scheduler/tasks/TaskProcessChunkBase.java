package fi.dy.masa.litematica.scheduler.tasks;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import com.google.common.collect.ArrayListMultimap;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.WorldUtils;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

public abstract class TaskProcessChunkBase extends TaskBase
{
    protected final ArrayListMultimap<ChunkPos, IntBoundingBox> boxesInChunks = ArrayListMultimap.create();
    protected final Set<ChunkPos> requiredChunks = new HashSet<>();
    protected final ClientWorld worldClient;
    protected final World world;
    protected final boolean isClientWorld;

    protected TaskProcessChunkBase(String nameOnHud)
    {
        this.worldClient = this.mc.world;
        this.world = WorldUtils.getBestWorld(this.mc);
        this.isClientWorld = (this.world == this.mc.world);
        this.name = StringUtils.translate(nameOnHud);

        InfoHud.getInstance().addInfoHudRenderer(this, true);
    }

    @Override
    public boolean execute()
    {
        if (this.worldClient != null)
        {
            Iterator<ChunkPos> iter = this.requiredChunks.iterator();
            int processed = 0;

            while (iter.hasNext())
            {
                ChunkPos pos = iter.next();

                if (this.canProcessChunk(pos))
                {
                    this.processChunk(pos);
                    iter.remove();
                    processed++;
                }
            }

            if (processed > 0)
            {
                this.updateInfoHudLinesMissingChunks(this.requiredChunks);
            }
        }

        this.finished = this.requiredChunks.isEmpty();

        return this.finished;
    }

    @Override
    public void stop()
    {
        // Multiplayer, just a client world
        if (this.isClientWorld)
        {
            this.onStop();
        }
        // Single player, saving from the integrated server world
        else
        {
            this.mc.execute(new Runnable()
            {
                @Override
                public void run()
                {
                    TaskProcessChunkBase.this.onStop();
                }
            });
        }
    }

    protected void onStop()
    {
        this.notifyListener();
    }

    protected abstract boolean canProcessChunk(ChunkPos pos);

    protected abstract boolean processChunk(ChunkPos pos);

    protected void addBoxesPerChunks(Collection<Box> allBoxes)
    {
        this.boxesInChunks.clear();
        this.requiredChunks.clear();

        this.requiredChunks.addAll(PositionUtils.getTouchedChunksForBoxes(allBoxes));

        for (ChunkPos pos : this.requiredChunks)
        {
            this.boxesInChunks.putAll(pos, PositionUtils.getBoxesWithinChunk(pos.x, pos.z, allBoxes));
        }
    }

    protected List<IntBoundingBox> getBoxesInChunk(ChunkPos pos)
    {
        return this.boxesInChunks.get(pos);
    }
}
