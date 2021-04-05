package fi.dy.masa.litematica;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import fi.dy.masa.malilib.event.InitializationHandler;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;

@Mod(Reference.MOD_ID)
public class Litematica
{
    public static final Logger logger = LogManager.getLogger(Reference.MOD_ID);

    public Litematica()
    {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(EventPriority.LOW, this::onClientSetup);
    }

    private void onClientSetup(final FMLClientSetupEvent event)
    {
        // Make sure the mod being absent on the other network side does not cause
        // the client to display the server as incompatible
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));

//        MinecraftForge.EVENT_BUS.register(new ForgeInputEventHandler());
//        MinecraftForge.EVENT_BUS.register(new ForgeRenderEventHandler());
//        MinecraftForge.EVENT_BUS.register(new ForgeTickEventHandler());
//        MinecraftForge.EVENT_BUS.register(new ForgeWorldEventHandler());

        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
//        ((InitializationHandler) InitializationHandler.getInstance()).onGameInitDone();
    }
}
