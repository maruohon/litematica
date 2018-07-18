package fi.dy.masa.litematica.gui.base;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import fi.dy.masa.litematica.gui.interfaces.IMessageConsumer;
import fi.dy.masa.litematica.gui.interfaces.ITextFieldListener;
import fi.dy.masa.litematica.gui.widgets.WidgetInfo;
import fi.dy.masa.litematica.interfaces.IStringConsumer;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonHoverText;
import fi.dy.masa.malilib.gui.button.ButtonWrapper;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

public abstract class GuiLitematicaBase extends GuiScreen implements IMessageConsumer, IStringConsumer
{
    public static final String TXT_BLUE = TextFormatting.BLUE.toString();
    public static final String TXT_GREEN = TextFormatting.GREEN.toString();
    public static final String TXT_ORANGE = TextFormatting.GOLD.toString();
    public static final String TXT_RED = TextFormatting.RED.toString();
    public static final String TXT_WHITE = TextFormatting.WHITE.toString();
    public static final String TXT_BOLD = TextFormatting.BOLD.toString();
    public static final String TXT_RST = TextFormatting.RESET.toString();

    public static final String TXT_DARK_GREEN = TextFormatting.DARK_GREEN.toString();
    public static final String TXT_DARK_RED = TextFormatting.DARK_RED.toString();

    protected static final String BUTTON_LABEL_ADD = TextFormatting.DARK_GREEN + "+" + TextFormatting.RESET;
    protected static final String BUTTON_LABEL_REMOVE = TextFormatting.DARK_RED + "-" + TextFormatting.RESET;

    public static final int COLOR_WHITE          = 0xFFFFFFFF;
    public static final int TOOLTIP_BACKGROUND   = 0x80000000;
    public static final int COLOR_HORIZONTAL_BAR = 0xFF999999;
    protected static final int LEFT         = 20;
    protected static final int TOP          = 10;
    private final List<ButtonWrapper<? extends ButtonBase>> buttons = new ArrayList<>();
    private final List<TextFieldEntry<? extends GuiTextField>> textFields = new ArrayList<>();
    private final List<GuiLabel> labels = new ArrayList<>();
    private final List<WidgetInfo> infoWidgets = new ArrayList<>();
    private final List<Message> messages = new ArrayList<>();
    private InfoType nextMessageType = InfoType.INFO;
    protected String title = "";
    @Nullable
    private GuiLitematicaBase parent;

    public GuiLitematicaBase()
    {
    }

    public GuiLitematicaBase setParent(@Nullable GuiLitematicaBase parent)
    {
        // Don't allow nesting the GUI with itself...
        if (parent == null || parent.getClass() != this.getClass())
        {
            this.parent = parent;
        }

        return this;
    }

    @Nullable
    public GuiLitematicaBase getParent()
    {
        return this.parent;
    }

    protected String getTitle()
    {
        return this.parent != null ? this.parent.getTitle() + " => " + this.title : this.title;
    }

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

        this.labels.clear();
        this.clearButtons();
        this.clearTextFields();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawPanel(mouseX, mouseY, partialTicks);

        this.drawContents(mouseX, mouseY, partialTicks);

        super.drawScreen(mouseX, mouseY, partialTicks);

        this.drawGuiMessages();

        this.drawButtonHoverTexts(mouseX, mouseY, partialTicks);

