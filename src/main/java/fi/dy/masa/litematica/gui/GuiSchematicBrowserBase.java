package fi.dy.masa.litematica.gui;

import java.io.IOException;
import fi.dy.masa.litematica.interfaces.IStringConsumer;
import net.minecraft.client.Minecraft;

public abstract class GuiSchematicBrowserBase extends GuiLitematicaBase implements IStringConsumer
{
    protected final WidgetSchematicBrowser schematicBrowser;

    public GuiSchematicBrowserBase(int browserX, int browserY)
    {
        // The width and height will be set to the actual values in initGui()
        this.schematicBrowser = new WidgetSchematicBrowser(browserX, browserY, 100, 100, this);
    }

    @Override
    public void initGui()
    {
        super.initGui();

        this.schematicBrowser.setSize(this.getBrowserWidth(), this.getBrowserHeight());
        this.schematicBrowser.initGui();
    }

    protected int getBrowserWidth()
    {
        return this.width - 20;
    }

    protected int getBrowserHeight()
    {
        return this.height - 80;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        this.schematicBrowser.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int button)
    {
        this.schematicBrowser.mouseReleased(mouseX, mouseY, button);

        super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void mouseWheelScrolled(int mouseX, int mouseY, int mouseWheelDelta)
    {
        super.mouseWheelScrolled(mouseX, mouseY, mouseWheelDelta);

        this.schematicBrowser.mouseWheelScrolled(mouseX, mouseY, mouseWheelDelta);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        super.keyTyped(typedChar, keyCode);

        this.schematicBrowser.keyTyped(typedChar, keyCode);
    }

    @Override
    public void setWorldAndResolution(Minecraft mc, int width, int height)
    {
        super.setWorldAndResolution(mc, width, height);

        this.schematicBrowser.setWorldAndResolution(mc, width, height);
    }

    @Override
    public void drawContents(int mouseX, int mouseY, float partialTicks)
    {
        this.schematicBrowser.drawContents(mouseX, mouseY, partialTicks);
    }

    @Override
    public void setString(String string)
    {
    }
}
