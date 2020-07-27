package fi.dy.masa.litematica.scheduler.tasks;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.selection.SelectionBox;
import fi.dy.masa.malilib.message.MessageType;
import fi.dy.masa.malilib.message.MessageUtils;
import fi.dy.masa.malilib.util.data.IntBoundingBox;

public class TaskUpdateBlocks extends TaskProcessChunkBase
{
    public TaskUpdateBlocks(List<SelectionBox> boxes)
    {
        super("litematica.gui.label.task_name.update_blocks");

        this.addBoxesPerChunks(boxes);
        this.updateInfoHudLinesMissingChunks(this.requiredChunks);
    }

    @Override
    public boolean canExecute()
    {
        return super.canExecute() && this.mc.isSingleplayer();
    }

    @Override
    protected boolean canProcessChunk(ChunkPos pos)
    {
        return this.mc.player != null && this.areSurroundingChunksLoaded(pos, this.worldClient, 1);
    }

    @Override
    protected boolean processChunk(ChunkPos pos)
    {
        for (IntBoundingBox box : this.getBoxesInChunk(pos))
        {
            this.updateBlocks(box, this.world);
        }

        return true;
    }

    protected void updateBlocks(IntBoundingBox box, World world)
    {
        for (int y = box.minY; y <= box.maxY; ++y)
        {
            for (int z = box.minZ; z <= box.maxZ; ++z)
            {
                for (int x = box.minX; x <= box.maxX; ++x)
                {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = world.getBlockState(pos).getBlock();
                    world.neighborChanged(pos, block, pos);
                }
            }
        }
    }

    @Override
    protected void onStop()
    {
        this.printCompletionMessage();
        InfoHud.getInstance().removeInfoHudRenderer(this, false);
        this.notifyListener();
    }

    protected void printCompletionMessage()
    {
        if (this.finished)
        {
            if (this.printCompletionMessage)
            {
                MessageUtils.showGuiMessage(MessageType.SUCCESS, "litematica.message.blocks_updated");
            }
        }
        else
        {
            MessageUtils.showGuiMessage(MessageType.ERROR, "litematica.message.error.update_blocks_aborted");
        }
    }
}
