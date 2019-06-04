package fi.dy.masa.litematica.materials;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.FileUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.block.BlockFlowingFluid;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockSnowLayer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.state.properties.BedPart;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.dimension.DimensionType;

public class MaterialCache
{
    private static final MaterialCache INSTANCE = new MaterialCache();

    protected final IdentityHashMap<IBlockState, ItemStack> itemsForStates = new IdentityHashMap<>();
    protected final WorldSchematic tempWorld;
    protected final BlockPos checkPos;
    protected boolean hasReadFromFile;
    protected boolean dirty;

    private MaterialCache()
    {
        WorldSettings settings = new WorldSettings(0L, GameType.CREATIVE, false, false, WorldType.FLAT);

        this.tempWorld = new WorldSchematic(null, settings, DimensionType.NETHER, EnumDifficulty.PEACEFUL, Minecraft.getInstance().profiler);
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
        this.itemsForStates.clear();
    }

    public ItemStack getItemForState(IBlockState state)
    {
        return this.getItemForState(state, this.tempWorld, this.checkPos);
    }

    public ItemStack getItemForState(IBlockState state, World world, BlockPos pos)
    {
        ItemStack stack = this.itemsForStates.get(state);

        if (stack == null)
        {
            stack = this.getItemForStateFromWorld(state, world, pos);
        }

        return stack;
    }

    protected ItemStack getItemForStateFromWorld(IBlockState state, World world, BlockPos pos)
    {
        ItemStack stack = this.getStateToItemOverride(state);

        if (stack == null)
        {
            world.setBlockState(pos, state, 0x14);
            stack = state.getBlock().getItem(world, pos, state);
        }

        if (stack == null || stack.isEmpty())
        {
            stack = ItemStack.EMPTY;
        }
        else
        {
            this.overrideStackSize(state, stack);
        }

        this.itemsForStates.put(state, stack);
        this.dirty = true;

        return stack;
    }

    public boolean requiresMultipleItems(IBlockState state)
    {
        Block block = state.getBlock();

        if (block instanceof BlockFlowerPot && block != Blocks.FLOWER_POT)
        {
            return true;
        }

        return false;
    }

    public ImmutableList<ItemStack> getItems(IBlockState state)
    {
        return this.getItems(state, this.tempWorld, this.checkPos);
    }

    public ImmutableList<ItemStack> getItems(IBlockState state, World world, BlockPos pos)
    {
        Block block = state.getBlock();

        if (block instanceof BlockFlowerPot && block != Blocks.FLOWER_POT)
        {
            return ImmutableList.of(new ItemStack(Blocks.FLOWER_POT), block.getItem(world, pos, state));
        }

        return ImmutableList.of(this.getItemForState(state, world, pos));
    }

    @Nullable
    protected ItemStack getStateToItemOverride(IBlockState state)
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
            if (state.get(BlockFlowingFluid.LEVEL) == 0)
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
            if (state.get(BlockFlowingFluid.LEVEL) == 0)
            {
                return new ItemStack(Items.WATER_BUCKET);
            }
            else
            {
                return ItemStack.EMPTY;
            }
        }
        else if (block instanceof BlockDoor && state.get(BlockDoor.HALF) == DoubleBlockHalf.UPPER)
        {
            return ItemStack.EMPTY;
        }
        else if (block instanceof BlockBed && state.get(BlockBed.PART) == BedPart.HEAD)
        {
            return ItemStack.EMPTY;
        }
        else if (block instanceof BlockDoublePlant && state.get(BlockDoublePlant.HALF) == DoubleBlockHalf.UPPER)
        {
            return ItemStack.EMPTY;
        }

        return null;
    }

    protected void overrideStackSize(IBlockState state, ItemStack stack)
    {
        if (state.getBlock() instanceof BlockSlab && state.get(BlockSlab.TYPE) == SlabType.DOUBLE)
        {
            stack.setCount(2);
        }
        else if (state.getBlock() == Blocks.SNOW)
        {
            stack.setCount(state.get(BlockSnowLayer.LAYERS));
        }
    }

    protected NBTTagCompound writeToNBT()
    {
        NBTTagCompound nbt = new NBTTagCompound();
        NBTTagList list = new NBTTagList();

        for (Map.Entry<IBlockState, ItemStack> entry : this.itemsForStates.entrySet())
        {
            NBTTagCompound tag = new NBTTagCompound();
            NBTTagCompound stateTag = NBTUtil.writeBlockState(entry.getKey());

            tag.put("Block", stateTag);
            tag.put("Item", entry.getValue().write(new NBTTagCompound()));

            list.add(tag);
        }

        nbt.put("MaterialCache", list);

        return nbt;
    }

    protected boolean readFromNBT(NBTTagCompound nbt)
    {
        this.itemsForStates.clear();

        if (nbt.contains("MaterialCache", Constants.NBT.TAG_LIST))
        {
            NBTTagList list = nbt.getList("MaterialCache", Constants.NBT.TAG_COMPOUND);
            final int count = list.size();

            for (int i = 0; i < count; ++i)
            {
                NBTTagCompound tag = list.getCompound(i);

                if (tag.contains("Block", Constants.NBT.TAG_COMPOUND) &&
                    tag.contains("Item", Constants.NBT.TAG_COMPOUND))
                {
                    IBlockState state = NBTUtil.readBlockState(tag.getCompound("Block"));

                    if (state != null)
                    {
                        ItemStack stack = ItemStack.read(tag.getCompound("Item"));
                        this.itemsForStates.put(state, stack);
                    }
                }
            }

            return true;
        }

        return false;
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
            CompressedStreamTools.writeCompressed(this.writeToNBT(), os);
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
            NBTTagCompound nbt = CompressedStreamTools.readCompressed(is);
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
