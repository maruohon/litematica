package fi.dy.masa.litematica.schematic.container;

import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.Vec3i;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

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
    protected final int totalVolume;
    protected long[] blockCounts;
    protected int bits;
    protected boolean hasSetBlockCounts;

    public LitematicaBlockStateContainer(Vec3i size)
    {
        this(size, 2, null);
    }

    protected LitematicaBlockStateContainer(Vec3i size, int bits, long[] backingLongArray)
    {
        this.size = size;
        this.sizeX = size.getX();
        this.sizeY = size.getY();
        this.sizeZ = size.getZ();
        this.totalVolume = this.sizeX * this.sizeY * this.sizeZ;
        this.sizeLayer = this.sizeX * this.sizeZ;

        this.setBits(bits, backingLongArray);
    }

    protected void setBlockCounts(long[] blockCounts)
    {
        final int length = blockCounts.length;

        if (this.blockCounts == null || this.blockCounts.length < length)
        {
            this.blockCounts = new long[length];
        }

        System.arraycopy(blockCounts, 0, this.blockCounts, 0, length);
        this.hasSetBlockCounts = true;
    }

    public Vec3i getSize()
    {
        return this.size;
    }

    public ILitematicaBlockStatePalette getPalette()
    {
        return this.palette;
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
                this.storage = new LitematicaBitArray(this.bits, this.totalVolume, backingLongArray);
            }
            else
            {
                this.storage = new LitematicaBitArray(this.bits, this.totalVolume);
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

    public byte[] getBackingArrayAsByteArray()
    {
        final int entrySize = PacketBuffer.getVarIntSize(this.palette.getPaletteSize() - 1);
        final long volume = this.storage.size();
        final long length = (long) entrySize * volume;

        if (length > Integer.MAX_VALUE)
        {
            throw new IndexOutOfBoundsException("Block data backing byte array length " + length + " exceeds the maximum value of " + Integer.MAX_VALUE);
        }

        byte[] arr = new byte[(int) length];
        PacketBuffer buf = new PacketBuffer(Unpooled.wrappedBuffer(arr));
        buf.writerIndex(0);

        for (int i = 0; i < volume; ++i)
        {
            buf.writeVarInt(this.storage.getAt(i));
        }

        return arr;
    }

    public long getTotalBlockCount()
    {
        this.calculateBlockCountsIfNeeded();

        ILitematicaBlockStatePalette palette = this.getPalette();
        IBlockState air = Blocks.AIR.getDefaultState();
        final int length = this.blockCounts.length;
        int count = 0;

        for (int id = 0; id < length; ++id)
        {
            IBlockState state = palette.getBlockState(id);

            if (state != null && state != air)
            {
                count += this.blockCounts[id];
            }
        }

        return count;
    }

    public Map<IBlockState, Long> getBlockCountsMap()
    {
        this.calculateBlockCountsIfNeeded();

        Map<IBlockState, Long> map = new Object2LongOpenHashMap<>(this.blockCounts.length);
        ILitematicaBlockStatePalette palette = this.getPalette();
        final int length = this.blockCounts.length;

        for (int id = 0; id < length; ++id)
        {
            IBlockState state = palette.getBlockState(id);

            if (state != null)
            {
                map.put(state, this.blockCounts[id]);
            }
        }

        return map;
    }

    protected void calculateBlockCountsIfNeeded()
    {
        if (this.hasSetBlockCounts == false)
        {
            long[] counts = new long[1 << this.bits];
            LitematicaBitArray storage = this.storage;
            final long length = storage.size();

            for (long i = 0; i < length; ++i)
            {
                int id = storage.getAt(i);
                ++counts[id];
            }

            this.setBlockCounts(counts);
        }
    }

    public LitematicaBlockStateContainer copy()
    {
        LitematicaBlockStateContainer copy = new LitematicaBlockStateContainer(this.size, this.bits, this.storage.getBackingLongArray().clone());
        copy.palette = this.palette.copy();

        return copy;
    }

    protected static SpongeBlockstateConverterResults convertVarintByteArrayToPackedLongArray(Vec3i size, int bits, byte[] blockStates)
    {
        int volume = size.getX() * size.getY() * size.getZ();
        LitematicaBitArray bitArray = new LitematicaBitArray(bits, volume);
        PacketBuffer buf = new PacketBuffer(Unpooled.wrappedBuffer(blockStates));
        long[] blockCounts = new long[1 << bits];

        for (int i = 0; i < volume; ++i)
        {
            int id = buf.readVarInt();
            bitArray.setAt(i, id);
            ++blockCounts[id];
        }

        return new SpongeBlockstateConverterResults(bitArray.getBackingLongArray(), blockCounts);
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
        SpongeBlockstateConverterResults results = convertVarintByteArrayToPackedLongArray(size, bits, blockStates);
        LitematicaBlockStateContainer container = new LitematicaBlockStateContainer(size, bits, results.backingArray);
        ILitematicaBlockStatePalette palette = createPalette(bits, container);

        if (palette.readFromSpongeTag(paletteTag))
        {
            container.palette = palette;
            container.setBlockCounts(results.blockCounts);
            return container;
        }

        return null;
    }

    protected static ILitematicaBlockStatePalette createPalette(int bits, LitematicaBlockStateContainer container)
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

    protected static class SpongeBlockstateConverterResults
    {
        public final long[] backingArray;
        public final long[] blockCounts;

        protected SpongeBlockstateConverterResults(long[] backingArray, long[] blockCounts)
        {
            this.backingArray = backingArray;
            this.blockCounts = blockCounts;
        }
    }
}
