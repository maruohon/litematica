package fi.dy.masa.litematica.util;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class ToolUtils
{
    public static void fillSelectionVolumes(Minecraft mc, IBlockState state, @Nullable IBlockState stateToReplace)
    {
        if (mc.player != null && mc.player.capabilities.isCreativeMode)
        {
            final AreaSelection area = DataManager.getSelectionManager().getCurrentSelection();

            if (area != null && area.getAllSubRegionBoxes().size() > 0)
            {
                Box currentBox = area.getSelectedSubRegionBox();
                final ImmutableList<Box> boxes = currentBox != null ? ImmutableList.of(currentBox) : ImmutableList.copyOf(area.getAllSubRegionBoxes());

                if (mc.isSingleplayer())
                {
                    final int dimId = fi.dy.masa.malilib.util.WorldUtils.getDimensionId(mc.player.getEntityWorld());
                    final WorldServer world = mc.getIntegratedServer().getWorld(dimId);

                    world.addScheduledTask(new Runnable()
                    {
                        public void run()
                        {
                            WorldUtils.setShouldPreventOnBlockAdded(true);

                            if (fillSelectionVolumesDirect(world, boxes, state, stateToReplace))
                            {
                                InfoUtils.showGuiOrActionBarMessage(MessageType.SUCCESS, "litematica.message.area_filled");
                            }
                            else
                            {
                                InfoUtils.showGuiOrActionBarMessage(MessageType.ERROR, "litematica.message.area_fill_fail");
                            }

                            WorldUtils.setShouldPreventOnBlockAdded(false);
                        }
                    });

                    InfoUtils.showGuiOrActionBarMessage(MessageType.INFO, "litematica.message.scheduled_task_added");
                }
                else if (fillSelectionVolumesCommand(boxes, state, stateToReplace, mc))
                {
                    InfoUtils.showGuiOrActionBarMessage(MessageType.SUCCESS, "litematica.message.area_filled");
                }
                else
                {
                    InfoUtils.showGuiOrActionBarMessage(MessageType.ERROR, "litematica.message.area_fill_fail");
                }
            }
            else
            {
                InfoUtils.showGuiOrActionBarMessage(MessageType.ERROR, "litematica.message.error.no_area_selected");
            }
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.generic.creative_mode_only");
        }
    }

    public static boolean fillSelectionVolumesDirect(World world, Collection<Box> boxes, IBlockState state, @Nullable IBlockState stateToReplace)
    {
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();

        for (Box box : boxes)
        {
            BlockPos posMin = PositionUtils.getMinCorner(box.getPos1(), box.getPos2());
            BlockPos posMax = PositionUtils.getMaxCorner(box.getPos1(), box.getPos2());

            for (int z = posMin.getZ(); z <= posMax.getZ(); ++z)
            {
                for (int x = posMin.getX(); x <= posMax.getX(); ++x)
                {
                    for (int y = posMax.getY(); y >= posMin.getY(); --y)
                    {
                        posMutable.setPos(x, y, z);

                        if (stateToReplace == null || world.getBlockState(posMutable) == stateToReplace)
                        {
                            TileEntity te = world.getTileEntity(posMutable);

                            if (te instanceof IInventory)
                            {
                                ((IInventory) te).clear();
                            }

                            world.setBlockState(posMutable, state, 0x12);
                        }
                    }
                }
            }
        }

        return true;
    }

    public static boolean fillSelectionVolumesCommand(Collection<Box> boxes, IBlockState state, @Nullable IBlockState stateToReplace, Minecraft mc)
    {
        ResourceLocation rl = Block.REGISTRY.getNameForObject(state.getBlock());

        if (rl == null)
        {
            return false;
        }

        String blockName = rl.toString();
        String strCommand = blockName + " " + String.valueOf(state.getBlock().getMetaFromState(state));

        if (stateToReplace != null)
        {
            rl = Block.REGISTRY.getNameForObject(stateToReplace.getBlock());

            if (rl == null)
            {
                return false;
            }

            strCommand += " replace " + rl.toString();
        }

        for (Box box : boxes)
        {
            BlockPos posMin = PositionUtils.getMinCorner(box.getPos1(), box.getPos2());
            BlockPos posMax = PositionUtils.getMaxCorner(box.getPos1(), box.getPos2());

            String cmd = String.format("/fill %d %d %d %d %d %d %s",
                    posMin.getX(), posMin.getY(), posMin.getZ(),
                    posMax.getX(), posMax.getY(), posMax.getZ(), strCommand);

            mc.player.sendChatMessage(cmd);
        }

        return true;
    }

    public static void killEntitiesCommand(Collection<Box> boxes, Minecraft mc)
    {
        for (Box box : boxes)
        {
            BlockPos posMin = PositionUtils.getMinCorner(box.getPos1(), box.getPos2());
            BlockPos posMax = PositionUtils.getMaxCorner(box.getPos1(), box.getPos2());

            String cmd = String.format("/kill @e[type=!player,x=%d,y=%d,z=%d,dx=%d,dy=%d,dz=%d]",
                    posMin.getX(), posMin.getY(), posMin.getZ(),
                    posMax.getX() - posMin.getX() + 1, posMax.getY() - posMin.getY() + 1, posMax.getZ() - posMin.getZ() + 1);

            mc.player.sendChatMessage(cmd);
        }
    }

    public static void deleteSelectionVolumes(boolean removeEntities, Minecraft mc)
    {
        deleteSelectionVolumes(DataManager.getSelectionManager().getCurrentSelection(), removeEntities, mc);
    }

    public static void deleteSelectionVolumes(@Nullable final AreaSelection area, boolean removeEntities, Minecraft mc)
    {
        if (mc.player != null && mc.player.capabilities.isCreativeMode)
        {
            if (area != null && area.getAllSubRegionBoxes().size() > 0)
            {
                Box currentBox = area.getSelectedSubRegionBox();
                final ImmutableList<Box> boxes = currentBox != null ? ImmutableList.of(currentBox) : ImmutableList.copyOf(area.getAllSubRegionBoxes());

                if (mc.isSingleplayer())
                {
                    final int dimId = fi.dy.masa.malilib.util.WorldUtils.getDimensionId(mc.player.getEntityWorld());
                    final WorldServer world = mc.getIntegratedServer().getWorld(dimId);

                    world.addScheduledTask(new Runnable()
                    {
                        public void run()
                        {
                            if (deleteSelectionVolumes(world, boxes, removeEntities))
                            {
                                InfoUtils.showGuiOrActionBarMessage(MessageType.SUCCESS, "litematica.message.area_cleared");
                            }
                            else
                            {
                                InfoUtils.showGuiOrActionBarMessage(MessageType.ERROR, "litematica.message.area_clear_fail");
                            }
                        }
                    });

                    InfoUtils.showGuiOrActionBarMessage(MessageType.INFO, "litematica.message.scheduled_task_added");
                }
                else if (fillSelectionVolumesCommand(boxes, Blocks.AIR.getDefaultState(), null, mc))
                {
                    killEntitiesCommand(boxes, mc);
                    InfoUtils.showGuiOrActionBarMessage(MessageType.SUCCESS, "litematica.message.area_cleared");
                }
                else
                {
                    InfoUtils.showGuiOrActionBarMessage(MessageType.ERROR, "litematica.message.area_clear_fail");
                }
            }
            else
            {
                InfoUtils.showGuiOrActionBarMessage(MessageType.ERROR, "litematica.message.error.no_area_selected");
            }
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.generic.creative_mode_only");
        }
    }

    public static boolean deleteSelectionVolumes(World world, Collection<Box> boxes, boolean removeEntities)
    {
        IBlockState air = Blocks.AIR.getDefaultState();
        IBlockState barrier = Blocks.BARRIER.getDefaultState();
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();

        for (Box box : boxes)
        {
            BlockPos posMin = PositionUtils.getMinCorner(box.getPos1(), box.getPos2());
            BlockPos posMax = PositionUtils.getMaxCorner(box.getPos1(), box.getPos2());

            for (int z = posMin.getZ(); z <= posMax.getZ(); ++z)
            {
                for (int x = posMin.getX(); x <= posMax.getX(); ++x)
                {
                    for (int y = posMax.getY(); y >= posMin.getY(); --y)
                    {
                        posMutable.setPos(x, y, z);
                        TileEntity te = world.getTileEntity(posMutable);

                        if (te instanceof IInventory)
                        {
                            ((IInventory) te).clear();
                            world.setBlockState(posMutable, barrier, 0x12);
                        }

                        world.setBlockState(posMutable, air, 0x12);
                    }
                }
            }

            if (removeEntities)
            {
                AxisAlignedBB bb = PositionUtils.createEnclosingAABB(posMin, posMax);
                List<Entity> entities = world.getEntitiesWithinAABB(Entity.class, bb);

                for (Entity entity : entities)
                {
                    if ((entity instanceof EntityPlayer) == false)
                    {
                        entity.setDead();
                    }
                }
            }
        }

        return true;
    }
}
