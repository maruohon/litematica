package fi.dy.masa.litematica.util;

import java.util.IdentityHashMap;
import net.minecraft.block.BlockAbstractSkull;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.state.properties.SlabType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.IRegistry;
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
            ITEMS_FOR_STATES.put(state, getItemForBlock(world, pos, state, false));
        }
    }

    public static ItemStack getItemForBlock(World world, BlockPos pos, IBlockState state, boolean checkCache)
    {
        if (checkCache)
        {
            ItemStack stack = ITEMS_FOR_STATES.get(state);

            if (stack != null)
            {
                return stack;
            }
        }

        if (state.isAir())
        {
            return ItemStack.EMPTY;
        }

        ItemStack stack = getStateToItemOverride(state);

        if (stack.isEmpty())
        {
            stack = state.getBlock().getItem(world, pos, state);
        }

        if (stack.isEmpty())
        {
            stack = ItemStack.EMPTY;
        }
        else
        {
            overrideStackSize(state, stack);
        }

        ITEMS_FOR_STATES.put(state, stack);

        return stack;
    }

    public static ItemStack getStateToItemOverride(IBlockState state)
    {
        if (state.getBlock() == Blocks.LAVA)
        {
            return new ItemStack(Items.LAVA_BUCKET);
        }
        else if (state.getBlock() == Blocks.WATER)
        {
            return new ItemStack(Items.WATER_BUCKET);
        }

        return ItemStack.EMPTY;
    }

    private static void overrideStackSize(IBlockState state, ItemStack stack)
    {
        if (state.getBlock() instanceof BlockSlab && state.get(BlockSlab.TYPE) == SlabType.DOUBLE)
        {
            stack.setCount(2);
        }
    }

    public static ItemStack storeTEInStack(ItemStack stack, TileEntity te)
    {
        NBTTagCompound nbt = te.write(new NBTTagCompound());

        if (nbt.contains("Owner") && stack.getItem() instanceof ItemBlock &&
            ((ItemBlock) stack.getItem()).getBlock() instanceof BlockAbstractSkull)
        {
            NBTTagCompound tagOwner = nbt.getCompound("Owner");
            NBTTagCompound tagSkull = new NBTTagCompound();

            tagSkull.put("SkullOwner", tagOwner);
            stack.setTag(tagSkull);

            return stack;
        }
        else
        {
            NBTTagCompound tagLore = new NBTTagCompound();
            NBTTagList tagList = new NBTTagList();

            tagList.add(new NBTTagString("(+NBT)"));
            tagLore.put("Lore", tagList);
            stack.setTagInfo("display", tagLore);
            stack.setTagInfo("BlockEntityTag", nbt);

            return stack;
        }
    }

    public static String getStackString(ItemStack stack)
    {
        if (stack.isEmpty() == false)
        {
            ResourceLocation rl = IRegistry.ITEM.getKey(stack.getItem());

            return String.format("[%s - display: %s - NBT: %s] (%s)",
                    rl != null ? rl.toString() : "null", stack.getDisplayName().getString(),
                    stack.getTag() != null ? stack.getTag().toString() : "<no NBT>", stack.toString());
        }

        return "<empty>";
    }
}
