package fi.dy.masa.litematica.scheduler.tasks;

import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;

public class TaskFillArea extends TaskProcessChunkMultiPhase
{
    protected final String fillCommand;
    protected final BlockState fillState;
    protected final String blockString;
    @Nullable protected final BlockState replaceState;
    @Nullable protected final String replaceBlockString;
    protected final int maxBoxVolume;
    protected final boolean removeEntities;

    public TaskFillArea(List<Box> boxes, BlockState fillState, @Nullable BlockState replaceState, boolean removeEntities)
    {
        this(boxes, fillState, replaceState, removeEntities, "litematica.gui.label.task_name.fill");
    }

    protected TaskFillArea(List<Box> boxes, BlockState fillState, @Nullable BlockState replaceState, boolean removeEntities, String nameOnHud)
    {
        super(nameOnHud);

        this.fillState = fillState;
        this.replaceState = replaceState;
        this.removeEntities = removeEntities;
        this.maxBoxVolume = Configs.Generic.COMMAND_FILL_MAX_VOLUME.getIntegerValue();
        this.maxCommandsPerTick = Configs.Generic.COMMAND_LIMIT.getIntegerValue();
        this.fillCommand = Configs.Generic.COMMAND_NAME_FILL.getStringValue();
        this.blockString = BlockArgumentParser.stringifyBlockState(fillState);

        if (replaceState != null)
        {
            this.replaceBlockString = BlockArgumentParser.stringifyBlockState(replaceState);
        }
        else
        {
            this.replaceBlockString = null;
        }

        this.processBoxBlocksTask = this::sendQueuedCommands;

        if (Configs.Generic.COMMAND_FILL_NO_CHUNK_CLAMP.getBooleanValue())
        {
            this.addNonChunkClampedBoxes(boxes);
        }
        else
        {
            this.addPerChunkBoxes(boxes);
        }
    }

    @Override
    public boolean canExecute()
    {
        return super.canExecute() && this.blockString != null;
    }

    @Override
    protected boolean canProcessChunk(ChunkPos pos)
    {
        return this.areSurroundingChunksLoaded(pos, this.clientWorld, 0);
    }

    @Override
    public void init()
    {
        super.init();

        if (this.useWorldEdit && this.isInWorld())
        {
            this.sendCommand("/perf neighbors off");
        }
    }

    @Override
    public boolean execute()
    {
        return this.executeMultiPhase();
    }

    @Override
    protected void onNextChunkFetched(ChunkPos pos)
    {
        if (this.isClientWorld)
        {
            this.queueCommandsForBoxesInChunk(pos);
        }
        else
        {
            this.directFillBoxesInChunk(pos);
        }
    }

    protected void queueCommandsForBoxesInChunk(ChunkPos pos)
    {
        for (IntBoundingBox box : this.getBoxesInChunk(pos))
        {
            this.queueFillCommandsForBox(box, this.removeEntities);
        }

        // Use this phase to send the queued commands
        this.phase = TaskPhase.PROCESS_BOX_BLOCKS;
    }

    protected void directFillBoxesInChunk(ChunkPos pos)
    {
        for (IntBoundingBox box : this.getBoxesInChunk(pos))
        {
            this.directFillBox(box, this.removeEntities);
        }

        this.finishProcessingChunk(pos);
    }

    protected void directFillBox(IntBoundingBox box, boolean removeEntities)
    {
        if (removeEntities)
        {
            directRemoveEntities(box, this.world);
        }

        WorldUtils.setShouldPreventBlockUpdates(this.world, true);

        BlockState barrier = Blocks.BARRIER.getDefaultState();
        BlockPos.Mutable posMutable = new BlockPos.Mutable();

        for (int z = box.minZ; z <= box.maxZ; ++z)
        {
            for (int x = box.minX; x <= box.maxX; ++x)
            {
                for (int y = box.maxY; y >= box.minY; --y)
                {
                    posMutable.set(x, y, z);
                    BlockState oldState = this.world.getBlockState(posMutable);

                    if ((this.replaceState == null && oldState != this.fillState) || oldState == this.replaceState)
                    {
                        BlockEntity te = this.world.getBlockEntity(posMutable);

                        if (te instanceof Inventory)
                        {
                            ((Inventory) te).clear();
                            this.world.setBlockState(posMutable, barrier, 0x32);
                        }

                        this.world.setBlockState(posMutable, this.fillState, 0x32);
                    }
                }
            }
        }

        WorldUtils.setShouldPreventBlockUpdates(this.world, false);
    }

