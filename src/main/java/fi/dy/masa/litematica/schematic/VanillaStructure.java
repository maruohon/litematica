package fi.dy.masa.litematica.schematic;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.malilib.overlay.message.MessageDispatcher;
import fi.dy.masa.malilib.util.nbt.NbtUtils;
import fi.dy.masa.malilib.util.wrap.NbtWrap;
import fi.dy.masa.litematica.schematic.container.ILitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.container.ILitematicaBlockStatePalette;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainerSparse;
import fi.dy.masa.litematica.schematic.container.VanillaStructurePalette;
import fi.dy.masa.litematica.util.PositionUtils;

public class VanillaStructure extends SingleRegionSchematic
{
    public static final String FILE_NAME_EXTENSION = ".nbt";

    VanillaStructure(File file)
    {
        super(file);
    }

    @Override
    public SchematicType<?> getType()
    {
        return SchematicType.VANILLA;
    }

    public static boolean isValidSchematic(NBTTagCompound tag)
    {
        if (NbtWrap.containsList(tag, "blocks") &&
            NbtWrap.containsList(tag, "palette") &&
            NbtWrap.containsList(tag, "size") &&
            NbtWrap.containsInt(tag, "DataVersion"))
        {
            return isSizeValid(readSizeFromTagImpl(tag));
        }

        return false;
    }

    @Override
    protected void createEmptyContainer(Vec3i size)
    {
        this.blockContainer = new LitematicaBlockStateContainerSparse(size);
    }

    @Override
    protected Class<? extends ILitematicaBlockStateContainer> getContainerClass()
    {
        return LitematicaBlockStateContainerSparse.class;
    }

    @Override
    @Nullable
    protected Vec3i readSizeFromTag(NBTTagCompound tag)
    {
        return readSizeFromTagImpl(tag);
    }

    @Nullable
    private static Vec3i readSizeFromTagImpl(NBTTagCompound tag)
    {
        return NbtUtils.readBlockPosFromListTag(tag, "size");
    }

    @Override
    protected boolean readBlocksFromTag(NBTTagCompound tag)
    {
        if (NbtWrap.containsList(tag, "palette") &&
            NbtWrap.containsList(tag, "blocks") &&
            isSizeValid(this.getSize()))
        {
            NBTTagList paletteTag = NbtWrap.getListOfCompounds(tag, "palette");
            LitematicaBlockStateContainerSparse container = (LitematicaBlockStateContainerSparse) this.blockContainer;
            ILitematicaBlockStatePalette palette = container.getPalette();

            if (readPaletteFromLitematicaFormatTag(paletteTag, palette) == false)
            {
                MessageDispatcher.error().translate("litematica.message.error.schematic_read.vanilla.failed_to_read_palette");
                return false;
            }

            if (NbtWrap.containsString(tag, "author"))
            {
                this.getMetadata().setAuthor(NbtWrap.getString(tag, "author"));
            }

            NBTTagList blockList = NbtWrap.getListOfCompounds(tag, "blocks");
            final int count = NbtWrap.getListSize(blockList);

            for (int i = 0; i < count; ++i)
            {
                NBTTagCompound blockTag = NbtWrap.getCompoundAt(blockList, i);
                BlockPos pos = NbtUtils.readBlockPosFromListTag(blockTag, "pos");

                if (pos == null)
                {
                    MessageDispatcher.error().translate("litematica.message.error.schematic_read.vanilla.failed_to_read_block_pos");
                    return false;
                }

                int id = NbtWrap.getInt(blockTag, "state");
                IBlockState state = palette.getBlockState(id);

                if (state == null)
                {
                    state = Blocks.AIR.getDefaultState();
                }

                container.setBlockState(pos.getX(), pos.getY(), pos.getZ(), state);

                if (NbtWrap.containsCompound(blockTag, "nbt"))
                {
                    this.blockEntities.put(pos, NbtWrap.getCompound(blockTag, "nbt"));
                }
            }

            return true;
        }

        return false;
    }

    @Override
    protected Map<BlockPos, NBTTagCompound> readBlockEntitiesFromTag(NBTTagCompound tag)
    {
        return ImmutableMap.of();
    }

