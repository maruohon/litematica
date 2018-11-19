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
                        GlStateManager.glVertexPointer(format.getElementCount(), glConstant, size, bytebuffer);
                        GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
                        break;
                    case UV:
                        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit + format.getIndex());
                        GlStateManager.glTexCoordPointer(format.getElementCount(), glConstant, size, bytebuffer);
                        GlStateManager.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
                        break;
                    case COLOR:
                        GlStateManager.glColorPointer(format.getElementCount(), glConstant, size, bytebuffer);
                        GlStateManager.glEnableClientState(GL11.GL_COLOR_ARRAY);
                        break;
                    case NORMAL:
                        GlStateManager.glNormalPointer(glConstant, size, bytebuffer);
                        GlStateManager.glEnableClientState(GL11.GL_NORMAL_ARRAY);
                        break;
                    default:
                }
            }

            GlStateManager.glDrawArrays(bufferBuilder.getDrawMode(), 0, bufferBuilder.getVertexCount());
            final int count = elements.size();

            for (int i = 0; i < count; ++i)
            {
                VertexFormatElement format = elements.get(i);
                VertexFormatElement.EnumUsage usage = format.getUsage();

                switch (usage)
                {
                    case POSITION:
                        GlStateManager.glDisableClientState(GL11.GL_VERTEX_ARRAY);
                        break;
                    case UV:
                        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit + format.getIndex());
                        GlStateManager.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
                        break;
                    case COLOR:
                        GlStateManager.glDisableClientState(GL11.GL_COLOR_ARRAY);
                        GlStateManager.resetColor();
                        break;
                    case NORMAL:
                        GlStateManager.glDisableClientState(GL11.GL_NORMAL_ARRAY);
                        break;
                    default:
                }
            }
        }

        bufferBuilder.reset();
    }
}
