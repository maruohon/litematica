package litematica.world;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.IChunkProvider;

import malilib.util.game.wrap.EntityWrap;
import litematica.mixin.IMixinWorldClient;

public class WorldSchematic extends WorldClient
{
    public WorldSchematic(NetHandlerPlayClient netHandler, WorldSettings settings, int dimension,
            EnumDifficulty difficulty, Profiler profilerIn)
    {
        super(netHandler, settings, dimension, difficulty, profilerIn);
    }

    @Override
    protected IChunkProvider createChunkProvider()
    {
        ChunkProviderSchematic provider = new ChunkProviderSchematic(this);

        ((IMixinWorldClient) (Object) this).setClientChunkProvider(provider);

        return provider;
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
            return this.getChunk(pos).setBlockState(pos, newState) != null;
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
        int cx = EntityWrap.getChunkX(entityIn);
        int cy = EntityWrap.getChunkZ(entityIn);
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
    public int getLight(BlockPos pos)
    {
        return 15;
    }

    @Override
    public int getLight(BlockPos pos, boolean checkNeighbors)
    {
        return 15;
    }

    @Override
    public float getLightBrightness(BlockPos pos)
    {
        return 1f;
    }

    @Override
    public int getLightFor(EnumSkyBlock type, BlockPos pos)
    {
        return 15;
    }

    @Override
    public int getLightFromNeighbors(BlockPos pos)
    {
        return 15;
    }

    @Override
    public int getLightFromNeighborsFor(EnumSkyBlock type, BlockPos pos)
    {
        return 15;
    }

    @Override
    public boolean checkLight(BlockPos pos)
    {
        return false;
    }

    @Override
    public boolean checkLightFor(EnumSkyBlock lightType, BlockPos pos)
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
