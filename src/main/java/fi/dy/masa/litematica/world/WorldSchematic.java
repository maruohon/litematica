package fi.dy.masa.litematica.world;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.map.MapState;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.LightType;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.entity.EntityLookup;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.tick.EmptyTickSchedulers;
import net.minecraft.world.tick.QueryableTickScheduler;

import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.render.schematic.WorldRendererSchematic;

public class WorldSchematic extends World
{
    protected static final RegistryKey<World> REGISTRY_KEY = RegistryKey.of(RegistryKeys.WORLD, new Identifier(Reference.MOD_ID, "schematic_world"));

    protected final MinecraftClient mc;
    protected final ChunkManagerSchematic chunkManagerSchematic;
    protected final RegistryEntry<Biome> biome;
    @Nullable protected final WorldRendererSchematic worldRenderer;
    protected int nextEntityId;
    protected int entityCount;

    public WorldSchematic(MutableWorldProperties properties,
                          RegistryEntry<DimensionType> dimension,
                          Supplier<Profiler> supplier,
                          @Nullable WorldRendererSchematic worldRenderer)
    {
        super(properties, REGISTRY_KEY, MinecraftClient.getInstance().getNetworkHandler().getRegistryManager(), dimension, supplier, true, false, 0L, 0);

        this.mc = MinecraftClient.getInstance();
        this.worldRenderer = worldRenderer;
        this.chunkManagerSchematic = new ChunkManagerSchematic(this);
        this.biome = this.mc.world.getRegistryManager().get(RegistryKeys.BIOME).entryOf(BiomeKeys.PLAINS);
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
    public QueryableTickScheduler<Block> getBlockTickScheduler()
    {
        return EmptyTickSchedulers.getClientTickScheduler();
    }

    @Override
    public QueryableTickScheduler<Fluid> getFluidTickScheduler()
    {
        return EmptyTickSchedulers.getClientTickScheduler();
    }

    public int getRegularEntityCount()
    {
        return this.entityCount;
    }

    @Override
    public WorldChunk getWorldChunk(BlockPos pos)
    {
        return this.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    @Override
    public ChunkSchematic getChunk(int chunkX, int chunkZ)
    {
        return this.chunkManagerSchematic.getChunk(chunkX, chunkZ);
    }

    @Override
    public Chunk getChunk(int chunkX, int chunkZ, ChunkStatus status, boolean required)
    {
        return this.getChunk(chunkX, chunkZ);
    }

    @Override
    public RegistryEntry<Biome> getGeneratorStoredBiome(int biomeX, int biomeY, int biomeZ)
    {
        return this.biome;
    }

    @Override
    public boolean setBlockState(BlockPos pos, BlockState newState, int flags)
    {
        if (pos.getY() < this.getBottomY() || pos.getY() >= this.getTopY())
        {
            return false;
        }
        else
        {
            return this.getChunk(pos.getX() >> 4, pos.getZ() >> 4).setBlockState(pos, newState, false) != null;
        }
    }

    @Override
    public boolean spawnEntity(Entity entity)
    {
        int chunkX = MathHelper.floor(entity.getX() / 16.0D);
        int chunkZ = MathHelper.floor(entity.getZ() / 16.0D);

        if (this.chunkManagerSchematic.isChunkLoaded(chunkX, chunkZ) == false)
        {
            return false;
        }
        else
        {
            entity.setId(this.nextEntityId++);
            this.chunkManagerSchematic.getChunk(chunkX, chunkZ).addEntity(entity);
            ++this.entityCount;

            return true;
        }
    }

    public void unloadedEntities(int count)
    {
        this.entityCount -= count;
    }

    @Nullable
    @Override
    public Entity getEntityById(int id)
    {
        // This shouldn't be used for anything in the mod, so just return null here
        return null;
    }

    @Override
    public List<? extends PlayerEntity> getPlayers()
    {
        return ImmutableList.of();
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
    public void putMapState(String name, MapState mapState)
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
    protected EntityLookup<Entity> getEntityLookup()
    {
        // This is not used in the mod
        return null;
    }

    @Override
    public List<Entity> getOtherEntities(@Nullable final Entity except, final Box box, Predicate<? super Entity> predicate)
    {
        final List<Entity> entities = new ArrayList<>();
        List<ChunkSchematic> chunks = this.getChunksWithinBox(box);

        for (ChunkSchematic chunk : chunks)
        {
            chunk.getEntityList().forEach((e) -> {
                if (e != except && box.intersects(e.getBoundingBox()) && predicate.test(e)) {
                    entities.add(e);
                }
            });
        }

        return entities;
    }

    @Override
    public <T extends Entity> List<T> getEntitiesByType(TypeFilter<Entity, T> arg, Box box, Predicate<? super T> predicate)
    {
        ArrayList<T> list = new ArrayList<>();

        for (Entity e : this.getOtherEntities(null, box, e -> true))
        {
            T t = arg.downcast(e);

            if (t != null && predicate.test(t))
            {
                list.add(t);
            }
        }

        return list;
    }

    public List<ChunkSchematic> getChunksWithinBox(Box box)
    {
        final int minX = MathHelper.floor(box.minX / 16.0);
        final int minZ = MathHelper.floor(box.minZ / 16.0);
        final int maxX = MathHelper.floor(box.maxX / 16.0);
        final int maxZ = MathHelper.floor(box.maxZ / 16.0);

        List<ChunkSchematic> chunks = new ArrayList<>();

        for (int cx = minX; cx <= maxX; ++cx)
        {
            for (int cz = minZ; cz <= maxZ; ++cz)
            {
                ChunkSchematic chunk = this.chunkManagerSchematic.getChunkIfExists(cx, cz);

                if (chunk != null)
                {
                    chunks.add(chunk);
                }
            }
        }

        return chunks;
    }

    @Override
    public void scheduleBlockRerenderIfNeeded(BlockPos pos, BlockState stateOld, BlockState stateNew)
    {
        if (stateNew != stateOld)
        {
            this.scheduleChunkRenders(pos.getX() >> 4, pos.getZ() >> 4);
        }
    }

    public void scheduleChunkRenders(int chunkX, int chunkZ)
    {
        if (this.worldRenderer != null)
        {
            this.worldRenderer.scheduleChunkRenders(chunkX, chunkZ);
        }
    }

    @Override
    public int getBottomY()
    {
        return this.mc.world != null ? this.mc.world.getBottomY() : -64;
    }

    @Override
    public int getHeight()
    {
        return this.mc.world != null ? this.mc.world.getHeight() : 384;
    }

    // The following HeightLimitView overrides are to work around an incompatibility with Lithium 0.7.4+

    @Override
    public int getTopY()
    {
        return this.getBottomY() + this.getHeight();
    }

    @Override
    public int getBottomSectionCoord()
    {
        return this.getBottomY() >> 4;
    }

    @Override
    public int getTopSectionCoord()
    {
        return this.getTopY() >> 4;
    }

    @Override
    public int countVerticalSections()
    {
        return this.getTopSectionCoord() - this.getBottomSectionCoord();
    }

    @Override
    public boolean isOutOfHeightLimit(BlockPos pos)
    {
        return this.isOutOfHeightLimit(pos.getY());
    }

    @Override
    public boolean isOutOfHeightLimit(int y)
    {
        return (y < this.getBottomY()) || (y >= this.getTopY());
    }

    @Override
    public int getSectionIndex(int y)
    {
        return (y >> 4) - (this.getBottomY() >> 4);
    }

    @Override
    public int sectionCoordToIndex(int coord)
    {
        return coord - (this.getBottomY() >> 4);
    }

    @Override
    public int sectionIndexToCoord(int index)
    {
        return index + (this.getBottomY() >> 4);
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
    public void emitGameEvent(GameEvent event, Vec3d pos, @Nullable GameEvent.Emitter emitter)
    {
        // NO-OP
    }

    @Override
    public void playSound(@Nullable PlayerEntity except, double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch, long seed)
    {
        // NO-OP
    }

    @Override
    public void playSoundFromEntity(@javax.annotation.Nullable PlayerEntity except, Entity entity, RegistryEntry<SoundEvent> sound, SoundCategory category, float volume, float pitch, long seed)
    {
        // NO-OP
    }

    @Override
    public void emitGameEvent(@Nullable Entity entity, GameEvent event, BlockPos pos)
    {
        // NO-OP
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
    public void playSound(@javax.annotation.Nullable PlayerEntity except, double x, double y, double z, RegistryEntry<SoundEvent> sound, SoundCategory category, float volume, float pitch, long seed)
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

    @Override
    public DynamicRegistryManager getRegistryManager()
    {
        return this.mc.world.getRegistryManager();
    }

    @Override
    public FeatureSet getEnabledFeatures()
    {
        return this.mc.world.getEnabledFeatures();
    }

    @Override
    public String asString()
    {
        return "Chunks[SCH] W: " + this.getChunkManager().getDebugString() + " E: " + this.getRegularEntityCount();
    }
}