        if (this.infoWidgets.isEmpty() == false)
        {
            for (WidgetInfo widget : this.infoWidgets)
            {
                widget.render(mouseX, mouseY, false);
            }
        }
    }

    public void drawContents(int mouseX, int mouseY, float partialTicks)
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

        if (mouseWheelDelta == 0 || this.onMouseScrolled(mouseX, mouseY, mouseWheelDelta) == false)
        {
            super.handleMouseInput();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        if (this.onMouseClicked(mouseX, mouseY, mouseButton) == false)
        {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int mouseButton)
    {
        if (this.onMouseReleased(mouseX, mouseY, mouseButton) == false)
        {
            super.mouseReleased(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (this.onKeyTyped(typedChar, keyCode) == false)
        {
            super.keyTyped(typedChar, keyCode);
        }
    }

    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        for (ButtonWrapper<?> entry : this.buttons)
        {
            if (entry.mousePressed(this.mc, mouseX, mouseY, mouseButton))
            {
                // Don't call super if the button press got handled
                return true;
            }
        }

        boolean handled = false;

        for (TextFieldEntry<?> entry : this.textFields)
        {
            if (entry.mouseClicked(mouseX, mouseY, mouseButton))
            {
                // Don't call super if the button press got handled
                handled = true;
            }
            else
            {
                entry.getTextField().setFocused(false);
            }
        }

        return handled;
    }

    public boolean onMouseReleased(int mouseX, int mouseY, int mouseButton)
    {
        return false;
    }

    public boolean onMouseScrolled(int mouseX, int mouseY, int mouseWheelDelta)
    {
        return false;
    }

    public boolean onKeyTyped(char typedChar, int keyCode)
    {
        if (keyCode == Keyboard.KEY_ESCAPE)
        {
            if (GuiScreen.isShiftKeyDown())
            {
                this.mc.displayGuiScreen(null);
            }
            else
            {
                this.mc.displayGuiScreen(this.parent);
            }

            return true;
        }

        boolean handled = false;
        int selected = -1;
        int i = 0;

        for (TextFieldEntry<?> entry : this.textFields)
        {
            if (keyCode == Keyboard.KEY_TAB && entry.getTextField().isFocused())
            {
                entry.getTextField().setFocused(false);
                selected = i;
                handled = true;
            }
            else if (entry.keyTyped(typedChar, keyCode))
            {
                handled = true;
            }

            i++;
        }

        if (selected >= 0)
        {
            if (GuiScreen.isShiftKeyDown())
            {
                selected = selected > 0 ? selected - 1 : this.textFields.size() - 1;
            }
            else
            {
                selected = (selected + 1) % this.textFields.size();
            }

            this.textFields.get(selected).getTextField().setFocused(true);
        }

        return handled;
    }

    protected void addInfoWidget(WidgetInfo widget)
    {
        this.infoWidgets.add(widget);
    }

    @Override
    public void setString(String string)
    {
        this.addGuiMessage(this.nextMessageType, string, 3000);
    }

    @Override
    public void addMessage(InfoType type, String messageKey)
    {
        this.addMessage(type, messageKey, new Object[0]);
    }

    @Override
    public void addMessage(InfoType type, String messageKey, Object... args)
    {
        this.addGuiMessage(type, messageKey, 3000, args);
    }

    public void addGuiMessage(InfoType type, String messageKey, int displayTimeMs, Object... args)
    {
        this.messages.add(new Message(type, displayTimeMs, 380, messageKey, args));
    }

    public void setNextMessageType(InfoType type)
    {
        this.nextMessageType = type;
    }

    protected void drawGuiMessages()
    {
        if (this.messages.isEmpty() == false)
        {
            int boxWidth = 400;
            int boxHeight = this.getMessagesHeight() + 20;
            int x = this.width / 2 - boxWidth / 2;
            int y = this.height / 2 - boxHeight / 2;

            drawOutlinedBox(x, y, boxWidth, boxHeight, 0xDD000000, COLOR_HORIZONTAL_BAR);
            x += 10;
            y += 10;

            for (int i = 0; i < this.messages.size(); ++i)
            {
                Message message = this.messages.get(i);
                y = message.renderAt(x, y, 0xFFFFFFFF);

                if (message.hasExpired())
                {
                    this.messages.remove(i);
                    --i;
                }
            }
        }
    }

    private int getMessagesHeight()
    {
        int height = 0;

        for (int i = 0; i < this.messages.size(); ++i)
        {
            height += this.messages.get(i).getMessageHeight();
        }

        return height;
    }

    public void bindTexture(ResourceLocation texture)
    {
        this.mc.getTextureManager().bindTexture(texture);
    }

    protected <T extends ButtonBase> ButtonWrapper<T> addButton(T button, IButtonActionListener<T> listener)
    {
        ButtonWrapper<T> entry = new ButtonWrapper<>(button, listener);
        this.buttons.add(entry);

        return entry;
    }

    protected <T extends GuiTextField> void addtextField(T textField, @Nullable ITextFieldListener<T> listener)
    {
        this.textFields.add(new TextFieldEntry<>(textField, listener));
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

    protected void clearTextFields()
    {
        this.textFields.clear();
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
        for (ButtonWrapper<?> entry : this.buttons)
        {
            entry.draw(this.mc, mouseX, mouseY, partialTicks);
        }
    }

    protected void drawButtonHoverTexts(int mouseX, int mouseY, float partialTicks)
    {
        for (ButtonWrapper<? extends ButtonBase> entry : this.buttons)
        {
            ButtonBase button = entry.getButton();

            if ((button instanceof ButtonHoverText) && button.isMouseOver())
            {
                this.drawHoveringText(((ButtonHoverText) button).getHoverStrings(), mouseX, mouseY);
            }
        }
    }

    protected void drawTextFields()
    {
        for (TextFieldEntry<?> entry : this.textFields)
        {
            entry.draw();
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
        this.mc.fontRenderer.drawString(this.getTitle(), LEFT, TOP, COLOR_WHITE);

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
        this.drawTextFields();
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

    public static boolean isMouseOver(int mouseX, int mouseY, int x, int y, int width, int height)
    {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
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

    public static void drawHoverText(int x, int y, List<String> textLines)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (textLines.isEmpty() == false && mc.currentScreen != null)
        {
            FontRenderer font = mc.fontRenderer;
            GlStateManager.disableRescaleNormal();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            int maxLineLength = 0;
            int maxWidth = mc.currentScreen.width;
            int maxHeight = mc.currentScreen.height;

            for (String s : textLines)
            {
                int length = font.getStringWidth(s);

                if (length > maxLineLength)
                {
                    maxLineLength = length;
                }
            }

            int textStartX = x + 12;
            int textStartY = y - 12;
            int textHeight = 8;

            if (textLines.size() > 1)
            {
                textHeight += 2 + (textLines.size() - 1) * 10;
            }

            if (textStartX + maxLineLength > maxWidth)
            {
                textStartX -= 28 + maxLineLength;
            }

            if (textStartY + textHeight + 6 > maxHeight)
            {
                textStartY = maxHeight - textHeight - 6;
            }

            double zLevel = 300;
            int borderColor = 0xF0100010;
            drawGradientRect(textStartX - 3, textStartY - 4, textStartX + maxLineLength + 3, textStartY - 3, zLevel, borderColor, borderColor);
            drawGradientRect(textStartX - 3, textStartY + textHeight + 3, textStartX + maxLineLength + 3, textStartY + textHeight + 4, zLevel, borderColor, borderColor);
            drawGradientRect(textStartX - 3, textStartY - 3, textStartX + maxLineLength + 3, textStartY + textHeight + 3, zLevel, borderColor, borderColor);
            drawGradientRect(textStartX - 4, textStartY - 3, textStartX - 3, textStartY + textHeight + 3, zLevel, borderColor, borderColor);
            drawGradientRect(textStartX + maxLineLength + 3, textStartY - 3, textStartX + maxLineLength + 4, textStartY + textHeight + 3, zLevel, borderColor, borderColor);

            int fillColor1 = 0x505000FF;
            int fillColor2 = 0x5028007F;
            drawGradientRect(textStartX - 3, textStartY - 3 + 1, textStartX - 3 + 1, textStartY + textHeight + 3 - 1, zLevel, fillColor1, fillColor2);
            drawGradientRect(textStartX + maxLineLength + 2, textStartY - 3 + 1, textStartX + maxLineLength + 3, textStartY + textHeight + 3 - 1, zLevel, fillColor1, fillColor2);
            drawGradientRect(textStartX - 3, textStartY - 3, textStartX + maxLineLength + 3, textStartY - 3 + 1, zLevel, fillColor1, fillColor1);
            drawGradientRect(textStartX - 3, textStartY + textHeight + 2, textStartX + maxLineLength + 3, textStartY + textHeight + 3, zLevel, fillColor2, fillColor2);

            for (int i = 0; i < textLines.size(); ++i)
            {
                String str = textLines.get(i);
                font.drawStringWithShadow(str, textStartX, textStartY, 0xFFFFFFFF);

                if (i == 0)
                {
                    textStartY += 2;
                }

                textStartY += 10;
            }

            GlStateManager.enableLighting();
            GlStateManager.enableDepth();
            RenderHelper.enableStandardItemLighting();
            GlStateManager.enableRescaleNormal();
        }
    }

    public static void drawGradientRect(int left, int top, int right, int bottom, double zLevel, int startColor, int endColor)
    {
        float sa = (float)(startColor >> 24 & 0xFF) / 255.0F;
        float sr = (float)(startColor >> 16 & 0xFF) / 255.0F;
        float sg = (float)(startColor >>  8 & 0xFF) / 255.0F;
        float sb = (float)(startColor & 0xFF) / 255.0F;

        float ea = (float)(endColor >> 24 & 0xFF) / 255.0F;
        float er = (float)(endColor >> 16 & 0xFF) / 255.0F;
        float eg = (float)(endColor >>  8 & 0xFF) / 255.0F;
        float eb = (float)(endColor & 0xFF) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        bufferbuilder.pos(right, top,    zLevel).color(sr, sg, sb, sa).endVertex();
        bufferbuilder.pos(left,  top,    zLevel).color(sr, sg, sb, sa).endVertex();
        bufferbuilder.pos(left,  bottom, zLevel).color(er, eg, eb, ea).endVertex();
        bufferbuilder.pos(right, bottom, zLevel).color(er, eg, eb, ea).endVertex();

        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public static class Message
    {
        public static final String RESET = TextFormatting.RESET.toString();
        public static final String GREEN = TextFormatting.GREEN.toString();
        public static final String GRAY = TextFormatting.GRAY.toString();
        public static final String WHITE = TextFormatting.WHITE.toString();
        public static final String GOLD = TextFormatting.GOLD.toString();
        public static final String RED = TextFormatting.RED.toString();
        private final InfoType type;
        private final long created;
        private final int displayTime;
        private final int maxLineWidth;
        private final List<String> message = new ArrayList<>();
        private final FontRenderer fontRenderer;

        public Message(InfoType type, int displayTimeMs, int maxLineWidth, String message, Object... args)
        {
            this.type = type;
            this.created = System.currentTimeMillis();
            this.displayTime = displayTimeMs;
            this.maxLineWidth = maxLineWidth;
            this.fontRenderer = Minecraft.getMinecraft().fontRenderer;

            this.setMessage(I18n.format(message, args));
        }

        public boolean hasExpired()
        {
            return System.currentTimeMillis() > (this.created + this.displayTime);
        }

        public int getMessageHeight()
        {
            return this.message.size() * (this.fontRenderer.FONT_HEIGHT + 1) - 1 + 5;
        }

        public void setMessage(String message)
        {
            this.message.clear();

            String[] arr = message.split(" ");
            StringBuilder sb = new StringBuilder(this.maxLineWidth + 32);
            final int spaceWidth = this.fontRenderer.getStringWidth(" ");
            int lineWidth = 0;

            for (String str : arr)
            {
                int width = this.fontRenderer.getStringWidth(str);

                if (lineWidth > 0 && (lineWidth + width + spaceWidth) > this.maxLineWidth)
                {
                    this.message.add(sb.toString());
                    sb = new StringBuilder(this.maxLineWidth + 32);
                    lineWidth = 0;
                }

                if (lineWidth > 0)
                {
                    sb.append(" ");
                }

                sb.append(str);
                lineWidth += width;
            }

            this.message.add(sb.toString());
        }

        /**
         * Renders the lines for this message
         * @return the y coordinate of the next message
         */
        public int renderAt(int x, int y, int textColor)
        {
            String format = this.getFormatCode();

            for (String text : this.message)
            {
                this.fontRenderer.drawString(format + text + RESET, x, y, textColor);
                y += this.fontRenderer.FONT_HEIGHT + 1;
            }

            return y + 3;
        }

        public String getFormatCode()
        {
            switch (this.type)
            {
                case INFO:      return GRAY;
                case SUCCESS:   return GREEN;
                case WARNING:   return GOLD;
                case ERROR:     return RED;
                default:        return GRAY;
            }
        }
    }

    public enum InfoType
    {
        INFO,
        SUCCESS,
        WARNING,
        ERROR;
    }

    public enum LeftRight
    {
        LEFT,
        RIGHT
    }
}
