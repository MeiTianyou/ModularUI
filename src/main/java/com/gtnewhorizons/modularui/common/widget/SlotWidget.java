package com.gtnewhorizons.modularui.common.widget;

import com.gtnewhorizons.modularui.api.GlStateManager;
import com.gtnewhorizons.modularui.api.ModularUITextures;
import com.gtnewhorizons.modularui.api.NumberFormat;
import com.gtnewhorizons.modularui.api.drawable.Text;
import com.gtnewhorizons.modularui.api.drawable.TextRenderer;
import com.gtnewhorizons.modularui.api.forge.IItemHandlerModifiable;
import com.gtnewhorizons.modularui.api.math.Alignment;
import com.gtnewhorizons.modularui.api.math.Color;
import com.gtnewhorizons.modularui.api.math.Pos2d;
import com.gtnewhorizons.modularui.api.math.Size;
import com.gtnewhorizons.modularui.api.nei.IGhostIngredientHandler;
import com.gtnewhorizons.modularui.api.widget.*;
import com.gtnewhorizons.modularui.api.widget.IGhostIngredientTarget;
import com.gtnewhorizons.modularui.api.widget.IIngredientProvider;
import com.gtnewhorizons.modularui.api.widget.ISyncedWidget;
import com.gtnewhorizons.modularui.api.widget.IVanillaSlot;
import com.gtnewhorizons.modularui.api.widget.Interactable;
import com.gtnewhorizons.modularui.api.widget.Widget;
import com.gtnewhorizons.modularui.common.internal.Theme;
import com.gtnewhorizons.modularui.common.internal.wrapper.BaseSlot;
import com.gtnewhorizons.modularui.common.internal.wrapper.GhostIngredientWrapper;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularGui;
import com.gtnewhorizons.modularui.mixins.GuiContainerMixin;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumChatFormatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

