package fi.dy.masa.litematica.gui.base;

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
    public GuiLitematicaBase setParent(GuiLitematicaBase parent)
    {
        if (this.widget != null)
        {
            this.widget.setParent(parent);
        }

        return super.setParent(parent);
    }

    @Override
    public void initGui()
    {
        super.initGui();

        this.widget.setSize(this.getBrowserWidth(), this.getBrowserHeight());
        this.widget.initGui();
    }

    @Override
    public void onGuiClosed()
    {
        super.onGuiClosed();

        this.widget.onGuiClosed();
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        if (this.widget.onMouseClicked(mouseX, mouseY, mouseButton))
        {
            return true;
        }

        return super.onMouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean onMouseReleased(int mouseX, int mouseY, int mouseButton)
    {
        if (this.widget.onMouseReleased(mouseX, mouseY, mouseButton))
        {
            return true;
        }

        return super.onMouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean onMouseScrolled(int mouseX, int mouseY, int mouseWheelDelta)
    {
        if (this.widget.onMouseScrolled(mouseX, mouseY, mouseWheelDelta))
        {
            return true;
        }

        return super.onMouseScrolled(mouseX, mouseY, mouseWheelDelta);
    }

    @Override
    public boolean  onKeyTyped(char typedChar, int keyCode)
    {
        if (this.widget.onKeyTyped(typedChar, keyCode))
        {
            return true;
        }

        return super.onKeyTyped(typedChar, keyCode);
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
