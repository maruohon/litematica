package fi.dy.masa.litematica.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
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
import net.minecraft.tag.TagManager;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.LightType;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.TickScheduler;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BuiltinBiomes;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.entity.EntityLookup;
import net.minecraft.world.event.GameEvent;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.render.schematic.WorldRendererSchematic;

public class WorldSchematic extends World
{
    private final MinecraftClient mc;
    private final WorldRendererSchematic worldRenderer;
    private final ChunkManagerSchematic chunkManagerSchematic;
    private int nextEntityId;
    private int entityCount;

    protected WorldSchematic(MutableWorldProperties mutableWorldProperties, DimensionType dimensionType, Supplier<Profiler> supplier)
    {
        super(mutableWorldProperties, null, dimensionType, supplier, true, true, 0L);

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
    public Biome getGeneratorStoredBiome(int biomeX, int biomeY, int biomeZ)
    {
        return BuiltinBiomes.PLAINS;
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
    public TagManager getTagManager()
    {
        return this.mc.world != null ? this.mc.world.getTagManager() : null;
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
        final int minY = MathHelper.floor(box.minY / 16.0);
        final int maxY = MathHelper.floor(box.maxY / 16.0);
        final List<Entity> entities = new ArrayList<>();
        List<ChunkSchematic> chunks = this.getChunksWithinBox(box);

        for (ChunkSchematic chunk : chunks)
        {
            for (int cy = minY; cy <= maxY; ++cy)
            {
                chunk.getEntityListForSectionIfExists(cy).forEach((e) -> {
                    if (e != except && box.intersects(e.getBoundingBox()) && predicate.test(e)) {
                        entities.add(e);
                    }
                });
            }
        }

        return entities;
    }

    @Override
    public <T extends Entity> List<T> getEntitiesByType(TypeFilter<Entity, T> arg, Box box, Predicate<? super T> predicate)
    {
        // This is not used in the mod, so just return an empty list...
        return Collections.emptyList();
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
        this.scheduleBlockRenders(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
    }

    public void scheduleBlockRenders(int chunkX, int chunkY, int chunkZ)
    {
        this.worldRenderer.scheduleChunkRenders(chunkX, chunkY, chunkZ);
    }

    public void scheduleChunkRenders(int chunkX, int chunkZ)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        int renderDistanceChunks = mc.options.viewDistance / 16 + 2;
        int cameraChunkY = (mc.gameRenderer.getCamera().getBlockPos().getY()) / 16;
        int startChunkY = Math.max(this.getBottomSectionCoord(), cameraChunkY - renderDistanceChunks);
        int endChunkY = Math.min(this.getTopSectionCoord(), cameraChunkY + renderDistanceChunks);

        for (int chunkY = startChunkY; chunkY < endChunkY; ++chunkY)
        {
            this.worldRenderer.scheduleChunkRenders(chunkX, chunkY, chunkZ);
        }
    }

    public void scheduleChunkRenders(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ)
    {
        final int minChunkX = Math.min(minBlockX, maxBlockX) >> 4;
        final int minChunkY = Math.min(minBlockY, maxBlockY) >> 4;
        final int minChunkZ = Math.min(minBlockZ, maxBlockZ) >> 4;
        final int maxChunkX = Math.max(minBlockX, maxBlockX) >> 4;
        final int maxChunkY = Math.max(minBlockY, maxBlockY) >> 4;
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
    public int getBottomY()
    {
        return this.mc.world != null ? this.mc.world.getBottomY() : -64;
    }

    @Override
    public int getHeight()
    {
        return this.mc.world != null ? this.mc.world.getHeight() : 384;
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
    public void emitGameEvent(@org.jetbrains.annotations.Nullable Entity entity, GameEvent event, BlockPos pos)
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
    public String asString()
    {
        return "Chunks[SCH] W: " + this.getChunkManager().getDebugString() + " E: " + this.getRegularEntityCount();
    }
}
