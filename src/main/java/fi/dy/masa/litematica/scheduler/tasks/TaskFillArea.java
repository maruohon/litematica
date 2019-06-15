package fi.dy.masa.litematica.scheduler.tasks;

import java.util.List;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.arguments.BlockStateParser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public class TaskFillArea extends TaskProcessChunkBase
{
    protected final IBlockState fillState;
    @Nullable protected final IBlockState replaceState;
    protected final String blockString;
    protected final boolean removeEntities;
    protected int chunkCount;

    public TaskFillArea(List<Box> boxes, IBlockState fillState, @Nullable IBlockState replaceState, boolean removeEntities)
    {
        this(boxes, fillState, replaceState, removeEntities, "litematica.gui.label.task_name.fill");
    }

    protected TaskFillArea(List<Box> boxes, IBlockState fillState, @Nullable IBlockState replaceState, boolean removeEntities, String nameOnHud)
    {
        super(nameOnHud);

        this.fillState = fillState;
        this.replaceState = replaceState;
        this.removeEntities = removeEntities;

        String blockString = BlockStateParser.toString(fillState, null);

        if (replaceState != null)
        {
            blockString += " replace " + BlockStateParser.toString(replaceState, null);
        }

        this.blockString = blockString;

        this.addBoxesPerChunks(boxes);
        this.updateInfoHudLinesMissingChunks(this.requiredChunks);
    }

    @Override
    public boolean canExecute()
    {
        return super.canExecute() && this.blockString != null;
    }

    @Override
    protected boolean canProcessChunk(ChunkPos pos)
    {
        return this.mc.player != null && this.areSurroundingChunksLoaded(pos, this.worldClient, 1);
    }

    @Override
    protected boolean processChunk(ChunkPos pos)
    {
        if (this.chunkCount == 0)
        {
            this.mc.player.sendChatMessage("/gamerule sendCommandFeedback false");
        }

        for (IntBoundingBox box : this.getBoxesInChunk(pos))
        {
            if (this.isClientWorld)
            {
                this.fillBoxCommands(box, this.removeEntities);
            }
            else
            {
                this.fillBoxDirect(box, this.removeEntities);
            }
        }

        this.chunkCount++;

        return true;
    }

    protected void fillBoxDirect(IntBoundingBox box, boolean removeEntities)
    {
        if (removeEntities)
        {
            AxisAlignedBB aabb = new AxisAlignedBB(box.minX, box.minY, box.minZ, box.maxX + 1, box.maxY + 1, box.maxZ + 1);
            List<Entity> entities = this.world.getEntitiesInAABBexcluding(this.mc.player, aabb, EntityUtils.NOT_PLAYER);

            for (Entity entity : entities)
            {
                if ((entity instanceof EntityPlayer) == false)
                {
                    entity.remove();
                }
            }
        }

        IBlockState barrier = Blocks.BARRIER.getDefaultState();
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();

        for (int z = box.minZ; z <= box.maxZ; ++z)
        {
            for (int x = box.minX; x <= box.maxX; ++x)
            {
                for (int y = box.maxY; y >= box.minY; --y)
                {
                    posMutable.setPos(x, y, z);
                    IBlockState oldState = this.world.getBlockState(posMutable);

                    if ((this.replaceState == null && oldState != this.fillState) || oldState == this.replaceState)
                    {
                        TileEntity te = this.world.getTileEntity(posMutable);

                        if (te instanceof IInventory)
                        {
                            ((IInventory) te).clear();
                            this.world.setBlockState(posMutable, barrier, 0x12);
                        }

                        this.world.setBlockState(posMutable, this.fillState, 0x12);
                    }
                }
            }
        }
    }

    protected void fillBoxCommands(IntBoundingBox box, boolean removeEntities)
    {
        if (removeEntities)
        {
            AxisAlignedBB aabb = new AxisAlignedBB(box.minX, box.minY, box.minZ, box.maxX + 1, box.maxY + 1, box.maxZ + 1);

            if (this.world.getEntitiesInAABBexcluding(this.mc.player, aabb, EntityUtils.NOT_PLAYER).size() > 0)
            {
                String killCmd = String.format("/kill @e[type=!player,x=%d,y=%d,z=%d,dx=%d,dy=%d,dz=%d]",
                        box.minX               , box.minY               , box.minZ,
                        box.maxX - box.minX + 1, box.maxY - box.minY + 1, box.maxZ - box.minZ + 1);

                this.mc.player.sendChatMessage(killCmd);
            }
        }

        String fillCmd = String.format("/fill %d %d %d %d %d %d %s",
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, this.blockString);

        this.mc.player.sendChatMessage(fillCmd);
    }

    @Override
    protected void onStop()
    {
        this.printCompletionMessage();

        if (this.isClientWorld && this.mc.player != null)
        {
            this.mc.player.sendChatMessage("/gamerule sendCommandFeedback true");
        }

        InfoHud.getInstance().removeInfoHudRenderer(this, false);

        this.notifyListener();
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
