package fi.dy.masa.litematica.mixin;

import java.nio.ByteOrder;
import java.nio.IntBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import fi.dy.masa.litematica.interfaces.IBufferBuilder;
import net.minecraft.client.renderer.BufferBuilder;

@Mixin(BufferBuilder.class)
public class MixinBufferBuilder implements IBufferBuilder
{
    @Shadow
    private IntBuffer rawIntBuffer;

    @Shadow
    private int getColorIndex(int vertexIndex) { return 0; }

    @Override
    public int getColorIndexAccessor(int vertexIndex)
    {
        return this.getColorIndex(vertexIndex);
    }

    @Override
    public void putColorRGBA(int index, int red, int green, int blue, int alpha)
    {
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN)
        {
            this.rawIntBuffer.put(index, alpha << 24 | blue << 16 | green << 8 | red);
        }
        else
        {
            this.rawIntBuffer.put(index, red << 24 | green << 16 | blue << 8 | alpha);
        }
    }
}