public class SlotWidget extends Widget
        implements IVanillaSlot, Interactable, ISyncedWidget, IIngredientProvider, IGhostIngredientTarget {

    public static final Size SIZE = new Size(18, 18);

    private final TextRenderer textRenderer = new TextRenderer();
    private final BaseSlot slot;
    private ItemStack lastStoredPhantomItem = null;

    @Nullable
    private String sortAreaName = null;

    public SlotWidget(BaseSlot slot) {
        this.slot = slot;
    }

    public SlotWidget(IItemHandlerModifiable handler, int index) {
        this(new BaseSlot(handler, index, false));
    }

    public static SlotWidget phantom(IItemHandlerModifiable handler, int index) {
        return new SlotWidget(BaseSlot.phantom(handler, index));
    }

    @Override
    public void onInit() {
        getContext().getContainer().addSlotToContainer(this.slot);
        if (getBackground() == null) {
            setBackground(ModularUITextures.ITEM_SLOT);
        }
        if (!isClient() && this.slot.getStack() != null) {
            this.lastStoredPhantomItem = this.slot.getStack().copy();
        }
    }

    @Override
    public void onDestroy() {
        getContext().getContainer().removeSlot(this.slot);
    }

    @Override
    public BaseSlot getMcSlot() {
        return this.slot;
    }

    @Override
    protected @NotNull Size determineSize(int maxWidth, int maxHeight) {
        return SIZE;
    }

    @Override
    public @Nullable String getBackgroundColorKey() {
        return Theme.KEY_ITEM_SLOT;
    }

    @Override
    public void draw(float partialTicks) {
        RenderHelper.enableGUIStandardItemLighting();
        drawSlot(this.slot);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.disableLighting();
        if (isHovering()) {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            GlStateManager.colorMask(true, true, true, false);
            ModularGui.drawSolidRect(1, 1, 16, 16, Theme.INSTANCE.getSlotHighlight());
            GlStateManager.colorMask(true, true, true, true);
            GL11.glDisable(GL11.GL_BLEND);
        }
    }

    @Override
    public void onRebuild() {
        Pos2d pos =
                getAbsolutePos().subtract(getContext().getMainWindow().getPos()).add(1, 1);
        if (this.slot.xDisplayPosition != pos.x || this.slot.yDisplayPosition != pos.y) {
            this.slot.xDisplayPosition = pos.x;
            this.slot.yDisplayPosition = pos.y;
        }
    }

    @Override
    public void detectAndSendChanges(boolean init) {
        if (init || this.slot.isNeedsSyncing()) {
            getContext().syncSlotContent(this.slot);
            this.slot.resetNeedsSyncing();
        }
    }

    @Override
    public void buildTooltip(List<Text> tooltip) {
        if (isPhantom()) {
            tooltip.add(Text.localised("modularui.item.phantom.control"));
        }
    }

    @Override
    public List<String> getExtraTooltip() {
        List<String> extraLines = new ArrayList<>();
        if (slot.getStack().stackSize >= 1000) {
            extraLines.add(I18n.format("modularui.amount", slot.getStack().stackSize));
        }
        if (isPhantom()) {
            String[] lines = I18n.format("modularui.item.phantom.control").split("\\\\n");
            extraLines.addAll(Arrays.asList(lines));
        }
        return extraLines.isEmpty() ? Collections.emptyList() : extraLines;
    }

    public boolean isPhantom() {
        return this.slot.isPhantom();
    }

    @Override
    public Object getIngredient() {
        return slot.getStack();
    }

    @Override
    public SlotWidget setPos(Pos2d relativePos) {
        return (SlotWidget) super.setPos(relativePos);
    }

    @Override
    public SlotWidget setSize(Size size) {
        return (SlotWidget) super.setSize(size);
    }

    public SlotWidget setShiftClickPrio(int prio) {
        this.slot.setShiftClickPriority(prio);
        return this;
    }

    public SlotWidget disableShiftInsert() {
        this.slot.disableShiftInsert();
        return this;
    }

    public SlotWidget setChangeListener(Runnable runnable) {
        this.slot.setChangeListener(runnable);
        return this;
    }

    public SlotWidget setChangeListener(Consumer<SlotWidget> changeListener) {
        return setChangeListener(() -> changeListener.accept(this));
    }

    public SlotWidget setFilter(Predicate<ItemStack> filter) {
        this.slot.setFilter(filter);
        return this;
    }

    public SlotWidget setAccess(boolean canTake, boolean canInsert) {
        this.slot.setAccess(canInsert, canTake);
        return this;
    }

    public SlotWidget setIgnoreStackSizeLimit(boolean ignoreStackSizeLimit) {
        this.slot.setIgnoreStackSizeLimit(ignoreStackSizeLimit);
        return this;
    }

    public SlotWidget setSortable(String areaName) {
        if (this.sortAreaName == null ^ areaName == null) {
            this.sortAreaName = areaName;
        }
        return this;
    }

    @Override
    public SlotWidget setEnabled(boolean enabled) {
        if (enabled != isEnabled()) {
            super.setEnabled(enabled);
            slot.setEnabled(enabled);
            if (isClient()) {
                syncToServer(4, buffer -> buffer.writeBoolean(enabled));
            }
        }
        return this;
    }

    @Override
    public void readOnClient(int id, PacketBuffer buf) {}

    @Override
    public void readOnServer(int id, PacketBuffer buf) throws IOException {
        if (id == 1) {
            this.slot.xDisplayPosition = buf.readVarIntFromBuffer();
            this.slot.yDisplayPosition = buf.readVarIntFromBuffer();
        } else if (id == 2) {
            phantomClick(ClickData.readPacket(buf));
        } else if (id == 3) {
            phantomScroll(buf.readVarIntFromBuffer());
        } else if (id == 4) {
            setEnabled(buf.readBoolean());
        } else if (id == 5) {
            this.slot.putStack(buf.readItemStackFromBuffer());
        }
    }

    @Override
    public ClickResult onClick(int buttonId, boolean doubleClick) {
        if (isPhantom()) {
            syncToServer(2, buffer -> ClickData.create(buttonId, doubleClick).writeToPacket(buffer));
            return ClickResult.ACCEPT;
        }
        return ClickResult.REJECT;
    }

    @Override
    public boolean onMouseScroll(int direction) {
        if (isPhantom()) {
            if (Interactable.hasShiftDown()) {
                direction *= 8;
            }
            final int finalDirection = direction;
            syncToServer(3, buffer -> buffer.writeVarIntToBuffer(finalDirection));
            return true;
        }
        return false;
    }

    protected void phantomClick(ClickData clickData) {
        ItemStack cursorStack = getContext().getCursor().getItemStack();
        ItemStack slotStack = getMcSlot().getStack();
        ItemStack stackToPut;
        if (slotStack == null) {
            if (cursorStack == null) {
                if (clickData.mouseButton == 1 && this.lastStoredPhantomItem != null) {
                    stackToPut = this.lastStoredPhantomItem.copy();
                } else {
                    return;
                }
            } else {
                stackToPut = cursorStack.copy();
            }
            if (clickData.mouseButton == 1) {
                stackToPut.stackSize = 1;
            }
            slot.putStack(stackToPut);
            this.lastStoredPhantomItem = stackToPut.copy();
        } else {
            if (clickData.mouseButton == 0) {
                if (clickData.shift) {
                    this.slot.putStack(null);
                } else {
                    this.slot.incrementStackCount(-1);
                }
            } else if (clickData.mouseButton == 1) {
                this.slot.incrementStackCount(1);
            }
        }
    }

    protected void phantomScroll(int direction) {
        ItemStack currentItem = this.slot.getStack();
        if (direction > 0 && currentItem == null && lastStoredPhantomItem != null) {
            ItemStack stackToPut = this.lastStoredPhantomItem.copy();
            stackToPut.stackSize = direction;
            this.slot.putStack(stackToPut);
        } else {
            this.slot.incrementStackCount(direction);
        }
    }

    @Override
    public IGhostIngredientHandler.@Nullable Target getTarget(@NotNull ItemStack ingredient) {
        if (!isPhantom() || !(ingredient instanceof ItemStack) || ((ItemStack) ingredient) == null) {
            return null;
        }
        return new GhostIngredientWrapper<>(this);
    }

    @Override
    public void accept(@NotNull ItemStack ingredient) {
        syncToServer(5, buffer -> {
            try {
                buffer.writeItemStackToBuffer(ingredient);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private GuiContainerMixin getGuiAccessor() {
        return getContext().getScreen().getAccessor();
    }

    private ModularGui getScreen() {
        return getContext().getScreen();
    }

    /**
     * Copied from {@link net.minecraft.client.gui.inventory.GuiContainer} and removed the bad parts
     */
    @SideOnly(Side.CLIENT)
    private void drawSlot(Slot slotIn) {
        int x = slotIn.xDisplayPosition;
        int y = slotIn.yDisplayPosition;
        ItemStack itemstack = slotIn.getStack();
        boolean flag = false;
        boolean flag1 = slotIn == getGuiAccessor().getClickedSlot()
                && getGuiAccessor().getDraggedStack() != null
                && !getGuiAccessor().getIsRightMouseClick();
        ItemStack itemstack1 = getScreen().mc.thePlayer.inventory.getItemStack();
        int amount = -1;
        String format = null;

        if (slotIn == this.getGuiAccessor().getClickedSlot()
                && getGuiAccessor().getDraggedStack() != null
                && getGuiAccessor().getIsRightMouseClick()
                && itemstack != null) {
            itemstack = itemstack.copy();
            itemstack.stackSize = itemstack.stackSize / 2;
        } else if (getScreen().isDragSplitting2()
                && getScreen().getDragSlots().contains(slotIn)
                && itemstack1 != null) {
            if (getScreen().getDragSlots().size() == 1) {
                return;
            }

            // Container#canAddItemToSlot
            if (Container.func_94527_a(slotIn, itemstack1, true)
                    && getScreen().inventorySlots.canDragIntoSlot(slotIn)) {
                itemstack = itemstack1.copy();
                flag = true;
                // Container#computeStackSize
                Container.func_94525_a(
                        getScreen().getDragSlots(),
                        getGuiAccessor().getDragSplittingLimit(),
                        itemstack,
                        slotIn.getStack() == null ? 0 : slotIn.getStack().stackSize);
                int k = Math.min(itemstack.getMaxStackSize(), slotIn.getSlotStackLimit());

                if (itemstack.stackSize > k) {
                    amount = k;
                    format = EnumChatFormatting.YELLOW.toString();
                    itemstack.stackSize = k;
                }
            } else {
                getScreen().getDragSlots().remove(slotIn);
                getGuiAccessor().invokeUpdateDragSplitting();
            }
        }

        getScreen().setZ(100f);
        getScreen().getItemRenderer().zLevel = 100.0F;

        if (!flag1) {
            if (flag) {
                ModularGui.drawSolidRect(1, 1, 16, 16, -2130706433);
            }

            if (itemstack != null) {
                GlStateManager.enableRescaleNormal();
                GlStateManager.enableLighting();
                RenderHelper.enableGUIStandardItemLighting();
                GlStateManager.enableDepth();
                // render the item itself
                getScreen()
                        .getItemRenderer()
                        .renderItemAndEffectIntoGUI(
                                getScreen().getFontRenderer(),
                                Minecraft.getMinecraft().getTextureManager(),
                                itemstack,
                                1,
                                1);
                if (amount < 0) {
                    amount = itemstack.stackSize;
                }
                // render the amount overlay
                if (amount > 1 || format != null) {
                    String amountText = NumberFormat.format(amount, 2);
                    if (format != null) {
                        amountText = format + amountText;
                    }
                    float scale = 1f;
                    if (amountText.length() == 3) {
                        scale = 0.8f;
                    } else if (amountText.length() == 4) {
                        scale = 0.6f;
                    } else if (amountText.length() > 4) {
                        scale = 0.5f;
                    }
                    textRenderer.setShadow(true);
                    textRenderer.setScale(scale);
                    textRenderer.setColor(Color.WHITE.normal);
                    textRenderer.setAlignment(Alignment.BottomRight, size.width - 1, size.height - 1);
                    textRenderer.setPos(1, 1);
                    GlStateManager.disableLighting();
                    GlStateManager.disableDepth();
                    GlStateManager.disableBlend();
                    textRenderer.draw(amountText);
                    GlStateManager.enableLighting();
                    GlStateManager.enableDepth();
                    GlStateManager.enableBlend();
                }

                int cachedCount = itemstack.stackSize;
                itemstack.stackSize = 1; // required to not render the amount overlay
                // render other overlays like durability bar
                getScreen()
                        .getItemRenderer()
                        .renderItemOverlayIntoGUI(
                                getScreen().getFontRenderer(),
                                Minecraft.getMinecraft().getTextureManager(),
                                itemstack,
                                1,
                                1,
                                null);
                itemstack.stackSize = cachedCount;
                GlStateManager.disableDepth();
            }
        }

        getScreen().getItemRenderer().zLevel = 0.0F;
        getScreen().setZ(0f);
    }
}