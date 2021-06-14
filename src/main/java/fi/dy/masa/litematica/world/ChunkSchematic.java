package fi.dy.masa.litematica.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.biome.BuiltinBiomes;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class ChunkSchematic extends WorldChunk
{
    private static final BlockState AIR = Blocks.AIR.getDefaultState();

    private final Int2ObjectOpenHashMap<List<Entity>> entityLists = new Int2ObjectOpenHashMap<>();
    private final long timeCreated;
    private final int bottomY;
    private final int topY;
    private int entityCount;
    private boolean isEmpty = true;

    public ChunkSchematic(World worldIn, ChunkPos pos)
    {
        super(worldIn, pos, new BiomeArray(worldIn.getRegistryManager().get(Registry.BIOME_KEY),
                                           worldIn, pos, new FixedBiomeSource(BuiltinBiomes.THE_VOID)));

        this.timeCreated = worldIn.getTime();
        this.bottomY = worldIn.getBottomY();
        this.topY = worldIn.getTopY();
    }

    @Override
    public BlockState getBlockState(BlockPos pos)
    {
        int x = pos.getX() & 0xF;
        int y = pos.getY();
        int z = pos.getZ() & 0xF;
        int cy = this.getSectionIndex(y);
        y &= 0xF;

        ChunkSection[] sections = this.getSectionArray();

        if (cy >= 0 && cy < sections.length)
        {
            ChunkSection chunkSection = sections[cy];

            if (ChunkSection.isEmpty(chunkSection) == false)
            {
                return chunkSection.getBlockState(x, y, z);
            }
         }

         return AIR;
    }

    @Override
    public BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving)
    {
        BlockState stateOld = this.getBlockState(pos);
        int y = pos.getY();

        if (stateOld == state || y >= this.topY || y < bottomY)
        {
            return null;
        }
        else
        {
            int x = pos.getX() & 15;
            int z = pos.getZ() & 15;
            int cy = this.getSectionIndex(y);

            Block blockNew = state.getBlock();
            Block blockOld = stateOld.getBlock();
            ChunkSection section = this.getSectionArray()[cy];

            if (section == EMPTY_SECTION)
            {
                if (state.isAir())
                {
                    return null;
                }

                section = new ChunkSection(ChunkSectionPos.getSectionCoord(y));
                this.getSectionArray()[cy] = section;
            }

            y &= 0xF;

            if (state.isAir() == false)
            {
                this.isEmpty = false;
            }

            section.setBlockState(x, y, z, state);

            if (blockOld != blockNew)
            {
                this.getWorld().removeBlockEntity(pos);
            }

            if (section.getBlockState(x, y, z).getBlock() != blockNew)
            {
                return null;
            }
            else
            {
                if (state.hasBlockEntity() && blockNew instanceof BlockEntityProvider)
                {
                    BlockEntity te = this.getBlockEntity(pos, WorldChunk.CreationType.CHECK);

                    if (te == null)
                    {
                        te = ((BlockEntityProvider) blockNew).createBlockEntity(pos, state);

                        if (te != null)
                        {
                            this.getWorld().getWorldChunk(pos).setBlockEntity(te);
                        }
                    }
                }

                this.markDirty();

                return stateOld;
            }
        }
    }

    @Override
    public void addEntity(Entity entity)
    {
        int chunkY = MathHelper.floor(entity.getY()) >> 4;
        List<Entity> list = this.entityLists.computeIfAbsent(chunkY, (y) -> new ArrayList<>());
        list.add(entity);
        ++this.entityCount;
    }

    public List<Entity> getEntityListForSectionIfExists(int sectionY)
    {
        return this.entityLists.getOrDefault(sectionY, Collections.emptyList());
    }

    public int getEntityCount()
    {
        return this.entityCount;
    }

    public long getTimeCreated()
    {
        return this.timeCreated;
    }

    @Override
    public boolean isEmpty()
    {
        return this.isEmpty;
    }
}
