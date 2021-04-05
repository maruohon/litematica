package fi.dy.masa.litematica.world;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.lighting.IWorldLightListener;
import net.minecraft.world.lighting.WorldLightManager;

public class FakeLightingProvider extends WorldLightManager
{
    private final FakeLightingView lightingView;

    public FakeLightingProvider()
    {
        super(null, false, false);

        this.lightingView = new FakeLightingView();
    }

    @Override
    public IWorldLightListener get(LightType type)
    {
        return this.lightingView;
    }

    @Override
    public int getLight(BlockPos pos, int defaultValue)
    {
        return 15;
    }

    public static class FakeLightingView implements IWorldLightListener
    {
        @Override
        public void setSectionStatus(SectionPos var1, boolean var2)
        {
            // NO-OP
        }

        @Override
        public NibbleArray getLightSection(SectionPos var1)
        {
            return null;
        }

        @Override
        public int getLightLevel(BlockPos pos)
        {
            return 15;
        }
    }
}
