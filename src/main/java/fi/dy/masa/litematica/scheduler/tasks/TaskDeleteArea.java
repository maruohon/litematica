package fi.dy.masa.litematica.scheduler.tasks;

import java.util.List;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;

public class TaskDeleteArea extends TaskProcessChunkBase
{
    protected final boolean removeEntities;
    protected int chunkCount;

    public TaskDeleteArea(List<Box> boxes, boolean removeEntities)
    {
        super();

        this.removeEntities = removeEntities;

        this.addBoxesPerChunks(boxes);
        this.updateInfoHudLinesMissingChunks(this.requiredChunks);
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

        for (StructureBoundingBox box : this.getBoxesInChunk(pos))
        {
            if (this.isClientWorld)
            {
                this.deleteBoxCommands(box, this.removeEntities);
            }
            else
            {
                this.deleteBoxDirect(box, this.removeEntities);
            }
        }

        this.chunkCount++;

        return true;
    }

    protected void deleteBoxDirect(StructureBoundingBox box, boolean removeEntities)
    {
        IBlockState air = Blocks.AIR.getDefaultState();
        IBlockState barrier = Blocks.BARRIER.getDefaultState();
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();

        for (int z = box.minZ; z <= box.maxZ; ++z)
        {
            for (int x = box.minX; x <= box.maxX; ++x)
            {
                for (int y = box.maxY; y >= box.minY; --y)
                {
                    posMutable.setPos(x, y, z);
                    TileEntity te = this.world.getTileEntity(posMutable);

                    if (te instanceof IInventory)
                    {
                        ((IInventory) te).clear();
                        this.world.setBlockState(posMutable, barrier, 0x12);
                    }

                    this.world.setBlockState(posMutable, air, 0x12);
                }
            }
        }

        if (removeEntities)
        {
            AxisAlignedBB aabb = new AxisAlignedBB(box.minX, box.minY, box.minZ, box.maxX + 1, box.maxY + 1, box.maxZ + 1);
            List<Entity> entities = this.world.getEntitiesInAABBexcluding(this.mc.player, aabb, EntityUtils.NOT_PLAYER);

            for (Entity entity : entities)
            {
                if ((entity instanceof EntityPlayer) == false)
                {
                    entity.setDead();
                }
            }
        }
    }

    protected void deleteBoxCommands(StructureBoundingBox box, boolean removeEntities)
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

        String fillCmd = String.format("/fill %d %d %d %d %d %d minecraft:air",
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);

        this.mc.player.sendChatMessage(fillCmd);
    }

    @Override
    protected void onStop()
    {
        if (this.finished)
        {
            InfoUtils.showGuiMessage(MessageType.SUCCESS, "litematica.message.area_cleared");
        }
        else
        {
            InfoUtils.showGuiMessage(MessageType.ERROR, "litematica.message.error.area_deletion_aborted");
        }

        if (this.isClientWorld && this.mc.player != null)
        {
            this.mc.player.sendChatMessage("/gamerule sendCommandFeedback true");
        }

        InfoHud.getInstance().removeInfoHudRenderer(this, false);
    }
}
