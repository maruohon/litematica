package fi.dy.masa.litematica.gui.interfaces;

import fi.dy.masa.litematica.gui.base.GuiLitematicaBase.InfoType;

public interface IMessageConsumer
{
    void addMessage(InfoType type, String messageKey);

    void addMessage(InfoType type, String messageKey, Object... args);
}
