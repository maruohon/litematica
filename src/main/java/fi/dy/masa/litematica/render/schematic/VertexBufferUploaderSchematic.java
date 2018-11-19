package fi.dy.masa.litematica.render.schematic;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.VertexBuffer;

public class VertexBufferUploaderSchematic
{
    private VertexBuffer vertexBuffer;

    public void draw(BufferBuilder bufferBuilder)
    {
        bufferBuilder.reset();
        this.vertexBuffer.bufferData(bufferBuilder.getByteBuffer());
    }

    public void setVertexBuffer(VertexBuffer buffer)
    {
        this.vertexBuffer = buffer;
    }
}
