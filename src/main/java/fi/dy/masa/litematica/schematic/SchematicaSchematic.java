package fi.dy.masa.litematica.schematic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.malilib.overlay.message.MessageDispatcher;
import fi.dy.masa.malilib.util.game.wrap.NbtWrap;
import fi.dy.masa.malilib.util.nbt.NbtUtils;
import fi.dy.masa.litematica.schematic.container.ILitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.container.ILitematicaBlockStatePalette;

public class SchematicaSchematic extends SingleRegionSchematic
{
    public static final String FILE_NAME_EXTENSION = ".schematic";

    private Block[] palette;

    SchematicaSchematic(@Nullable File fileName)
    {
        super(fileName);
    }

    @Override
    public SchematicType<?> getType()
    {
        return SchematicType.SCHEMATICA;
    }

    public static boolean isValidSchematic(NBTTagCompound tag)
    {
        if (NbtWrap.containsShort(tag, "Width") &&
            NbtWrap.containsShort(tag, "Height") &&
            NbtWrap.containsShort(tag, "Length") &&
            NbtWrap.containsByteArray(tag, "Blocks") &&
            NbtWrap.containsByteArray(tag, "Data"))
        {
            return isSizeValid(readSizeFromTagImpl(tag));
        }

        return false;
    }

    private static Vec3i readSizeFromTagImpl(NBTTagCompound tag)
    {
        return new Vec3i(NbtWrap.getShort(tag, "Width"),
                         NbtWrap.getShort(tag, "Height"),
                         NbtWrap.getShort(tag, "Length"));
    }

    @Override
    @Nullable
    protected Vec3i readSizeFromTag(NBTTagCompound tag)
    {
        if (isValidSchematic(tag))
        {
            return readSizeFromTagImpl(tag);
        }

        return null;
    }

    @Override
    protected Map<BlockPos, NBTTagCompound> readBlockEntitiesFromTag(NBTTagCompound tag)
    {
        return this.readBlockEntitiesFromListTag(NbtWrap.getListOfCompounds(tag, "TileEntities"));
    }

    @Override
    protected List<EntityInfo> readEntitiesFromTag(NBTTagCompound tag)
    {
        return this.readEntitiesFromListTag(NbtWrap.getListOfCompounds(tag, "Entities"));
    }

    @Override
    public NBTTagCompound toTag()
    {
        NBTTagCompound nbt = new NBTTagCompound();

        this.createPalette();
        this.writeBlocksToTag(nbt);
        this.writePaletteToTag(nbt);

        NbtWrap.putTag(nbt, "TileEntities", this.writeBlockEntitiesToListTag(this.blockEntities));
        NbtWrap.putTag(nbt, "Entities", this.writeEntitiesToListTag(this.entities));
        NbtWrap.putTag(nbt, "Metadata", this.getMetadata().toTag());

        return nbt;
    }

    protected boolean readPaletteFromTag(NBTTagCompound nbt)
    {
        final Block air = Blocks.AIR;
        this.palette = new Block[4096];
        Arrays.fill(this.palette, air);

        // Schematica palette
        if (NbtWrap.containsCompound(nbt, "SchematicaMapping"))
        {
            return this.readSchematicaPaletteFromTag(NbtWrap.getCompound(nbt, "SchematicaMapping"));
        }
        // MCEdit2 palette
        else if (NbtWrap.containsCompound(nbt, "BlockIDs"))
        {
            return this.readMCEdit2PaletteFromTag(NbtWrap.getCompound(nbt, "BlockIDs"));
        }
        // No palette, use the current registry IDs directly
        else
        {
            this.createRegistryBasedPalette();
        }

        return true;
    }

