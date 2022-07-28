package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nullable;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;

// Thanks plusls for this hack fix :p
public class OmegaHackfixForCrashJustTemporarilyForNowISwearBecauseOfShittyBrokenCodeBufferBuilder extends BufferBuilder
{
    @Nullable public BufferBuilder.BuiltBuffer lastRenderBuildBuffer;
    public boolean first = true;

    public OmegaHackfixForCrashJustTemporarilyForNowISwearBecauseOfShittyBrokenCodeBufferBuilder(int initialCapacity)
    {
        super(initialCapacity);
    }

    @Override
    public void begin(VertexFormat.DrawMode drawMode, VertexFormat format)
    {
        if (this.lastRenderBuildBuffer == null)
        {
            if (this.first == false)
            {
                this.end();
            }
            else
            {
                this.first = false;
            }
        }
        else
        {
            this.lastRenderBuildBuffer = null;
        }

        super.begin(drawMode, format);
    }

    @Override
    public BufferBuilder.BuiltBuffer end()
    {
        this.lastRenderBuildBuffer = super.end();
        return this.lastRenderBuildBuffer;
    }
}
