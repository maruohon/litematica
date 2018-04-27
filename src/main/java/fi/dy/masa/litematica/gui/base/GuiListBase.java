package fi.dy.masa.litematica.gui.base;

import java.io.IOException;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.gui.interfaces.ISelectionListener;
import fi.dy.masa.litematica.gui.widgets.base.WidgetBase;
import fi.dy.masa.litematica.gui.widgets.base.WidgetListBase;
import net.minecraft.client.Minecraft;

public abstract class GuiListBase<TYPE, WIDGET extends WidgetBase, WIDGETLIST extends WidgetListBase<TYPE, WIDGET>> extends GuiLitematicaBase
{
    protected final WIDGETLIST widget;

    protected GuiListBase(int listX, int listY)
    {
        this.widget = this.createListWidget(listX, listY);
    }

    protected abstract WIDGETLIST createListWidget(int listX, int listY);

    protected abstract int getBrowserWidth();

    protected abstract int getBrowserHeight();

    @Nullable
    protected ISelectionListener<TYPE> getSelectionListener()
    {
        return null;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        this.widget.setSize(this.getBrowserWidth(), this.getBrowserHeight());
        this.widget.initGui();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        this.widget.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int button)
    {
        this.widget.mouseReleased(mouseX, mouseY, button);

        super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void mouseWheelScrolled(int mouseX, int mouseY, int mouseWheelDelta)
    {
        super.mouseWheelScrolled(mouseX, mouseY, mouseWheelDelta);

        this.widget.mouseWheelScrolled(mouseX, mouseY, mouseWheelDelta);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        super.keyTyped(typedChar, keyCode);

        this.widget.keyTyped(typedChar, keyCode);
    }

    @Override
    public void setWorldAndResolution(Minecraft mc, int width, int height)
    {
        super.setWorldAndResolution(mc, width, height);

        this.widget.setWorldAndResolution(mc, width, height);
    }

    @Override
    public void drawContents(int mouseX, int mouseY, float partialTicks)
    {
        this.widget.drawContents(mouseX, mouseY, partialTicks);
    }
}
