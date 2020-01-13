package fi.dy.masa.litematica.util;

import java.util.IdentityHashMap;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.SlabType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public class ItemUtils
{
    private static final IdentityHashMap<BlockState, ItemStack> ITEMS_FOR_STATES = new IdentityHashMap<>();

    public static ItemStack getItemForState(BlockState state)
    {
        ItemStack stack = ITEMS_FOR_STATES.get(state);
        return stack != null ? stack : ItemStack.EMPTY;
    }

    public static void setItemForBlock(World world, BlockPos pos, BlockState state)
    {
        if (ITEMS_FOR_STATES.containsKey(state) == false)
        {
            ITEMS_FOR_STATES.put(state, getItemForBlock(world, pos, state, false));
        }
    }

    public static ItemStack getItemForBlock(World world, BlockPos pos, BlockState state, boolean checkCache)
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
            stack = state.getBlock().getPickStack(world, pos, state);
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

    public static ItemStack getStateToItemOverride(BlockState state)
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

    private static void overrideStackSize(BlockState state, ItemStack stack)
    {
        if (state.getBlock() instanceof SlabBlock && state.get(SlabBlock.TYPE) == SlabType.DOUBLE)
        {
            stack.setCount(2);
        }
    }

    public static ItemStack storeTEInStack(ItemStack stack, BlockEntity te)
    {
        CompoundTag nbt = te.toTag(new CompoundTag());

        if (nbt.contains("Owner") && stack.getItem() instanceof BlockItem &&
            ((BlockItem) stack.getItem()).getBlock() instanceof AbstractSkullBlock)
        {
            CompoundTag tagOwner = nbt.getCompound("Owner");
            CompoundTag tagSkull = new CompoundTag();

            tagSkull.put("SkullOwner", tagOwner);
            stack.setTag(tagSkull);

            return stack;
        }
        else
        {
            CompoundTag tagLore = new CompoundTag();
            ListTag tagList = new ListTag();

            tagList.add(StringTag.of("(+NBT)"));
            tagLore.put("Lore", tagList);
            stack.putSubTag("display", tagLore);
            stack.putSubTag("BlockEntityTag", nbt);

            return stack;
        }
    }

    public static String getStackString(ItemStack stack)
    {
        if (stack.isEmpty() == false)
        {
            Identifier rl = Registry.ITEM.getId(stack.getItem());

            return String.format("[%s - display: %s - NBT: %s] (%s)",
                    rl != null ? rl.toString() : "null", stack.getName().getString(),
                    stack.getTag() != null ? stack.getTag().toString() : "<no NBT>", stack.toString());
        }

        return "<empty>";
    }
}