    public static void directRemoveEntities(IntBoundingBox box, World world)
    {
        net.minecraft.util.math.Box aabb = new net.minecraft.util.math.Box(box.minX, box.minY, box.minZ, box.maxX + 1, box.maxY + 1, box.maxZ + 1);
        List<Entity> entities = world.getOtherEntities(null, aabb, EntityUtils.NOT_PLAYER);

        for (Entity entity : entities)
        {
            if ((entity instanceof PlayerEntity) == false)
            {
                entity.discard();
            }
        }
    }

    protected void queueFillCommandsForBox(IntBoundingBox box, boolean removeEntities)
    {
        if (removeEntities)
        {
            net.minecraft.util.math.Box aabb = new net.minecraft.util.math.Box(box.minX, box.minY, box.minZ, box.maxX + 1, box.maxY + 1, box.maxZ + 1);

            if (this.world.getOtherEntities(this.mc.player, aabb, EntityUtils.NOT_PLAYER).size() > 0)
            {
                String killCmd = String.format("kill @e[type=!player,x=%d,y=%d,z=%d,dx=%d,dy=%d,dz=%d]",
                        box.minX               , box.minY               , box.minZ,
                        box.maxX - box.minX + 1, box.maxY - box.minY + 1, box.maxZ - box.minZ + 1);

                this.queuedCommands.offer(killCmd);
            }
        }

        int totalVolume = (box.maxX - box.minX + 1) * (box.maxY - box.minY + 1) * (box.maxZ - box.minZ + 1);

        if (totalVolume <= this.maxBoxVolume || this.useWorldEdit)
        {
            this.queueFillCommandForBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        }
        else
        {
            int singleLayerVolume = (box.maxX - box.minX + 1) * (box.maxZ - box.minZ + 1);
            int singleBoxHeight = this.maxBoxVolume / singleLayerVolume;

            if (singleBoxHeight < 1)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "Error: Calculated single box height was less than 1 block");
                return;
            }

            for (int y = box.minY; y <= box.maxY; y += singleBoxHeight)
            {
                int maxY = Math.min(y + singleBoxHeight - 1, box.maxY);
                this.queueFillCommandForBox(box.minX, y, box.minZ, box.maxX, maxY, box.maxZ);
            }
        }
    }

    protected void queueFillCommandForBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ)
    {
        if (this.useWorldEdit)
        {
            this.queuedCommands.offer(String.format("/pos1 %d,%d,%d", minX, minY, minZ));
            this.queuedCommands.offer(String.format("/pos2 %d,%d,%d", maxX, maxY, maxZ));

            if (this.replaceState != null)
            {
                this.queuedCommands.offer(String.format("/replace %s %s", this.replaceBlockString, this.blockString));
            }
            else
            {
                this.queuedCommands.offer("/set " + this.blockString);
            }
        }
        else
        {
            String fillCmd;

            if (this.replaceState != null)
            {
                fillCmd = String.format("%s %d %d %d %d %d %d %s replace %s", this.fillCommand,
                                        minX, minY, minZ, maxX, maxY, maxZ,
                                        this.blockString, this.replaceBlockString);
            }
            else
            {
                fillCmd = String.format("%s %d %d %d %d %d %d %s", this.fillCommand,
                                        minX, minY, minZ, maxX, maxY, maxZ, this.blockString);
            }

            this.queuedCommands.offer(fillCmd);
        }
    }

    @Override
    protected void onStop()
    {
        this.printCompletionMessage();
        this.sendTaskEndCommands();

        DataManager.removeChatListener(this.gameRuleListener);
        InfoHud.getInstance().removeInfoHudRenderer(this, false);

        super.onStop();
    }

    protected void printCompletionMessage()
    {
        if (this.finished)
        {
            if (this.printCompletionMessage)
            {
                InfoUtils.showGuiMessage(MessageType.SUCCESS, "litematica.message.area_filled");
            }
        }
        else
        {
            InfoUtils.showGuiMessage(MessageType.ERROR, "litematica.message.area_fill_fail");
        }
    }
}
