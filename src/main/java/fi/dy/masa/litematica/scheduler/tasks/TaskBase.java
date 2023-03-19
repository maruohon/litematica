package fi.dy.masa.litematica.scheduler.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.interfaces.ICompletionListener;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.render.infohud.IInfoHudRenderer;
import fi.dy.masa.litematica.render.infohud.RenderPhase;
import fi.dy.masa.litematica.scheduler.ITask;
import fi.dy.masa.litematica.scheduler.TaskTimer;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.WorldUtils;

public abstract class TaskBase implements ITask, IInfoHudRenderer
{
    protected final List<String> infoHudLines = new ArrayList<>();
    protected final MinecraftClient mc;
    protected String name = "";
    private TaskTimer timer = new TaskTimer(1);
    @Nullable private ICompletionListener completionListener;
    protected boolean finished;
    protected boolean printCompletionMessage = true;

    protected TaskBase()
    {
        this.mc = MinecraftClient.getInstance();
    }

    @Override
    public TaskTimer getTimer()
    {
        return this.timer;
    }

    @Override
    public String getDisplayName()
    {
        return this.name;
    }

    @Override
    public void createTimer(int interval)
    {
        this.timer = new TaskTimer(interval);
    }

    public void disableCompletionMessage()
    {
        this.printCompletionMessage = false;
    }

    public void setCompletionListener(@Nullable ICompletionListener listener)
    {
        this.completionListener = listener;
    }

    @Override
    public boolean canExecute()
    {
        return this.isInWorld();
    }

    @Override
    public boolean shouldRemove()
    {
        return this.canExecute() == false;
    }

    @Override
    public void init()
    {
    }

    @Override
    public void stop()
    {
        this.notifyListener();
    }

    protected boolean isInWorld()
    {
        return this.mc.world != null && this.mc.player != null;
    }

    protected void notifyListener()
    {
        if (this.completionListener != null)
        {
            this.mc.execute(() ->
            {
                if (this.finished)
                {
                    this.completionListener.onTaskCompleted();
                }
                else
                {
                    this.completionListener.onTaskAborted();
                }
            });
        }
    }

    protected boolean areSurroundingChunksLoaded(ChunkPos pos, ClientWorld world, int radius)
    {
        if (radius <= 0)
        {
            return WorldUtils.isClientChunkLoaded(world, pos.x, pos.z);
        }

        int chunkX = pos.x;
        int chunkZ = pos.z;

        for (int cx = chunkX - radius; cx <= chunkX + radius; ++cx)
        {
            for (int cz = chunkZ - radius; cz <= chunkZ + radius; ++cz)
            {
                if (WorldUtils.isClientChunkLoaded(world, cx, cz) == false)
                {
                    return false;
                }
            }
        }

        return true;
    }

    protected void updateInfoHudLinesPendingChunks(Collection<ChunkPos> pendingChunks)
    {
        this.infoHudLines.clear();

        if (pendingChunks.isEmpty() == false)
        {
            // TODO
            List<ChunkPos> list = new ArrayList<>(pendingChunks);
            PositionUtils.CHUNK_POS_COMPARATOR.setReferencePosition(BlockPos.ofFloored(this.mc.player.getPos()));
            PositionUtils.CHUNK_POS_COMPARATOR.setClosestFirst(true);
            list.sort(PositionUtils.CHUNK_POS_COMPARATOR);

            String pre = GuiBase.TXT_WHITE + GuiBase.TXT_BOLD;
            String title = StringUtils.translate("litematica.gui.label.task.title.remaining_chunks", this.getDisplayName(), pendingChunks.size());
            this.infoHudLines.add(String.format("%s%s%s", pre, title, GuiBase.TXT_RST));

            int maxLines = Math.min(list.size(), Configs.InfoOverlays.INFO_HUD_MAX_LINES.getIntegerValue());

            for (int i = 0; i < maxLines; ++i)
            {
                ChunkPos pos = list.get(i);
                this.infoHudLines.add(String.format("cx: %5d, cz: %5d (x: %d, z: %d)", pos.x, pos.z, pos.x << 4, pos.z << 4));
            }
        }
    }

    @Override
    public boolean getShouldRenderText(RenderPhase phase)
    {
        return phase == RenderPhase.POST;
    }

    @Override
    public List<String> getText(RenderPhase phase)
    {
        return this.infoHudLines;
    }
}