    @Override
    protected List<EntityInfo> readEntitiesFromTag(NBTTagCompound tag)
    {
        List<EntityInfo> entities = new ArrayList<>();
        NBTTagList tagList = NbtWrap.getListOfCompounds(tag, "entities");
        final int size = NbtWrap.getListSize(tagList);

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound entityData = NbtWrap.getCompoundAt(tagList, i);
            Vec3d pos = NbtUtils.readVec3dFromListTag(entityData, "pos");

            if (pos != null && NbtWrap.containsCompound(entityData, "nbt"))
            {
                entities.add(new EntityInfo(pos, NbtWrap.getCompound(entityData, "nbt")));
            }
        }

        return entities;
    }

    protected void writeMetadataToTag(NBTTagCompound tag)
    {
        NbtWrap.putTag(tag, "Metadata", this.getMetadata().toTag());
        NbtWrap.putString(tag, "author", this.getMetadata().getAuthor());
    }

    protected void writeBlocksToTag(NBTTagCompound tag)
    {
        // Dummy resize handler, the hash map palette doesn't need to be re-created
        ILitematicaBlockStatePalette palette = new VanillaStructurePalette();
        NBTTagList blockList = new NBTTagList();

        if (this.blockContainer instanceof LitematicaBlockStateContainerSparse)
        {
            LitematicaBlockStateContainerSparse container = (LitematicaBlockStateContainerSparse) this.blockContainer;
            Long2ObjectOpenHashMap<IBlockState> blockMap = container.getBlockMap();

            blockMap.forEach((posLong, state) -> {
                long pos = posLong.longValue();
                this.writeBlockToList((int) (pos & 0xFFFF), (int) ((pos >>> 32) & 0xFFFF), (int) ((pos >> 16) & 0xFFFF), palette.idFor(state), blockList);
            });
        }
        else
        {
            ILitematicaBlockStateContainer container = this.blockContainer;
            Vec3i size = container.getSize();
            final int sizeX = size.getX();
            final int sizeY = size.getY();
            final int sizeZ = size.getZ();
            long volume = PositionUtils.getAreaVolume(size);
            IBlockState ignore = volume < 100000 ? null : Blocks.AIR.getDefaultState();

            for (int y = 0; y < sizeY; ++y)
            {
                for (int z = 0; z < sizeZ; ++z)
                {
                    for (int x = 0; x < sizeX; ++x)
                    {
                        IBlockState state = container.getBlockState(x, y, z);

                        if (state != ignore)
                        {
                            this.writeBlockToList(sizeX, y, z, palette.idFor(state), blockList);
                        }
                    }
                }
            }
        }

        NBTTagList paletteTag = this.writePaletteToLitematicaFormatTag(palette);

        NbtWrap.putTag(tag, "palette", paletteTag);
        NbtWrap.putTag(tag, "blocks", blockList);
    }

    private void writeBlockToList(int x, int y, int z, int id, NBTTagList blockList)
    {
        NBTTagCompound blockTag = new NBTTagCompound();
        BlockPos pos = new BlockPos(x, y, z);

        NbtUtils.writeBlockPosToListTag(pos, blockTag, "pos");
        NbtWrap.putInt(blockTag, "state", id);

        NBTTagCompound beTag = this.blockEntities.get(pos);

        if (beTag != null)
        {
            NbtWrap.putTag(blockTag, "nbt", beTag.copy());
        }

        NbtWrap.addTag(blockList, blockTag);
    }

    protected void writeEntitiesToTag(NBTTagCompound tag)
    {
        NBTTagList tagList = new NBTTagList();

        for (EntityInfo info : this.entities)
        {
            NBTTagCompound entityData = new NBTTagCompound();
            NbtUtils.writeVec3dToListTag(info.pos, entityData, "pos");
            NbtUtils.writeBlockPosToListTag(new BlockPos(info.pos), entityData, "blockPos");
            NBTTagCompound entityTag = info.nbt.copy();

            NbtWrap.remove(entityTag, "Pos");
            NbtWrap.putTag(entityData, "nbt", entityTag);

            NbtWrap.addTag(tagList, entityData);
        }

        NbtWrap.putTag(tag, "entities", tagList);
    }

    @Override
    public NBTTagCompound toTag()
    {
        NBTTagCompound tag = new NBTTagCompound();

        this.writeBlocksToTag(tag);
        this.writeEntitiesToTag(tag);
        this.writeMetadataToTag(tag);

        NbtUtils.writeBlockPosToListTag(this.getSize(), tag, "size");

        NbtWrap.putInt(tag, "DataVersion", LitematicaSchematic.MINECRAFT_DATA_VERSION);

        return tag;
    }
}
