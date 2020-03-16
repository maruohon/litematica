package fi.dy.masa.litematica.util;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.materials.MaterialCache;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper.HitType;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.systems.BlockPlacementPositionHandler;

public class WorldUtils
{
    private static boolean preventBlockUpdates;

    public static boolean shouldPreventBlockUpdates()
    {
        return preventBlockUpdates;
    }

    public static void setShouldPreventBlockUpdates(boolean prevent)
    {
        preventBlockUpdates = prevent;
    }

    public static void loadChunksClientWorld(WorldClient world, BlockPos origin, Vec3i areaSize)
    {
        BlockPos posEnd = origin.add(PositionUtils.getRelativeEndPositionFromAreaSize(areaSize));
        BlockPos posMin = fi.dy.masa.malilib.util.PositionUtils.getMinCorner(origin, posEnd);
        BlockPos posMax = fi.dy.masa.malilib.util.PositionUtils.getMaxCorner(origin, posEnd);
        final int cxMin = posMin.getX() >> 4;
        final int czMin = posMin.getZ() >> 4;
        final int cxMax = posMax.getX() >> 4;
        final int czMax = posMax.getZ() >> 4;

        for (int cz = czMin; cz <= czMax; ++cz)
        {
            for (int cx = cxMin; cx <= cxMax; ++cx)
            {
                world.getChunkProvider().loadChunk(cx, cz);
            }
        }
    }

    public static void setToolModeBlockState(ToolMode mode, boolean primary, Minecraft mc)
    {
        IBlockState state = Blocks.AIR.getDefaultState();
        double reach = mc.playerController.getBlockReachDistance();
        Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        RayTraceWrapper wrapper = RayTraceUtils.getGenericTrace(mc.world, entity, reach, true);

        if (wrapper != null)
        {
            RayTraceResult trace = wrapper.getRayTraceResult();

            if (trace != null)
            {
                BlockPos pos = trace.getBlockPos();

                if (wrapper.getHitType() == HitType.SCHEMATIC_BLOCK)
                {
                    state = SchematicWorldHandler.getSchematicWorld().getBlockState(pos);
                }
                else if (wrapper.getHitType() == HitType.VANILLA)
                {
                    state = mc.world.getBlockState(pos).getActualState(mc.world, pos);
                }
            }
        }

        if (primary)
        {
            mode.setPrimaryBlock(state);
        }
        else
        {
            mode.setSecondaryBlock(state);
        }
    }

    /**
     * Does a ray trace to the schematic world, and returns either the closest or the furthest hit block.
     * @param adjacentOnly whether to only accept traced schematic world position that are adjacent to a client world block, ie. normally placeable
     * @param mc
     * @return true if the correct item was or is in the player's hand after the pick block
     */
    public static boolean pickBlockFirst(Minecraft mc)
    {
        double reach = mc.playerController.getBlockReachDistance();
        Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        BlockPos pos = RayTraceUtils.getSchematicWorldTraceIfClosest(mc.world, entity, reach);

        if (pos != null)
        {
            doPickBlockForPosition(pos, mc);
            return true;
        }

        return false;
    }

    public static boolean pickBlockLast(boolean adjacentOnly, Minecraft mc)
    {
        BlockPos pos = BlockPlacementPositionHandler.INSTANCE.getCurrentPlacementPosition();

        // No overrides by other mods
        if (pos == null)
        {
            double reach = mc.playerController.getBlockReachDistance();
            Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
            pos = RayTraceUtils.getPickBlockLastTrace(mc.world, entity, reach, adjacentOnly);
        }

        if (pos != null)
        {
            IBlockState state = mc.world.getBlockState(pos);

            if (state.getBlock().isReplaceable(mc.world, pos) || state.getMaterial().isReplaceable())
            {
                return doPickBlockForPosition(pos, mc);
            }
        }

        return false;
    }

    private static boolean doPickBlockForPosition(BlockPos pos, Minecraft mc)
    {
        World world = SchematicWorldHandler.getSchematicWorld();
        EntityPlayer player = mc.player;
        IBlockState state = world.getBlockState(pos);
        ItemStack stack = MaterialCache.getInstance().getRequiredBuildItemForState(state, world, pos);
        boolean ignoreNbt = Configs.Generic.PICK_BLOCK_IGNORE_NBT.getBooleanValue();
        boolean picked = false;

        if (stack.isEmpty() == false && EntityUtils.getUsedHandForItem(player, stack, ignoreNbt) == null)
        {
            if (mc.player.capabilities.isCreativeMode)
            {
                TileEntity te = world.getTileEntity(pos);

                // The creative mode pick block with NBT only works correctly
                // if the server world doesn't have a TileEntity in that position.
                // Otherwise it would try to write whatever that TE is into the picked ItemStack.
                if (GuiBase.isCtrlDown() && te != null && mc.world.isAirBlock(pos))
                {
                    stack = stack.copy();
                    ItemUtils.storeTEInStack(stack, te);
                }
            }

            picked = InventoryUtils.switchItemToHand(stack, ignoreNbt, mc);
        }

        EnumHand hand = EntityUtils.getUsedHandForItem(player, stack, ignoreNbt);

        if (hand != null)
        {
            fi.dy.masa.malilib.util.InventoryUtils.preRestockHand(player, hand, 6, true);
        }

        return picked;
    }

