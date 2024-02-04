package litematica.scheduler.tasks;

import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;

import malilib.overlay.message.MessageDispatcher;
import malilib.util.game.wrap.GameWrap;
import malilib.util.game.wrap.RegistryUtils;
import malilib.util.position.BlockPos;
import malilib.util.position.ChunkPos;
import malilib.util.position.IntBoundingBox;
import litematica.render.infohud.InfoHud;
import litematica.selection.SelectionBox;
import litematica.util.EntityUtils;
import litematica.util.WorldUtils;

public class TaskFillArea extends TaskProcessChunkBase
{
    protected final IBlockState fillState;
    @Nullable protected final IBlockState replaceState;
    protected final String blockString;
    protected final boolean removeEntities;
    protected int chunkCount;

    public TaskFillArea(List<SelectionBox> boxes, IBlockState fillState, @Nullable IBlockState replaceState, boolean removeEntities)
    {
        this(boxes, fillState, replaceState, removeEntities, "litematica.gui.label.task_name.fill");
    }

    protected TaskFillArea(List<SelectionBox> boxes, IBlockState fillState, @Nullable IBlockState replaceState, boolean removeEntities, String nameOnHud)
    {
        super(nameOnHud);

        this.fillState = fillState;
        this.replaceState = replaceState;
        this.removeEntities = removeEntities;

        String id = RegistryUtils.getBlockIdStr(fillState.getBlock());
        String strName = null;

        if (id != null)
        {
            strName = id + " " + fillState.getBlock().getMetaFromState(fillState);

            if (replaceState != null)
            {
                id = RegistryUtils.getBlockIdStr(replaceState.getBlock());

                if (id != null)
                {
                    strName += " replace " + id;
                }
                else
                {
                    MessageDispatcher.error().translate("litematica.message.error.invalid_block", replaceState.toString());
                    strName = null;
                }
            }
        }
        else
        {
            MessageDispatcher.error().translate("litematica.message.error.invalid_block", fillState.toString());
        }

        this.blockString = strName;

        this.addPerChunkBoxes(boxes);
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
        return GameWrap.getClientPlayer() != null &&
               this.areSurroundingChunksLoaded(pos, this.worldClient, 1);
    }

    @Override
    protected boolean processChunk(ChunkPos pos)
    {
        if (this.isClientWorld && this.chunkCount == 0)
        {
            GameWrap.sendCommand("/gamerule sendCommandFeedback false");
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
            List<Entity> entities = this.world.getEntitiesInAABBexcluding(null, aabb, EntityUtils::testNotPlayer);
            entities.forEach(Entity::setDead);
        }

        try
        {
            WorldUtils.setShouldPreventBlockUpdates(this.world, true);

            IBlockState barrier = Blocks.BARRIER.getDefaultState();
            BlockPos.MutBlockPos posMutable = new BlockPos.MutBlockPos();

            for (int z = box.minZ; z <= box.maxZ; ++z)
            {
                for (int x = box.minX; x <= box.maxX; ++x)
                {
                    for (int y = box.maxY; y >= box.minY; --y)
                    {
                        posMutable.set(x, y, z);
                        IBlockState oldState = this.world.getBlockState(posMutable).getActualState(this.world, posMutable);

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
        finally
        {
            WorldUtils.setShouldPreventBlockUpdates(this.world, false);
        }
    }

    protected void fillBoxCommands(IntBoundingBox box, boolean removeEntities)
    {
        if (removeEntities)
        {
            AxisAlignedBB aabb = new AxisAlignedBB(box.minX, box.minY, box.minZ, box.maxX + 1, box.maxY + 1, box.maxZ + 1);

            if (this.world.getEntitiesInAABBexcluding(null, aabb, EntityUtils::testNotPlayer).size() > 0)
            {
                String killCmd = String.format("/kill @e[type=!player,x=%d,y=%d,z=%d,dx=%d,dy=%d,dz=%d]",
                        box.minX               , box.minY               , box.minZ,
                        box.maxX - box.minX + 1, box.maxY - box.minY + 1, box.maxZ - box.minZ + 1);

                GameWrap.sendCommand(killCmd);
            }
        }

        String fillCmd = String.format("/fill %d %d %d %d %d %d %s",
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, this.blockString);

        GameWrap.sendCommand(fillCmd);
    }

    @Override
    protected void onStop()
    {
        this.printCompletionMessage();

        if (this.isClientWorld)
        {
            GameWrap.sendCommand("/gamerule sendCommandFeedback true");
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
                MessageDispatcher.success().customHotbar().translate("litematica.message.area_filled");
            }
        }
        else
        {
            MessageDispatcher.error().translate("litematica.message.area_fill_fail");
        }
    }
}
