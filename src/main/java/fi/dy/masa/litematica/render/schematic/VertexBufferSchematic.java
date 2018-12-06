package fi.dy.masa.litematica.render.schematic;

import java.nio.ByteBuffer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;

public class VertexBufferSchematic extends VertexBuffer
{
    private int glBufferId;
    private final VertexFormat vertexFormat;
    private int count;

    public VertexBufferSchematic(VertexFormat vertexFormatIn)
    {
        super(vertexFormatIn);

        this.vertexFormat = vertexFormatIn;
        this.glBufferId = OpenGlHelper.glGenBuffers();
    }

    @Override
    public void bindBuffer()
    {
        OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, this.glBufferId);
    }

    @Override
    public void bufferData(ByteBuffer data)
    {
        this.bindBuffer();
        OpenGlHelper.glBufferData(OpenGlHelper.GL_ARRAY_BUFFER, data, 35044);
        this.unbindBuffer();
        this.count = data.limit() / this.vertexFormat.getSize();
    }

    @Override
    public void drawArrays(int mode)
    {
        GlStateManager.drawArrays(mode, 0, this.count);
    }

    @Override
    public void unbindBuffer()
    {
        OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);
    }

    @Override
    public void deleteGlBuffers()
    {
        super.deleteGlBuffers();

        if (this.glBufferId >= 0)
        {
            OpenGlHelper.glDeleteBuffers(this.glBufferId);
            this.glBufferId = -1;
        }
    }
}
