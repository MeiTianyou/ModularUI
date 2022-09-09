package com.gtnewhorizons.modularui;

import codechicken.nei.guihook.GuiContainerManager;
import com.gtnewhorizons.modularui.common.internal.JsonLoader;
import com.gtnewhorizons.modularui.common.peripheral.PeripheralInputHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.SimpleReloadableResourceManager;

@SuppressWarnings("unused")
@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        GuiContainerManager.addInputHandler(new PeripheralInputHandler());
    }

    public void postInit() {
        super.postInit();
        ((SimpleReloadableResourceManager) Minecraft.getMinecraft().getResourceManager())
                .registerReloadListener(this::onReload);
    }

    public void onReload(IResourceManager manager) {
        ModularUI.logger.info("Reloading GUIs");
        JsonLoader.loadJson();
    }
}