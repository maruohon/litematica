package fi.dy.masa.litematica.schematic.container;

import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.Vec3i;
import io.netty.buffer.Unpooled;

public class LitematicaBlockStateContainer implements ILitematicaBlockStatePaletteResizer
{
    public static final IBlockState AIR_BLOCK_STATE = Blocks.AIR.getDefaultState();
    private static final int MAX_BITS_LINEAR = 4;

    protected LitematicaBitArray storage;
    protected ILitematicaBlockStatePalette palette;
    protected final Vec3i size;
    protected final int sizeX;
    protected final int sizeY;
    protected final int sizeZ;
    protected final int sizeLayer;
    protected int bits;

    public LitematicaBlockStateContainer(Vec3i size)
    {
        this(size, 2, null);
    }

    private LitematicaBlockStateContainer(Vec3i size, int bits, long[] backingLongArray)
    {
        this.size = size;
        this.sizeX = size.getX();
        this.sizeY = size.getY();
        this.sizeZ = size.getZ();
        this.sizeLayer = this.sizeX * this.sizeZ;

        this.setBits(bits, backingLongArray);
    }

    public Vec3i getSize()
    {
        return this.size;
    }

    public IBlockState get(int x, int y, int z)
    {
        IBlockState state = this.palette.getBlockState(this.storage.getAt(this.getIndex(x, y, z)));
        return state == null ? AIR_BLOCK_STATE : state;
    }

    public void set(int x, int y, int z, IBlockState state)
    {
        int id = this.palette.idFor(state);
        this.storage.setAt(this.getIndex(x, y, z), id);
    }

    protected void set(int index, IBlockState state)
    {
        int id = this.palette.idFor(state);
        this.storage.setAt(index, id);
    }

    protected int getIndex(int x, int y, int z)
    {
        return (y * this.sizeLayer) + z * this.sizeX + x;
    }

    protected void setBits(int bitsIn, long[] backingLongArray)
    {
        if (bitsIn != this.bits)
        {
            this.bits = bitsIn;

            if (this.bits <= MAX_BITS_LINEAR)
            {
                this.bits = Math.max(2, this.bits);
                this.palette = new LitematicaBlockStatePaletteLinear(this.bits, this);
            }
            else
            {
                this.palette = new LitematicaBlockStatePaletteHashMap(this.bits, this);
            }

            this.palette.idFor(AIR_BLOCK_STATE);

            if (backingLongArray != null)
            {
                this.storage = new LitematicaBitArray(this.bits, this.sizeX * this.sizeY * this.sizeZ, backingLongArray);
            }
            else
            {
                this.storage = new LitematicaBitArray(this.bits, this.sizeX * this.sizeY * this.sizeZ);
            }
        }
    }

    @Override
    public int onResize(int bits, IBlockState state)
    {
        LitematicaBitArray bitArray = this.storage;
        ILitematicaBlockStatePalette statePaletteOld = this.palette;
        this.setBits(bits, null);

        for (int index = 0; index < bitArray.size(); ++index)
        {
            IBlockState stateTmp = statePaletteOld.getBlockState(bitArray.getAt(index));

            if (stateTmp != null)
            {
                this.set(index, stateTmp);
            }
        }

        return this.palette.idFor(state);
    }

    public long[] getBackingLongArray()
    {
        return this.storage.getBackingLongArray();
    }

    public ILitematicaBlockStatePalette getPalette()
    {
        return this.palette;
    }

    public LitematicaBlockStateContainer copy()
    {
        long[] backingLongArrayOld = this.storage.getBackingLongArray();
        long[] backingLongArrayNew = backingLongArrayOld.clone();

        LitematicaBlockStateContainer copy = new LitematicaBlockStateContainer(this.size, this.bits, backingLongArrayNew);
        copy.palette = this.palette.copy();

        return copy;
    }

    public byte[] getBackingArrayAsByteArray()
    {
        final int entrySize = PacketBuffer.getVarIntSize(this.palette.getPaletteSize() - 1);
        final int volume = this.storage.size();
        byte[] arr = new byte[entrySize * volume];
        PacketBuffer buf = new PacketBuffer(Unpooled.wrappedBuffer(arr));

        for (int i = 0; i < volume; ++i)
        {
            buf.writeVarInt(this.storage.getAt(i));
        }

        return arr;
    }

    public static long[] convertVarintByteArrayToPackedLongArray(int volume, int bits, byte[] blockStates)
    {
        LitematicaBitArray bitArray = new LitematicaBitArray(bits, volume);
        PacketBuffer buf = new PacketBuffer(Unpooled.wrappedBuffer(blockStates));

        for (int i = 0; i < volume; ++i)
        {
            bitArray.setAt(i, buf.readVarInt());
        }

        return bitArray.getBackingLongArray();
    }

    @Nullable
    public static LitematicaBlockStateContainer createFromLitematicaFormat(NBTTagList paletteTag, long[] blockStates, Vec3i size)
    {
        int bits = Math.max(2, Integer.SIZE - Integer.numberOfLeadingZeros(paletteTag.tagCount() - 1));
        LitematicaBlockStateContainer container = new LitematicaBlockStateContainer(size, bits, blockStates);
        ILitematicaBlockStatePalette palette = createPalette(bits, container);

        if (palette.readFromLitematicaTag(paletteTag))
        {
            container.palette = palette;
            return container;
        }

        return null;
    }

    @Nullable
    public static LitematicaBlockStateContainer createFromSpongeFormat(NBTTagCompound paletteTag, byte[] blockStates, Vec3i size)
    {
        int bits = Math.max(2, Integer.SIZE - Integer.numberOfLeadingZeros(paletteTag.getKeySet().size() - 1));
        int volume = size.getX() * size.getY() * size.getZ();
        long[] arr = convertVarintByteArrayToPackedLongArray(volume, bits, blockStates);
        LitematicaBlockStateContainer container = new LitematicaBlockStateContainer(size, bits, arr);
        ILitematicaBlockStatePalette palette = createPalette(bits, container);

        if (palette.readFromSpongeTag(paletteTag))
        {
            container.palette = palette;
            return container;
        }

        return null;
    }

    private static ILitematicaBlockStatePalette createPalette(int bits, LitematicaBlockStateContainer container)
    {
        if (bits <= MAX_BITS_LINEAR)
        {
            return new LitematicaBlockStatePaletteLinear(bits, container);
        }
        else
        {
            return new LitematicaBlockStatePaletteHashMap(bits, container);
        }
    }
}
