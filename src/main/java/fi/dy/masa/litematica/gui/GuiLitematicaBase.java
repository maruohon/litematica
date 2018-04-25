package fi.dy.masa.litematica.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import fi.dy.masa.litematica.config.gui.button.ButtonBase;
import fi.dy.masa.litematica.config.gui.button.ButtonEntry;
import fi.dy.masa.litematica.config.gui.button.IButtonActionListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

public abstract class GuiLitematicaBase extends GuiScreen
{
    protected static final String BUTTON_LABEL_ADD = TextFormatting.DARK_GREEN + "+" + TextFormatting.RESET;
    protected static final String BUTTON_LABEL_REMOVE = TextFormatting.DARK_RED + "-" + TextFormatting.RESET;

    protected static final int WHITE                = 0xFFFFFFFF;
    protected static final int TOOLTIP_BACKGROUND   = 0xB0000000;
    protected static final int COLOR_HORIZONTAL_BAR = 0xFF999999;
    protected static final int LEFT         = 20;
    protected static final int TOP          = 10;
    private final List<ButtonEntry<?>> buttons = new ArrayList<>();
    private final List<GuiLabel> labels = new ArrayList<>();
    private final List<InfoWidget> infoWidgets = new ArrayList<>();

    public GuiLitematicaBase()
    {
    }

    protected abstract String getTitle();

    public Minecraft getMinecraft()
    {
        return this.mc;
    }

