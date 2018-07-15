package fi.dy.masa.litematica.util;

import java.util.IdentityHashMap;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemUtils
{
    private static final IdentityHashMap<IBlockState, ItemStack> ITEMS_FOR_STATES = new IdentityHashMap<>();

    public static ItemStack getItemForState(IBlockState state)
    {
        ItemStack stack = ITEMS_FOR_STATES.get(state);
        return stack != null ? stack : ItemStack.EMPTY;
    }

    public static void setItemForBlock(World world, BlockPos pos, IBlockState state)
    {
        if (ITEMS_FOR_STATES.containsKey(state) == false)
        {
            ITEMS_FOR_STATES.put(state, getItemForBlock(world, pos, state));
        }
    }

    public static ItemStack getItemForBlock(World world, BlockPos pos, IBlockState state)
    {
        if (state.getMaterial() == Material.AIR)
        {
            return ItemStack.EMPTY;
        }

        ItemStack stack = ITEMS_FOR_STATES.get(state);

        if (stack != null)
        {
            return stack;
        }

        stack = state.getBlock().getItem(world, pos, state);

        if (stack.isEmpty())
        {
            stack = ItemStack.EMPTY;
        }
        else if (state.getBlock() instanceof BlockSlab && ((BlockSlab) state.getBlock()).isDouble())
        {
            stack.setCount(2);
        }

        ITEMS_FOR_STATES.put(state, stack);

        return stack;
    }
}