    protected boolean readSchematicaPaletteFromTag(NBTTagCompound tag)
    {
        for (String key : NbtWrap.getKeys(tag))
        {
            int id = NbtWrap.getShort(tag, key);

            if (id >= this.palette.length)
            {
                MessageDispatcher.error().translate("litematica.message.error.schematic_read.schematica.palette.invalid_id",
                                                    id, key, this.palette.length - 1);
                continue;
            }

            Block block = Block.REGISTRY.getObject(new ResourceLocation(key));

            if (block == null)
            {
                MessageDispatcher.error().translate("litematica.message.error.schematic_read.schematica.palette.invalid_block", key);
                continue;
            }

            this.palette[id] = block;
        }

        return true;
    }

    protected boolean readMCEdit2PaletteFromTag(NBTTagCompound tag)
    {
        for (String idStr : NbtWrap.getKeys(tag))
        {
            String key = NbtWrap.getString(tag, idStr);
            int id;

            try
            {
                id = Integer.parseInt(idStr);
            }
            catch (NumberFormatException e)
            {
                MessageDispatcher.error().translate("litematica.message.error.schematic_read.schematica.palette.id_not_number", idStr, key);
                continue;
            }

            if (id >= this.palette.length)
            {
                MessageDispatcher.error().translate("litematica.message.error.schematic_read.mcedit2.palette.invalid_id", id, key, this.palette.length - 1);
                continue;
            }

            Block block = Block.REGISTRY.getObject(new ResourceLocation(key));

            if (block == null)
            {
                MessageDispatcher.error().translate("litematica.message.error.schematic_read.mcedit2.missing_block_data", key);
                continue;
            }

            this.palette[id] = block;
        }

        return true;
    }

