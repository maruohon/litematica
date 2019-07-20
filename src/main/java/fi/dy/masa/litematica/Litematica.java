package fi.dy.masa.litematica;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLFingerprintViolationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import fi.dy.masa.litematica.event.RenderHandler;
import fi.dy.masa.malilib.event.InitializationHandler;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.MOD_VERSION, certificateFingerprint = Reference.FINGERPRINT,
    guiFactory = "fi.dy.masa.litematica.config.gui.LitematicaGuiFactory",
    updateJSON = "https://raw.githubusercontent.com/maruohon/litematica/forge_1.12.2/update.json",
    clientSideOnly=true, acceptedMinecraftVersions = "1.12.2", dependencies = "required-after:malilib;")
public class Litematica
{
    @Mod.Instance(Reference.MOD_ID)
    public static Litematica instance;

    public static final Logger logger = LogManager.getLogger(Reference.MOD_ID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
        MinecraftForge.EVENT_BUS.register(RenderHandler.INSTANCE);
    }

    @Mod.EventHandler
    public void onFingerPrintViolation(FMLFingerprintViolationEvent event)
    {
        // Not running in a dev environment
        if (event.isDirectory() == false)
        {
            logger.warn("*********************************************************************************************");
            logger.warn("*****                                    WARNING                                        *****");
            logger.warn("*****                                                                                   *****");
            logger.warn("*****   The signature of the mod file '{}' does not match the expected fingerprint!     *****", event.getSource().getName());
            logger.warn("*****   This might mean that the mod file has been tampered with!                       *****");
            logger.warn("*****   If you did not download the mod {} directly from Curse/CurseForge,       *****", Reference.MOD_NAME);
            logger.warn("*****   or using one of the well known launchers, and you did not                       *****");
            logger.warn("*****   modify the mod file at all yourself, then it's possible,                        *****");
            logger.warn("*****   that it may contain malware or other unwanted things!                           *****");
            logger.warn("*********************************************************************************************");
        }
    }
}
