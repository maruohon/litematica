package fi.dy.masa.litematica.materials;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.FileUtils;

public class MaterialCache
{
    private static final MaterialCache INSTANCE = new MaterialCache();

    protected final IdentityHashMap<BlockState, ItemStack> buildItemsForStates = new IdentityHashMap<>();
    protected final IdentityHashMap<BlockState, ItemStack> displayItemsForStates = new IdentityHashMap<>();
    protected final WorldSchematic tempWorld;
    protected final BlockPos checkPos;
    protected boolean hasReadFromFile;
    protected boolean dirty;

    private MaterialCache()
    {
        this.tempWorld = SchematicWorldHandler.createSchematicWorld();
        this.checkPos = new BlockPos(8, 0, 8);

        WorldUtils.loadChunksSchematicWorld(this.tempWorld, this.checkPos, new Vec3i(1, 1, 1));
    }

    public static MaterialCache getInstance()
    {
        if (INSTANCE.hasReadFromFile == false)
        {
            INSTANCE.readFromFile();
        }

        return INSTANCE;
    }

    public void clearCache()
    {
        this.buildItemsForStates.clear();
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

        this.dirty = true;

        return stack;
    }

    public boolean requiresMultipleItems(BlockState state)
    {
        Block block = state.getBlock();

        if (block instanceof FlowerPotBlock && block != Blocks.FLOWER_POT)
        {
            return true;
        }

        return false;
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
            block == Blocks.PISTON_HEAD ||
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
        if (state.getBlock() instanceof SlabBlock && state.get(SlabBlock.TYPE) == SlabType.DOUBLE)
        {
            stack.setCount(2);
        }
        else if (state.getBlock() == Blocks.SNOW)
        {
            stack.setCount(state.get(SnowBlock.LAYERS));
        }
    }

    protected NbtCompound writeToNBT()
    {
        NbtCompound nbt = new NbtCompound();

        nbt.put("MaterialCache", this.writeMapToNBT(this.buildItemsForStates));
        nbt.put("DisplayMaterialCache", this.writeMapToNBT(this.displayItemsForStates));

        return nbt;
    }

    protected NbtList writeMapToNBT(IdentityHashMap<BlockState, ItemStack> map)
    {
        NbtList list = new NbtList();

        for (Map.Entry<BlockState, ItemStack> entry : map.entrySet())
        {
            NbtCompound tag = new NbtCompound();
            NbtCompound stateTag = NbtHelper.fromBlockState(entry.getKey());

            tag.put("Block", stateTag);
            tag.put("Item", entry.getValue().writeNbt(new NbtCompound()));

            list.add(tag);
        }

        return list;
    }

    protected void readFromNBT(NbtCompound nbt)
    {
        this.buildItemsForStates.clear();
        this.displayItemsForStates.clear();

        this.readMapFromNBT(nbt, "MaterialCache", this.buildItemsForStates);
        this.readMapFromNBT(nbt, "DisplayMaterialCache", this.displayItemsForStates);
    }

    protected void readMapFromNBT(NbtCompound nbt, String tagName, IdentityHashMap<BlockState, ItemStack> map)
    {
        if (nbt.contains(tagName, Constants.NBT.TAG_LIST))
        {
            NbtList list = nbt.getList(tagName, Constants.NBT.TAG_COMPOUND);
            final int count = list.size();

            for (int i = 0; i < count; ++i)
            {
                NbtCompound tag = list.getCompound(i);

                if (tag.contains("Block", Constants.NBT.TAG_COMPOUND) &&
                    tag.contains("Item", Constants.NBT.TAG_COMPOUND))
                {
                    BlockState state = NbtHelper.toBlockState(tag.getCompound("Block"));

                    if (state != null)
                    {
                        ItemStack stack = ItemStack.fromNbt(tag.getCompound("Item"));
                        this.buildItemsForStates.put(state, stack);
                    }
                }
            }
        }
    }

    protected File getCacheDir()
    {
        return new File(FileUtils.getConfigDirectory(), Reference.MOD_ID);
    }

    protected File getCacheFile()
    {
        return new File(this.getCacheDir(), "material_cache.nbt");
    }

    public boolean writeToFile()
    {
        if (this.dirty == false)
        {
            return false;
        }

        File dir = this.getCacheDir();
        File file = this.getCacheFile();

        try
        {
            if (dir.exists() == false && dir.mkdirs() == false)
            {
                Litematica.logger.warn("Failed to write the material list cache to file '{}'", file.getAbsolutePath());
                return false;
            }

            FileOutputStream os = new FileOutputStream(file);
            NbtIo.writeCompressed(this.writeToNBT(), os);
            os.close();
            this.dirty = false;

            return true;
        }
        catch (Exception e)
        {
            Litematica.logger.warn("Failed to write the material list cache to file '{}'", file.getAbsolutePath(), e);
        }

        return false;
    }

    public void readFromFile()
    {
        File file = this.getCacheFile();

        if (file.exists() == false || file.canRead() == false)
        {
            return;
        }

        try
        {
            FileInputStream is = new FileInputStream(file);
            NbtCompound nbt = NbtIo.readCompressed(is);
            is.close();

            if (nbt != null)
            {
                this.readFromNBT(nbt);
                this.hasReadFromFile = true;
                this.dirty = false;
            }
        }
        catch (Exception e)
        {
            Litematica.logger.warn("Failed to read the material list cache from file '{}'", file.getAbsolutePath(), e);
        }
    }
}