    @Override
    public void onGuiClosed()
    {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame()
    {
        return false;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        this.clearButtons();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawPanel(mouseX, mouseY, partialTicks);

        this.drawContents(mouseX, mouseY, partialTicks);

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (this.infoWidgets.isEmpty() == false)
        {
            for (InfoWidget widget : this.infoWidgets)
            {
                widget.render(mouseX, mouseY);
            }
        }
    }

    protected void drawContents(int mouseX, int mouseY, float partialTicks)
    {
    }

    @Override
    protected void actionPerformed(GuiButton control)
    {
        if (control.id == 0)
        {
            this.close();
        }
    }

    @Override
    public void handleMouseInput() throws IOException
    {
        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        int mouseWheelDelta = Mouse.getEventDWheel();

        if (mouseWheelDelta != 0)
        {
            this.mouseWheelScrolled(mouseX, mouseY, mouseWheelDelta);
        }

        super.handleMouseInput();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        for (ButtonEntry<?> entry : this.buttons)
        {
            if (entry.mousePressed(this.mc, mouseX, mouseY, mouseButton))
            {
                // Don't call super if the button press got handled
                return;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    protected void mouseWheelScrolled(int mouseX, int mouseY, int mouseWheelDelta)
    {
    }

    protected void addInfoWidget(InfoWidget widget)
    {
        this.infoWidgets.add(widget);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (keyCode == Keyboard.KEY_ESCAPE)
        {
            this.mc.displayGuiScreen(null);
        }
    }

    public void bindTexture(ResourceLocation texture)
    {
        this.mc.getTextureManager().bindTexture(texture);
    }

    protected <T extends ButtonBase> void addButton(T button, IButtonActionListener<T> listener)
    {
        this.buttons.add(new ButtonEntry<>(button, listener));
    }

    protected void addLabel(int id, int x, int y, int width, int height, int colour, String... lines)
    {
        if (lines != null && lines.length >= 1)
        {
            GuiLabel label = new GuiLabel(this.mc.fontRenderer, id, x, y, width, height, colour);

            for (String line : lines)
            {
                label.addLine(line);
            }

            this.labels.add(label);
        }
    }

    protected void clearButtons()
    {
        this.buttons.clear();
    }

    private boolean stealFocus()
    {
        return true;
    }

    public void close()
    {
        this.mc.displayGuiScreen(null);
    }

    private void drawPanel(int mouseX, int mouseY, float partialTicks)
    {
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.pushMatrix();

        // Draw the dark background
        drawRect(0, 0, this.width, this.height, TOOLTIP_BACKGROUND);
        // Draw the left edge
        //drawRect(LEFT_EDGE, 0, LEFT_EDGE + 1, this.height, WHITE);

        //GlStateManager.translate(LEFT_EDGE, 0, 0);
        this.draw(mouseX, mouseY, partialTicks);

        if (this.stealFocus() == false)
        {
            // Draw other controls inside the transform so that they slide properly
            //super.drawScreen(mouseX, mouseY, partialTicks);
        }

        GlStateManager.popMatrix();

        //this.drawTooltips(mouseX, mouseY, partialTicks, active, xOffset, mouseOverTab);
    }

    protected void drawButtons(int mouseX, int mouseY, float partialTicks)
    {
        for (ButtonEntry<?> entry : this.buttons)
        {
            entry.draw(this.mc, mouseX, mouseY, partialTicks);
        }
    }

    protected void drawLabels(int mouseX, int mouseY, float partialTicks)
    {
        for (GuiLabel label : this.labels)
        {
            label.drawLabel(this.mc, mouseX, mouseY);
        }
    }

    /**
     * Draw the panel and chrome
     * 
     * @param mouseX
     * @param mouseY
     * @param partialTicks
     */
    private void draw(int mouseX, int mouseY, float partialTicks)
    {
        // Scroll position
        //this.innerTop = TOP - this.scrollBar.getValue();

        // Draw panel title
        this.mc.fontRenderer.drawString(this.getTitle(), LEFT, TOP, WHITE);

        // Draw top and bottom horizontal bars
        //drawRect(MARGIN, TOP - 4, this.innerWidth + MARGIN, TOP - 3, COLOR_HORIZONTAL_BAR);
        //drawRect(MARGIN, this.height - BOTTOM + 2, this.innerWidth + MARGIN, this.height - BOTTOM + 3, COLOR_HORIZONTAL_BAR);

        // Clip rect
        //glEnableClipping(MARGIN, this.innerWidth, TOP, this.height - BOTTOM);

        // Offset by scroll
        GlStateManager.pushMatrix();
        //GlStateManager.translate(MARGIN, this.innerTop, 0.0F);

        // Draw panel contents
        //this.mainPanel.drawPanel(this.host, mouseX - MARGIN - (this.mouseOverPanel(mouseX, mouseY) ? 0 : 99999), mouseY - this.innerTop, partialTicks);
        //this.drawButtons(mouseX - MARGIN - (this.mouseOverPanel(mouseX, mouseY) ? 0 : 99999), mouseY - this.innerTop, partialTicks);
        this.drawLabels(mouseX, mouseY, partialTicks);
        this.drawButtons(mouseX, mouseY, partialTicks);
        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);

        // Disable clip rect
        //glDisableClipping();

        // Restore transform
        GlStateManager.popMatrix();

        /*
        // Get total scroll height from panel
        this.totalHeight = Math.max(-1, this.mainPanel.getContentHeight());

        // Update and draw scroll bar
        this.scrollBar.setMaxValue(this.totalHeight - this.innerHeight);
        this.scrollBar.drawScrollBar(mouseX, mouseY, partialTicks, this.innerWidth + MARGIN - 5, TOP, 5, this.innerHeight,
                Math.max(this.innerHeight, this.totalHeight));
        */
    }

    public static void drawOutlinedBox(int x, int y, int width, int height, int colorBg, int colorBorder)
    {
        // Draw the background
        drawRect(x, y, x + width, y + height, colorBg);

        // Draw the border
        drawOutline(x, y, width, height, colorBorder);
    }

    public static void drawOutline(int x, int y, int width, int height, int colorBorder)
    {
        int right = x + width;
        int bottom = y + height;

        drawRect(x - 1,  y - 1,         x, bottom + 1, colorBorder); // left edge
        drawRect(right,  y - 1, right + 1, bottom + 1, colorBorder); // right edge
        drawRect(    x,  y - 1,     right,          y, colorBorder); // top edge
        drawRect(    x, bottom,     right, bottom + 1, colorBorder); // bottom edge
    }

    public static void drawTexturedRect(int x, int y, int u, int v, int width, int height, float zLevel)
    {
        float pixelWidth = 0.00390625F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        buffer.pos(x        , y + height, zLevel).tex( u          * pixelWidth, (v + height) * pixelWidth).endVertex();
        buffer.pos(x + width, y + height, zLevel).tex((u + width) * pixelWidth, (v + height) * pixelWidth).endVertex();
        buffer.pos(x + width, y         , zLevel).tex((u + width) * pixelWidth,  v           * pixelWidth).endVertex();
        buffer.pos(x        , y         , zLevel).tex( u          * pixelWidth,  v           * pixelWidth).endVertex();

        tessellator.draw();
    }
}