    public static void insertSignTextFromSchematic(TileEntitySign teClient)
    {
        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();

        if (worldSchematic != null)
        {
            TileEntity te = worldSchematic.getTileEntity(teClient.getPos());

            if (te instanceof TileEntitySign)
            {
                ITextComponent[] textSchematic = ((TileEntitySign) te).signText;
                ITextComponent[] textClient = teClient.signText;

                if (textClient != null && textSchematic != null)
                {
                    int size = Math.min(textSchematic.length, textClient.length);

                    for (int i = 0; i < size; ++i)
                    {
                        if (textSchematic[i] != null)
                        {
                            textClient[i] = textSchematic[i].createCopy();
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if the given one block thick slice has non-air blocks or not.
     * NOTE: The axis is the perpendicular axis (that goes through the plane).
     * @param axis
     * @param pos1
     * @param pos2
     * @return
     */
    public static boolean isSliceEmpty(World world, EnumFacing.Axis axis, BlockPos pos1, BlockPos pos2)
    {
        switch (axis)
        {
            case Z:
            {
                int x1 = Math.min(pos1.getX(), pos2.getX());
                int x2 = Math.max(pos1.getX(), pos2.getX());
                int y1 = Math.min(pos1.getY(), pos2.getY());
                int y2 = Math.max(pos1.getY(), pos2.getY());
                int z = pos1.getZ();
                int cxMin = (x1 >> 4);
                int cxMax = (x2 >> 4);

                for (int cx = cxMin; cx <= cxMax; ++cx)
                {
                    Chunk chunk = world.getChunk(cx, z >> 4);
                    int xMin = Math.max(x1,  cx << 4      );
                    int xMax = Math.min(x2, (cx << 4) + 15);
                    int yMax = Math.min(y2, chunk.getTopFilledSegment() + 15);

                    for (int x = xMin; x <= xMax; ++x)
                    {
                        for (int y = y1; y <= yMax; ++y)
                        {
                            if (chunk.getBlockState(x, y, z).getMaterial() != Material.AIR)
                            {
                                return false;
                            }
                        }
                    }
                }

                break;
            }

            case Y:
            {
                int x1 = Math.min(pos1.getX(), pos2.getX());
                int x2 = Math.max(pos1.getX(), pos2.getX());
                int y = pos1.getY();
                int z1 = Math.min(pos1.getZ(), pos2.getZ());
                int z2 = Math.max(pos1.getZ(), pos2.getZ());
                int cxMin = (x1 >> 4);
                int cxMax = (x2 >> 4);
                int czMin = (z1 >> 4);
                int czMax = (z2 >> 4);

                for (int cz = czMin; cz <= czMax; ++cz)
                {
                    for (int cx = cxMin; cx <= cxMax; ++cx)
                    {
                        Chunk chunk = world.getChunk(cx, cz);

                        if (y > chunk.getTopFilledSegment() + 15)
                        {
                            continue;
                        }

                        int xMin = Math.max(x1,  cx << 4      );
                        int xMax = Math.min(x2, (cx << 4) + 15);
                        int zMin = Math.max(z1,  cz << 4      );
                        int zMax = Math.min(z2, (cz << 4) + 15);

                        for (int z = zMin; z <= zMax; ++z)
                        {
                            for (int x = xMin; x <= xMax; ++x)
                            {
                                if (chunk.getBlockState(x, y, z).getMaterial() != Material.AIR)
                                {
                                    return false;
                                }
                            }
                        }
                    }
                }

                break;
            }

            case X:
            {
                int x = pos1.getX();
                int z1 = Math.min(pos1.getZ(), pos2.getZ());
                int z2 = Math.max(pos1.getZ(), pos2.getZ());
                int y1 = Math.min(pos1.getY(), pos2.getY());
                int y2 = Math.max(pos1.getY(), pos2.getY());
                int czMin = (z1 >> 4);
                int czMax = (z2 >> 4);

                for (int cz = czMin; cz <= czMax; ++cz)
                {
                    Chunk chunk = world.getChunk(x >> 4, cz);
                    int zMin = Math.max(z1,  cz << 4      );
                    int zMax = Math.min(z2, (cz << 4) + 15);
                    int yMax = Math.min(y2, chunk.getTopFilledSegment() + 15);

                    for (int z = zMin; z <= zMax; ++z)
                    {
                        for (int y = y1; y <= yMax; ++y)
                        {
                            if (chunk.getBlockState(x, y, z).getMaterial() != Material.AIR)
                            {
                                return false;
                            }
                        }
                    }
                }

                break;
            }
        }

        return true;
    }
}