    protected void createRegistryBasedPalette()
    {
        for (ResourceLocation key : Block.REGISTRY.getKeys())
        {
            Block block = Block.REGISTRY.getObject(key);

            if (block != null)
            {
                int id = Block.getIdFromBlock(block);

                if (id >= 0 && id < this.palette.length)
                {
                    this.palette[id] = block;
                }
                else
                {
                    MessageDispatcher.error().translate("litematica.message.error.schematic_read.registry_palette.missing_block_data", id, key);
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    protected boolean readBlocksFromTag(NBTTagCompound tag)
    {
        // This method was implemented based on
        // https://minecraft.gamepedia.com/Schematic_file_format
        // as it was on 2018-04-18.

        Vec3i size = this.getSize();
        final int sizeX = size.getX();
        final int sizeY = size.getY();
        final int sizeZ = size.getZ();
        final byte[] blockIdsByte = NbtWrap.getByteArray(tag, "Blocks");
        final byte[] metaArr = NbtWrap.getByteArray(tag, "Data");
        final int numBlocks = blockIdsByte.length;
        final int layerSize = sizeX * sizeZ;

        if (numBlocks != (sizeX * sizeY * sizeZ))
        {
            MessageDispatcher.error().translate("litematica.message.error.schematic_read.schematica.schematic.invalid_block_array_size", numBlocks, sizeX, sizeY, sizeZ);
            return false;
        }

        if (numBlocks != metaArr.length)
        {
            MessageDispatcher.error().translate("litematica.message.error.schematic_read.schematica.schematic.invalid_metadata_array_size", numBlocks, metaArr.length);
            return false;
        }

        if (this.readPaletteFromTag(tag) == false)
        {
            MessageDispatcher.error().translate("litematica.message.error.schematic_read.schematica.palette.failed_to_read");
            return false;
        }

        if (NbtWrap.containsByteArray(tag, "AddBlocks"))
        {
            return this.readBlocks12Bit(tag, blockIdsByte, metaArr, sizeX, layerSize);
        }
        // Old Schematica format
        else if (NbtWrap.containsByteArray(tag, "Add"))
        {
            // FIXME is this array 4 or 8 bits per block?
            MessageDispatcher.error().translate("litematica.message.error.schematic_read.schematica.old_schematica_format_not_supported");
            return false;
        }
        // No palette, use the registry IDs directly
        else
        {
            ILitematicaBlockStateContainer container = this.blockContainer;
            Block[] palette = this.palette;

            for (int i = 0; i < numBlocks; i++)
            {
                Block block = palette[blockIdsByte[i] & 0xFF];
                int x = i % sizeX;
                int y = i / layerSize;
                int z = (i % layerSize) / sizeX;
                container.setBlockState(x, y, z, block.getStateFromMeta(metaArr[i]));
            }
        }

        return true;
    }

    @SuppressWarnings("deprecation")
    protected boolean readBlocks12Bit(NBTTagCompound nbt, final byte[] blockIdsByte, final byte[] metaArr, final int sizeX, final int layerSize)
    {
        ILitematicaBlockStateContainer container = this.blockContainer;
        Block[] palette = this.palette;
        byte[] add = NbtWrap.getByteArray(nbt, "AddBlocks");
        final int numBlocks = blockIdsByte.length;
        final int expectedAddLength = (int) Math.ceil((double) blockIdsByte.length / 2D);

        if (add.length != expectedAddLength)
        {
            MessageDispatcher.error().translate("litematica.message.error.schematic_read.schematica.schematic.invalid_block_add_array_size", numBlocks, add.length, expectedAddLength);
            return false;
        }

        final int loopMax;

        // Even number of blocks, we can handle two position (meaning one full add byte) at a time
        if ((numBlocks % 2) == 0)
        {
            loopMax = numBlocks - 1;
        }
        else
        {
            loopMax = numBlocks - 2;
        }

        Block block;
        int byteId;
        int bi, ai;

        // Handle two positions per iteration, ie. one full byte of the add array
        for (bi = 0, ai = 0; bi < loopMax; bi += 2, ai++)
        {
            final int addValue = add[ai];

            byteId = blockIdsByte[bi    ] & 0xFF;
            block = palette[(addValue & 0xF0) << 4 | byteId];
            int x = bi % sizeX;
            int y = bi / layerSize;
            int z = (bi % layerSize) / sizeX;
            container.setBlockState(x, y, z, block.getStateFromMeta(metaArr[bi    ]));

            x = (bi + 1) % sizeX;
            y = (bi + 1) / layerSize;
            z = ((bi + 1) % layerSize) / sizeX;
            byteId = blockIdsByte[bi + 1] & 0xFF;
            block = palette[(addValue & 0x0F) << 8 | byteId];
            container.setBlockState(x, y, z, block.getStateFromMeta(metaArr[bi + 1]));
        }

        // Odd number of blocks, handle the last position
        if ((numBlocks % 2) != 0)
        {
            final int addValue = add[ai];
            byteId = blockIdsByte[bi    ] & 0xFF;
            block = palette[(addValue & 0xF0) << 4 | byteId];
            int x = bi % sizeX;
            int y = bi / layerSize;
            int z = (bi % layerSize) / sizeX;
            container.setBlockState(x, y, z, block.getStateFromMeta(metaArr[bi    ]));
        }

        return true;
    }

    protected void createPalette()
    {
        if (this.palette == null)
        {
            this.palette = new Block[4096];
            ILitematicaBlockStatePalette litematicaPalette = this.blockContainer.getPalette();
            final int numBlocks = litematicaPalette.getPaletteSize();

            for (int i = 0; i < numBlocks; ++i)
            {
                IBlockState state = litematicaPalette.getBlockState(i);
                Block block = state.getBlock();
                int id = Block.getIdFromBlock(block);

                if (id >= this.palette.length)
                {
                    MessageDispatcher.error().translate("litematica.message.error.schematic_write.schematica.palette.invalid_id", id, state, this.palette.length - 1);
                    continue;
                }

                this.palette[id] = block;
            }
        }
    }

    protected void writePaletteToTag(NBTTagCompound nbt)
    {
        NBTTagCompound tag = new NBTTagCompound();

        for (int i = 0; i < this.palette.length; ++i)
        {
            Block block = this.palette[i];

            if (block != null)
            {
                ResourceLocation rl = Block.REGISTRY.getNameForObject(block);

                if (rl != null)
                {
                    NbtWrap.putShort(tag, rl.toString(), (short) (i & 0xFFF));
                }
            }
        }

        NbtWrap.putTag(nbt, "SchematicaMapping", tag);
    }

    protected void writeBlocksToTag(NBTTagCompound nbt)
    {
        Vec3i size = this.getSize();
        final int sizeX = size.getX();
        final int sizeZ = size.getZ();

        NbtWrap.putShort(nbt, "Width", (short) sizeX);
        NbtWrap.putShort(nbt, "Height", (short) size.getY());
        NbtWrap.putShort(nbt, "Length", (short) sizeZ);
        NbtWrap.putString(nbt, "Materials", "Alpha");

        final int numBlocks = sizeX * size.getY() * sizeZ;
        final int loopMax = (int) Math.floor((double) numBlocks / 2D);
        final int addSize = (int) Math.ceil((double) numBlocks / 2D);
        final byte[] blockIdsArr = new byte[numBlocks];
        final byte[] metaArr = new byte[numBlocks];
        final byte[] addArr = new byte[addSize];
        final int layerSize = sizeX * sizeZ;
        int numAdd = 0;
        int bi, ai;
        ILitematicaBlockStateContainer container = this.blockContainer;

        for (bi = 0, ai = 0; ai < loopMax; bi += 2, ++ai)
        {
            int x = bi % sizeX;
            int y = bi / layerSize;
            int z = (bi % layerSize) / sizeX;
            IBlockState state1 = container.getBlockState(x, y, z);

            x = (bi + 1) % sizeX;
            y = (bi + 1) / layerSize;
            z = ((bi + 1) % layerSize) / sizeX;
            IBlockState state2 = container.getBlockState(x, y, z);

            int id1 = Block.getIdFromBlock(state1.getBlock());
            int id2 = Block.getIdFromBlock(state2.getBlock());
            int add = ((id1 >>> 4) & 0xF0) | ((id2 >>> 8) & 0x0F);
            blockIdsArr[bi    ] = (byte) (id1 & 0xFF);
            blockIdsArr[bi + 1] = (byte) (id2 & 0xFF);

            if (add != 0)
            {
                addArr[ai] = (byte) add;
                ++numAdd;
            }

            metaArr[bi    ] = (byte) state1.getBlock().getMetaFromState(state1);
            metaArr[bi + 1] = (byte) state2.getBlock().getMetaFromState(state2);
        }

        // Odd number of blocks, handle the last position
        if ((numBlocks % 2) != 0)
        {
            int x = bi % sizeX;
            int y = bi / layerSize;
            int z = (bi % layerSize) / sizeX;
            IBlockState state = container.getBlockState(x, y, z);

            int id = Block.getIdFromBlock(state.getBlock());
            int add = (id >>> 4) & 0xF0;
            blockIdsArr[bi] = (byte) (id & 0xFF);

            if (add != 0)
            {
                addArr[ai] = (byte) add;
                ++numAdd;
            }

            metaArr[bi] = (byte) state.getBlock().getMetaFromState(state);
        }

        NbtWrap.putByteArray(nbt, "Blocks", blockIdsArr);
        NbtWrap.putByteArray(nbt, "Data", metaArr);

        if (numAdd > 0)
        {
            NbtWrap.putByteArray(nbt, "AddBlocks", addArr);
        }
    }

    @Override
    public void writeToStream(NBTTagCompound tag, FileOutputStream outputStream) throws IOException
    {
        // MCEdit and World Edit require the root compound tag to be named "Schematic".
        // The vanilla util methods don't support doing that, so we have to use a custom method for it.
        NbtUtils.writeCompressed(tag, "Schematic", outputStream);
    }
}
