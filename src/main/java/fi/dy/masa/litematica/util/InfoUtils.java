package fi.dy.masa.litematica.util;

import fi.dy.masa.litematica.interfaces.IStringConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TextComponentTranslation;

public class InfoUtils
{
    public static final IStringConsumer INFO_MESSAGE_CONSUMER = new InfoMessageConsumer();

    public static class InfoMessageConsumer implements IStringConsumer
    {
        @Override
        public void setString(String string)
        {
            TextComponentTranslation message = new TextComponentTranslation(string);
            Minecraft.getMinecraft().ingameGUI.addChatMessage(ChatType.GAME_INFO, message);
        }
    }
}
