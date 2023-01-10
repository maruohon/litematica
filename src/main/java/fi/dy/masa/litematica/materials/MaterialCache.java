package fi.dy.masa.litematica.materials;

import java.util.IdentityHashMap;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.MultifaceGrowthBlock;
import net.minecraft.block.SeaPickleBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.TurtleEggBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

public class MaterialCache
{
    private static final MaterialCache INSTANCE = new MaterialCache();

    protected final IdentityHashMap<BlockState, ItemStack> buildItemsForStates = new IdentityHashMap<>();
    protected final IdentityHashMap<BlockState, ItemStack> displayItemsForStates = new IdentityHashMap<>();
    protected final WorldSchematic tempWorld;
    protected final BlockPos checkPos;

    private MaterialCache()
    {
        this.tempWorld = SchematicWorldHandler.createSchematicWorld(null);
        this.checkPos = new BlockPos(8, 0, 8);

        WorldUtils.loadChunksSchematicWorld(this.tempWorld, this.checkPos, new Vec3i(1, 1, 1));
    }

    public static MaterialCache getInstance()
    {
        return INSTANCE;
    }

    public void clearCache()
    {
        this.buildItemsForStates.clear();
        this.displayItemsForStates.clear();
    }

    public ItemStack getRequiredBuildItemForState(BlockState state)
    {
        return this.getRequiredBuildItemForState(state, this.tempWorld, this.checkPos);
    }

    public ItemStack getRequiredBuildItemForState(BlockState state, World world, BlockPos pos)
    {
        ItemStack stack = this.buildItemsForStates.get(state);

        if (stack == null)
        {
            stack = this.getItemForStateFromWorld(state, world, pos, true);
        }

        return stack;
    }

    public ItemStack getItemForDisplayNameForState(BlockState state)
    {
        ItemStack stack = this.displayItemsForStates.get(state);

        if (stack == null)
        {
            stack = this.getItemForStateFromWorld(state, this.tempWorld, this.checkPos, false);
        }

        return stack;
    }

    protected ItemStack getItemForStateFromWorld(BlockState state, World world, BlockPos pos, boolean isBuildItem)
    {
        ItemStack stack = isBuildItem ? this.getStateToItemOverride(state) : null;

        if (stack == null)
        {
            world.setBlockState(pos, state, 0x14);
            stack = state.getBlock().getPickStack(world, pos, state);
        }

        if (stack == null || stack.isEmpty())
        {
            stack = ItemStack.EMPTY;
        }
        else
        {
            this.overrideStackSize(state, stack);
        }

        if (isBuildItem)
        {
            this.buildItemsForStates.put(state, stack);
        }
        else
        {
            this.displayItemsForStates.put(state, stack);
        }

        return stack;
    }

    public boolean requiresMultipleItems(BlockState state)
    {
        Block block = state.getBlock();
        return block instanceof FlowerPotBlock && block != Blocks.FLOWER_POT;
    }

    public ImmutableList<ItemStack> getItems(BlockState state)
    {
        return this.getItems(state, this.tempWorld, this.checkPos);
    }

    public ImmutableList<ItemStack> getItems(BlockState state, World world, BlockPos pos)
    {
        Block block = state.getBlock();

        if (block instanceof FlowerPotBlock && block != Blocks.FLOWER_POT)
        {
            return ImmutableList.of(new ItemStack(Blocks.FLOWER_POT), block.getPickStack(world, pos, state));
        }

        return ImmutableList.of(this.getRequiredBuildItemForState(state, world, pos));
    }

    @Nullable
    protected ItemStack getStateToItemOverride(BlockState state)
    {
        Block block = state.getBlock();

        if (block == Blocks.PISTON_HEAD ||
            block == Blocks.MOVING_PISTON ||
            block == Blocks.NETHER_PORTAL ||
            block == Blocks.END_PORTAL ||
            block == Blocks.END_GATEWAY)
        {
            return ItemStack.EMPTY;
        }
        else if (block == Blocks.FARMLAND)
        {
            return new ItemStack(Blocks.DIRT);
        }
        else if (block == Blocks.BROWN_MUSHROOM_BLOCK)
        {
            return new ItemStack(Blocks.BROWN_MUSHROOM_BLOCK);
        }
        else if (block == Blocks.RED_MUSHROOM_BLOCK)
        {
            return new ItemStack(Blocks.RED_MUSHROOM_BLOCK);
        }
        else if (block == Blocks.LAVA)
        {
            if (state.get(FluidBlock.LEVEL) == 0)
            {
                return new ItemStack(Items.LAVA_BUCKET);
            }
            else
            {
                return ItemStack.EMPTY;
            }
        }
        else if (block == Blocks.WATER)
        {
            if (state.get(FluidBlock.LEVEL) == 0)
            {
                return new ItemStack(Items.WATER_BUCKET);
            }
            else
            {
                return ItemStack.EMPTY;
            }
        }
        else if (block instanceof DoorBlock && state.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER)
        {
            return ItemStack.EMPTY;
        }
        else if (block instanceof BedBlock && state.get(BedBlock.PART) == BedPart.HEAD)
        {
            return ItemStack.EMPTY;
        }
        else if (block instanceof TallPlantBlock && state.get(TallPlantBlock.HALF) == DoubleBlockHalf.UPPER)
        {
            return ItemStack.EMPTY;
        }

        return null;
    }

    protected void overrideStackSize(BlockState state, ItemStack stack)
    {
        Block block = state.getBlock();

        if (block instanceof SlabBlock && state.get(SlabBlock.TYPE) == SlabType.DOUBLE)
        {
            stack.setCount(2);
        }
        else if (block == Blocks.SNOW)
        {
            stack.setCount(state.get(SnowBlock.LAYERS));
        }
        else if (block instanceof TurtleEggBlock)
        {
            stack.setCount(state.get(TurtleEggBlock.EGGS));
        }
        else if (block instanceof SeaPickleBlock)
        {
            stack.setCount(state.get(SeaPickleBlock.PICKLES));
        }
        else if (block instanceof CandleBlock)
        {
            stack.setCount(state.get(CandleBlock.CANDLES));
        }
        else if (block instanceof MultifaceGrowthBlock)
        {
            stack.setCount(MultifaceGrowthBlock.collectDirections(state).size());
        }
    }
}
