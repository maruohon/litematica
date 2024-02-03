package litematica.util;

import java.util.IdentityHashMap;

import net.minecraft.block.BlockSlab;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import malilib.util.game.wrap.ItemWrap;
import malilib.util.game.wrap.NbtWrap;
import malilib.util.position.BlockPos;

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

        if (state.getMaterial() == Material.AIR)
        {
            return ItemStack.EMPTY;
        }

        ItemStack stack = getStateToItemOverride(state);

        if (ItemWrap.isEmpty(stack))
        {
            stack = state.getBlock().getItem(world, pos, state);
        }

        if (ItemWrap.isEmpty(stack))
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
        if (state.getBlock() == Blocks.LAVA || state.getBlock() == Blocks.FLOWING_LAVA)
        {
            return new ItemStack(Items.LAVA_BUCKET);
        }
        else if (state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.FLOWING_WATER)
        {
            return new ItemStack(Items.WATER_BUCKET);
        }

        return ItemStack.EMPTY;
    }

    private static void overrideStackSize(IBlockState state, ItemStack stack)
    {
        if (state.getBlock() instanceof BlockSlab && ((BlockSlab) state.getBlock()).isDouble())
        {
            stack.setCount(2);
        }
    }

    public static ItemStack storeBlockEntityInStack(ItemStack stack, TileEntity te)
    {
        NBTTagCompound nbt = te.writeToNBT(new NBTTagCompound());

        if (stack.getItem() == Items.SKULL && nbt.hasKey("Owner"))
        {
            NBTTagCompound tagOwner = NbtWrap.getCompound(nbt, "Owner");
            NBTTagCompound tagSkull = new NBTTagCompound();

            NbtWrap.putTag(tagSkull, "SkullOwner", tagOwner);
            ItemWrap.setTag(stack, tagSkull);

            return stack;
        }
        else
        {
            NBTTagCompound tagLore = new NBTTagCompound();
            NBTTagList tagList = new NBTTagList();

            NbtWrap.addTag(tagList, NbtWrap.asStringTag("(+NBT)"));
            NbtWrap.putTag(tagLore, "Lore", tagList);
            stack.setTagInfo("display", tagLore);
            stack.setTagInfo("BlockEntityTag", nbt);

            return stack;
        }
    }
}
