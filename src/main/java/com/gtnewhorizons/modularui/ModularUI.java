package com.gtnewhorizons.modularui;

import com.gtnewhorizons.modularui.api.UIInfos;
import com.gtnewhorizons.modularui.api.screen.ModularUIContext;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.internal.JsonLoader;
import com.gtnewhorizons.modularui.common.internal.network.NetworkHandler;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularGui;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularUIContainer;
import com.gtnewhorizons.modularui.common.widget.WidgetJsonRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.function.Function;
import net.minecraft.entity.player.EntityPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = ModularUI.MODID,
        version = Tags.VERSION,
        name = Tags.MODNAME,
        acceptedMinecraftVersions = "[1.7.10]",
        dependencies = ModularUI.DEPENDENCIES,
        guiFactory = ModularUI.GUI_FACTORY)
public class ModularUI {

    public static final String MODID = "modularui";
    public static final String DEPENDENCIES = "required-after:CodeChickenLib; required-after:NotEnoughItems;";
    public static final String GUI_FACTORY = Tags.GROUPNAME + ".config.GuiFactory";

    public static final Logger logger = LogManager.getLogger(Tags.MODID);

    @Mod.Instance(ModularUI.MODID)
    public static ModularUI INSTANCE;

    @SidedProxy(
            modId = MODID,
            clientSide = Tags.GROUPNAME + ".ClientProxy",
            serverSide = Tags.GROUPNAME + ".CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
        NetworkHandler.init();
        UIInfos.init();
        WidgetJsonRegistry.init();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (FMLCommonHandler.instance().getSide() == Side.SERVER) {
            JsonLoader.loadJson();
        }
    }

    @Mod.EventHandler
    public void onPostInit(FMLPostInitializationEvent event) {
        proxy.postInit();
    }

    public static ModularUIContainer createContainer(
            EntityPlayer player, Function<UIBuildContext, ModularWindow> windowCreator) {
        UIBuildContext buildContext = new UIBuildContext(player);
        ModularWindow window = windowCreator.apply(buildContext);
        return new ModularUIContainer(new ModularUIContext(buildContext), window);
    }

    @SideOnly(Side.CLIENT)
    public static ModularGui createGuiScreen(
            EntityPlayer player, Function<UIBuildContext, ModularWindow> windowCreator) {
        return new ModularGui(createContainer(player, windowCreator));
    }
}