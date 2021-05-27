package fi.dy.masa.litematica.world;

import javax.annotation.Nullable;
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
        @Nullable
        @Override
        public NibbleArray getLightSection(SectionPos pos)
        {
            return null;
        }

        @Override
        public int getLightLevel(BlockPos pos)
        {
            return 15;
        }

        @Override
        public void setSectionStatus(SectionPos pos, boolean notReady)
        {
            // NO-OP
        }
    }
}
