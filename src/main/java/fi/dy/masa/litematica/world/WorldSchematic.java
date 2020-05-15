package fi.dy.masa.litematica.world;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.render.schematic.WorldRendererSchematic;

public class WorldSchematic extends ClientWorld
{
    private final MinecraftClient mc;
    private final WorldRendererSchematic worldRenderer;
    private ChunkProviderSchematic chunkProviderSchematic;
    private int nextEntityId;

    public WorldSchematic(ClientPlayNetworkHandler clientPlayNetworkHandler, class_5271 arg, DimensionType dimensionType, Supplier<Profiler> supplier)
    {
        super(clientPlayNetworkHandler, arg, dimensionType, 1, supplier, null, true, 0L);

        this.mc = MinecraftClient.getInstance();
        this.worldRenderer = LitematicaRenderer.getInstance().getWorldRenderer();
        this.chunkProviderSchematic = new ChunkProviderSchematic(this);
    }

    public ChunkProviderSchematic getChunkProvider()
    {
        return this.chunkProviderSchematic;
    }

    @Override
    public ClientChunkManager getChunkManager()
    {
        return this.getChunkProvider();
    }

    @Override
    public WorldChunk getWorldChunk(BlockPos pos)
    {
        return this.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    @Override
    public WorldChunk getChunk(int chunkX, int chunkZ)
    {
        return this.chunkProviderSchematic.getChunk(chunkX, chunkZ);
    }

    @Override
    public Chunk getChunk(int chunkX, int chunkZ, ChunkStatus status, boolean required)
    {
        return this.getChunk(chunkX, chunkZ);
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

    private boolean spawnEntityBase(Entity entityIn)
    {
        int cx = MathHelper.floor(entityIn.getX() / 16.0D);
        int cz = MathHelper.floor(entityIn.getZ() / 16.0D);

        if (this.chunkProviderSchematic.isChunkLoaded(cx, cz) == false)
        {
            return false;
        }
        else
        {
            entityIn.setEntityId(this.nextEntityId++);

            super.addEntity(entityIn.getEntityId(), entityIn);

            return true;
        }
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
    public void checkBlockRerender(BlockPos pos, BlockState stateOld, BlockState stateNew)
    {
        this.scheduleBlockRenders(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
    }

    @Override
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
    public void playSound(BlockPos pos, SoundEvent soundIn, SoundCategory category, float volume, float pitch, boolean distanceDelay)
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
}
