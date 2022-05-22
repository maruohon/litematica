package fi.dy.masa.litematica.schematic;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.malilib.util.nbt.NbtUtils;
import fi.dy.masa.litematica.mixin.IMixinDataFixer;
import fi.dy.masa.litematica.schematic.container.ILitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.container.ILitematicaBlockStatePalette;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainerFull;

public abstract class SchematicBase implements ISchematic
{
    public static final int MINECRAFT_DATA_VERSION = ((IMixinDataFixer) Minecraft.getMinecraft().getDataFixer()).getVersion();

    @Nullable protected final File schematicFile;
    protected final SchematicMetadata metadata = new SchematicMetadata();
    protected long totalBlocksReadFromWorld;

    public SchematicBase(@Nullable File file)
    {
        this.schematicFile = file;
    }

    @Override
    @Nullable
    public File getFile()
    {
        return this.schematicFile;
    }

    @Override
    public SchematicMetadata getMetadata()
    {
        return this.metadata;
    }

    public long getTotalBlocksReadFromWorld()
    {
        return this.totalBlocksReadFromWorld;
    }

    public void setTotalBlocksReadFromWorld(long count)
    {
        this.totalBlocksReadFromWorld = count;
    }

    public static boolean isSizeValid(@Nullable Vec3i size)
    {
        return size != null && size.getX() > 0 && size.getY() > 0 && size.getZ() > 0;
    }

    protected Class<? extends ILitematicaBlockStateContainer> getContainerClass()
    {
        return LitematicaBlockStateContainerFull.class;
    }

    protected void copyContainerContents(ILitematicaBlockStateContainer from, ILitematicaBlockStateContainer to)
    {
        Vec3i sizeFrom = from.getSize();
        Vec3i sizeTo = to.getSize();
        final int sizeX = Math.min(sizeFrom.getX(), sizeTo.getX());
        final int sizeY = Math.min(sizeFrom.getY(), sizeTo.getY());
        final int sizeZ = Math.min(sizeFrom.getZ(), sizeTo.getZ());

        for (int y = 0; y < sizeY; ++y)
        {
            for (int z = 0; z < sizeZ; ++z)
            {
                for (int x = 0; x < sizeX; ++x)
                {
                    IBlockState state = from.getBlockState(x, y, z);
                    to.setBlockState(x, y, z, state);
                }
            }
        }
    }

    protected void readMetadataFromTag(NBTTagCompound tag)
    {
        if (NbtUtils.containsCompound(tag, "Metadata"))
        {
            this.getMetadata().fromTag(NbtUtils.getCompound(tag, "Metadata"));
        }
    }

    public static boolean readPaletteFromLitematicaFormatTag(NBTTagList tagList, ILitematicaBlockStatePalette palette)
    {
        final int size = NbtUtils.getListSize(tagList);
        List<IBlockState> list = new ArrayList<>(size);

        for (int id = 0; id < size; ++id)
        {
            NBTTagCompound tag = NbtUtils.getCompoundAt(tagList, id);
            IBlockState state = NBTUtil.readBlockState(tag);
            list.add(state);
        }

        return palette.setMapping(list);
    }

    protected List<EntityInfo> readEntitiesFromListTag(NBTTagList tagList)
    {
        List<EntityInfo> entityList = new ArrayList<>();
        final int size = NbtUtils.getListSize(tagList);

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound entityData = NbtUtils.getCompoundAt(tagList, i);
            Vec3d posVec = NbtUtils.readVec3dFromListTag(entityData);

            if (posVec != null && entityData.isEmpty() == false)
            {
                entityList.add(new EntityInfo(posVec, entityData));
            }
        }

        return entityList;
    }

    protected Map<BlockPos, NBTTagCompound> readBlockEntitiesFromListTag(NBTTagList tagList)
    {
        Map<BlockPos, NBTTagCompound> tileMap = new HashMap<>();
        final int size = NbtUtils.getListSize(tagList);

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound tag = NbtUtils.getCompoundAt(tagList, i);
            BlockPos pos = NbtUtils.readBlockPos(tag);
            NbtUtils.removeBlockPosFromTag(tag);

            if (pos != null && tag.isEmpty() == false)
            {
                tileMap.put(pos, tag);
            }
        }

        return tileMap;
    }

    protected NBTTagList writePaletteToLitematicaFormatTag(ILitematicaBlockStatePalette palette)
    {
        final int size = palette.getPaletteSize();
        List<IBlockState> list = palette.getMapping();
        NBTTagList tagList = new NBTTagList();

        for (int id = 0; id < size; ++id)
        {
            NBTTagCompound tag = new NBTTagCompound();
            NBTUtil.writeBlockState(tag, list.get(id));
            NbtUtils.addTag(tagList, tag);
        }

        return tagList;
    }

    protected NBTTagList writeEntitiesToListTag(List<EntityInfo> entityList)
    {
        NBTTagList tagList = new NBTTagList();

        if (entityList.isEmpty() == false)
        {
            for (EntityInfo info : entityList)
            {
                NbtUtils.addTag(tagList, info.nbt);
            }
        }

        return tagList;
    }

    protected NBTTagList writeBlockEntitiesToListTag(Map<BlockPos, NBTTagCompound> tileMap)
    {
        NBTTagList tagList = new NBTTagList();

        if (tileMap.isEmpty() == false)
        {
            for (Map.Entry<BlockPos, NBTTagCompound> entry : tileMap.entrySet())
            {
                NBTTagCompound tag = entry.getValue();
                NbtUtils.putVec3i(tag, entry.getKey());
                NbtUtils.addTag(tagList, tag);
            }
        }

        return tagList;
    }
}
