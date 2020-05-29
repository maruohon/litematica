package fi.dy.masa.litematica.world;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.DummyClientTickScheduler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.map.MapState;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.tag.RegistryTagManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.LightType;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.TickScheduler;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.render.schematic.WorldRendererSchematic;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class WorldSchematic extends World
{
    private final MinecraftClient mc;
    private final WorldRendererSchematic worldRenderer;
    private final ChunkManagerSchematic chunkManagerSchematic;
    private final Int2ObjectOpenHashMap<Entity> regularEntities = new Int2ObjectOpenHashMap<>();
    private int nextEntityId;

    protected WorldSchematic(MutableWorldProperties mutableWorldProperties, DimensionType dimensionType, Supplier<Profiler> supplier)
    {
        super(mutableWorldProperties, null, null, dimensionType, supplier, true, true, 0L);

        this.mc = MinecraftClient.getInstance();
        this.worldRenderer = LitematicaRenderer.getInstance().getWorldRenderer();
        this.chunkManagerSchematic = new ChunkManagerSchematic(this);
    }

    public ChunkManagerSchematic getChunkProvider()
    {
        return this.chunkManagerSchematic;
    }

    @Override
    public ChunkManagerSchematic getChunkManager()
    {
        return this.chunkManagerSchematic;
    }

    @Override
    public TickScheduler<Block> getBlockTickScheduler()
    {
        return DummyClientTickScheduler.get();
    }

    @Override
    public TickScheduler<Fluid> getFluidTickScheduler()
    {
        return DummyClientTickScheduler.get();
    }

    public int getRegularEntityCount()
    {
        return this.regularEntities.size();
    }

    @Override
    public WorldChunk getWorldChunk(BlockPos pos)
    {
        return this.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    @Override
    public WorldChunk getChunk(int chunkX, int chunkZ)
    {
        return this.chunkManagerSchematic.getChunk(chunkX, chunkZ);
    }

    @Override
    public Chunk getChunk(int chunkX, int chunkZ, ChunkStatus status, boolean required)
    {
        return this.getChunk(chunkX, chunkZ);
    }

    @Override
    public Biome getGeneratorStoredBiome(int biomeX, int biomeY, int biomeZ)
    {
        return Biomes.PLAINS;
    }

    @Override
    public boolean setBlockState(BlockPos pos, BlockState newState, int flags)
    {
        if (pos.getY() < 0 || pos.getY() >= 256)
        {
            return false;
        }
        else
        {
            return this.getChunk(pos.getX() >> 4, pos.getZ() >> 4).setBlockState(pos, newState, false) != null;
        }
    }

    public boolean spawnEntity(Entity entityIn)
    {
        return this.spawnEntityBase(entityIn);
    }

    private boolean spawnEntityBase(Entity entity)
    {
        int cx = MathHelper.floor(entity.getX() / 16.0D);
        int cz = MathHelper.floor(entity.getZ() / 16.0D);

        if (this.chunkManagerSchematic.isChunkLoaded(cx, cz) == false)
        {
            return false;
        }
        else
        {
            entity.setEntityId(this.nextEntityId++);

            int id = entity.getEntityId();
            this.removeEntity(id);

            this.regularEntities.put(id, entity);
            this.chunkManagerSchematic.getChunk(MathHelper.floor(entity.getX() / 16.0D), MathHelper.floor(entity.getZ() / 16.0D)).addEntity(entity);

            return true;
        }
    }

    public void removeEntity(int id)
    {
        Entity entity = this.regularEntities.remove(id);

        if (entity != null)
        {
            entity.remove();
            entity.detach();

            if (entity.updateNeeded)
            {
                this.getChunk(entity.chunkX, entity.chunkZ).remove(entity);
            }
        }
    }

    @Nullable
    @Override
    public Entity getEntityById(int id)
    {
        return this.regularEntities.get(id);
    }

    @Override
    public List<? extends PlayerEntity> getPlayers()
    {
        return ImmutableList.of();
    }

    public void unloadBlockEntities(Collection<BlockEntity> blockEntities)
    {
        Set<BlockEntity> remove = Collections.newSetFromMap(new IdentityHashMap<>());
        remove.addAll(blockEntities);
        this.tickingBlockEntities.removeAll(remove);
        this.blockEntities.removeAll(remove);
    }

    @Override
    public long getTime()
    {
        return this.mc.world != null ? this.mc.world.getTime() : 0;
    }

    @Override
    @Nullable
    public MapState getMapState(String id)
    {
        return null;
    }

    @Override
    public void putMapState(MapState mapState)
    {
        // NO-OP
    }

    @Override
    public int getNextMapId()
    {
        return 0;
    }

    @Override
    public Scoreboard getScoreboard()
    {
        return this.mc.world != null ? this.mc.world.getScoreboard() : null;
    }

    @Override
    public RecipeManager getRecipeManager()
    {
        return this.mc.world != null ? this.mc.world.getRecipeManager() : null;
    }

    @Override
    public RegistryTagManager getTagManager()
    {
        return this.mc.world != null ? this.mc.world.getTagManager() : null;
    }

    @Override
    public void checkBlockRerender(BlockPos pos, BlockState stateOld, BlockState stateNew)
    {
        this.scheduleBlockRenders(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
    }

    public void scheduleBlockRenders(int chunkX, int chunkY, int chunkZ)
    {
        if (chunkY >= 0 && chunkY < 16)
        {
            this.worldRenderer.scheduleChunkRenders(chunkX, chunkY, chunkZ);
        }
    }

    public void scheduleChunkRenders(int chunkX, int chunkZ)
    {
        for (int chunkY = 0; chunkY < 16; ++chunkY)
        {
            this.worldRenderer.scheduleChunkRenders(chunkX, chunkY, chunkZ);
        }
    }

    public void scheduleChunkRenders(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ)
    {
        final int minChunkX = Math.min(minBlockX, maxBlockX) >> 4;
        final int minChunkY = MathHelper.clamp(Math.min(minBlockY, maxBlockY) >> 4, 0, 15);
        final int minChunkZ = Math.min(minBlockZ, maxBlockZ) >> 4;
        final int maxChunkX = Math.max(minBlockX, maxBlockX) >> 4;
        final int maxChunkY = MathHelper.clamp(Math.max(minBlockY, maxBlockY) >> 4, 0, 15);
        final int maxChunkZ = Math.max(minBlockZ, maxBlockZ) >> 4;

        for (int cz = minChunkZ; cz <= maxChunkZ; ++cz)
        {
            for (int cx = minChunkX; cx <= maxChunkX; ++cx)
            {
                for (int cy = minChunkY; cy <= maxChunkY; ++cy)
                {
                    this.worldRenderer.scheduleChunkRenders(cx, cy, cz);
                }
            }
        }
    }

    @Override
    public float getBrightness(Direction direction, boolean shaded)
    {
        return 0;
    }

    @Override
    public int getLightLevel(LightType type, BlockPos pos)
    {
        return 15;
    }

    @Override
    public int getBaseLightLevel(BlockPos pos, int defaultValue)
    {
        return 15;
    }

    @Override
    public void updateListeners(BlockPos blockPos_1, BlockState blockState_1, BlockState blockState_2, int flags)
    {
        // NO-OP
    }

    @Override
    public void setBlockBreakingInfo(int entityId, BlockPos pos, int progress)
    {
        // NO-OP
    }

    @Override
    public void syncGlobalEvent(int eventId, BlockPos pos, int data)
    {
        // NO-OP
    }
    
    @Override
    public void syncWorldEvent(@Nullable PlayerEntity entity, int id, BlockPos pos, int data)
    {
    }

    @Override
    public void addParticle(ParticleEffect particleParameters_1, double double_1, double double_2, double double_3, double double_4, double double_5, double double_6)
    {
        // NO-OP
    }

    @Override
    public void addParticle(ParticleEffect particleParameters_1, boolean boolean_1, double double_1, double double_2, double double_3, double double_4, double double_5, double double_6)
    {
        // NO-OP
    }

    @Override
    public void addImportantParticle(ParticleEffect particleParameters_1, double double_1, double double_2, double double_3, double double_4,   double double_5, double double_6)
    {
        // NO-OP
    }

    @Override
    public void addImportantParticle(ParticleEffect particleParameters_1, boolean boolean_1, double double_1, double double_2, double double_3,     double double_4, double double_5, double double_6)
    {
        // NO-OP
    }

    @Override
    public void playSound(double x, double y, double z, SoundEvent soundIn, SoundCategory category, float volume, float pitch, boolean distanceDelay)
    {
        // NO-OP
    }

    @Override
    public void playSound(PlayerEntity player, BlockPos pos, SoundEvent soundIn, SoundCategory category, float volume, float pitch)
    {
        // NO-OP
    }

    @Override
    public void playSound(PlayerEntity player, double x, double y, double z, SoundEvent soundIn, SoundCategory category, float volume, float pitch)
    {
        // NO-OP
    }

    @Override
    public void playSoundFromEntity(@Nullable PlayerEntity player, Entity entity, SoundEvent sound, SoundCategory category, float volume, float pitch)
    {
        // NO-OP
    }
}
