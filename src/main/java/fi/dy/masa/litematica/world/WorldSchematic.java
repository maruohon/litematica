package fi.dy.masa.litematica.world;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumLightType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.dimension.DimensionType;

public class WorldSchematic extends WorldClient
{
    private final Minecraft mc;
    private ChunkProviderSchematic chunkProviderSchematic;

    public WorldSchematic(NetHandlerPlayClient netHandler, WorldSettings settings, DimensionType dimType,
            EnumDifficulty difficulty, Profiler profilerIn)
    {
        super(netHandler, settings, dimType, difficulty, profilerIn);

        this.mc = Minecraft.getInstance();
    }

    @Override
    protected IChunkProvider createChunkProvider()
    {
        this.chunkProviderSchematic = new ChunkProviderSchematic(this);
        return this.chunkProviderSchematic;
    }

    @Override
    public ChunkProviderSchematic getChunkProvider()
    {
        return this.chunkProviderSchematic;
    }

    @Override
    public boolean setBlockState(BlockPos pos, IBlockState newState, int flags)
    {
        if (pos.getY() < 0 || pos.getY() >= 256)
        {
            return false;
        }
        else
        {
            return this.getChunk(pos).setBlockState(pos, newState, false) != null;
        }
    }

    // This override is just to get rid of the annoying Minecart sounds >_>
    @Override
    public boolean spawnEntity(Entity entityIn)
    {
        return this.spawnEntityBase(entityIn);
    }

    private boolean spawnEntityBase(Entity entityIn)
    {
        int cx = MathHelper.floor(entityIn.posX / 16.0D);
        int cy = MathHelper.floor(entityIn.posZ / 16.0D);
        boolean forceSpawn = entityIn.forceSpawn;

        if (entityIn instanceof EntityPlayer)
        {
            forceSpawn = true;
        }

        if (forceSpawn == false && this.isChunkLoaded(cx, cy, false) == false)
        {
            return false;
        }
        else
        {
            if (entityIn instanceof EntityPlayer)
            {
                EntityPlayer entityplayer = (EntityPlayer)entityIn;
                this.playerEntities.add(entityplayer);
                this.updateAllPlayersSleepingFlag();
            }

            this.getChunk(cx, cy).addEntity(entityIn);
            this.loadedEntityList.add(entityIn);
            this.onEntityAdded(entityIn);
            return true;
        }
    }

    @Override
    public long getGameTime()
    {
        return this.mc.world != null ? this.mc.world.getGameTime() : 0;
    }

    @Override
    public int getLight(BlockPos pos)
    {
        return 15;
    }

    @Override
    public int getLightFor(EnumLightType type, BlockPos pos)
    {
        return 15;
    }

    @Override
    public int getLightFromNeighborsFor(EnumLightType type, BlockPos pos)
    {
        return 15;
    }

    @Override
    public boolean checkLight(BlockPos pos)
    {
        return false;
    }

    @Override
    public boolean checkLightFor(EnumLightType lightType, BlockPos pos)
    {
        return false;
    }

    @Override
    public void playBroadcastSound(int id, BlockPos pos, int data)
    {
        // NO-OP
    }

    @Override
    public void playRecord(BlockPos blockPositionIn, SoundEvent soundEventIn)
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
    public void playSound(EntityPlayer player, BlockPos pos, SoundEvent soundIn, SoundCategory category, float volume, float pitch)
    {
        // NO-OP
    }

    @Override
    public void playSound(EntityPlayer player, double x, double y, double z, SoundEvent soundIn, SoundCategory category, float volume, float pitch)
    {
        // NO-OP
    }

    @Override
    public void playEvent(EntityPlayer player, int type, BlockPos pos, int data)
    {
        // NO-OP
    }

    @Override
    public void playEvent(int type, BlockPos pos, int data)
    {
        // NO-OP
    }
}
