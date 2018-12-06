package fi.dy.masa.litematica.render.schematic;

import java.nio.ByteBuffer;
import java.util.List;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;

public class DisplayListUploaderSchematic
{
    public void draw(BufferBuilder bufferBuilder)
    {
        if (bufferBuilder.getVertexCount() > 0)
        {
            VertexFormat vertexformat = bufferBuilder.getVertexFormat();
            int size = vertexformat.getSize();
            ByteBuffer bytebuffer = bufferBuilder.getByteBuffer();
            List<VertexFormatElement> elements = vertexformat.getElements();

            for (int i = 0; i < elements.size(); ++i)
            {
                VertexFormatElement format = elements.get(i);
                VertexFormatElement.EnumUsage usage = format.getUsage();
                int glConstant = format.getType().getGlConstant();
                bytebuffer.position(vertexformat.getOffset(i));

                switch (usage)
                {
                    case POSITION:
                        GlStateManager.vertexPointer(format.getElementCount(), glConstant, size, bytebuffer);
                        GlStateManager.enableClientState(GL11.GL_VERTEX_ARRAY);
                        break;
                    case UV:
                        OpenGlHelper.glClientActiveTexture(OpenGlHelper.GL_TEXTURE0 + format.getIndex());
                        GlStateManager.texCoordPointer(format.getElementCount(), glConstant, size, bytebuffer);
                        GlStateManager.enableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                        OpenGlHelper.glClientActiveTexture(OpenGlHelper.GL_TEXTURE0);
                        break;
                    case COLOR:
                        GlStateManager.colorPointer(format.getElementCount(), glConstant, size, bytebuffer);
                        GlStateManager.enableClientState(GL11.GL_COLOR_ARRAY);
                        break;
                    case NORMAL:
                        GlStateManager.normalPointer(glConstant, size, bytebuffer);
                        GlStateManager.enableClientState(GL11.GL_NORMAL_ARRAY);
                        break;
                    default:
                }
            }

            GlStateManager.drawArrays(bufferBuilder.getDrawMode(), 0, bufferBuilder.getVertexCount());
            final int count = elements.size();

            for (int i = 0; i < count; ++i)
            {
                VertexFormatElement format = elements.get(i);
                VertexFormatElement.EnumUsage usage = format.getUsage();

                switch (usage)
                {
                    case POSITION:
                        GlStateManager.disableClientState(GL11.GL_VERTEX_ARRAY);
                        break;
                    case UV:
                        OpenGlHelper.glClientActiveTexture(OpenGlHelper.GL_TEXTURE0 + format.getIndex());
                        GlStateManager.disableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                        OpenGlHelper.glClientActiveTexture(OpenGlHelper.GL_TEXTURE0);
                        break;
                    case COLOR:
                        GlStateManager.disableClientState(GL11.GL_COLOR_ARRAY);
                        GlStateManager.resetColor();
                        break;
                    case NORMAL:
                        GlStateManager.disableClientState(GL11.GL_NORMAL_ARRAY);
                        break;
                    default:
                }
            }
        }

        bufferBuilder.reset();
    }
}
