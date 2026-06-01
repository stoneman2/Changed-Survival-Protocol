package net.stonenibbler.changed_survive_protocol.common.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.stonenibbler.changed_survive_protocol.common.blockentity.LatexCentrifugeBlockEntity;
import net.stonenibbler.changed_survive_protocol.common.registry.CSPItems;
import net.stonenibbler.changed_survive_protocol.common.registry.CSPMenus;

public class LatexCentrifugeMenu extends AbstractContainerMenu {
    public final Container container;
    public final ContainerData data;

    public LatexCentrifugeMenu(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public LatexCentrifugeMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, new SimpleContainer(5), new SimpleContainerData(3));
    }

    public LatexCentrifugeMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(CSPMenus.LATEX_CENTRIFUGE.get(), id);
        checkContainerSize(container, 5);
        checkContainerDataCount(data, 3);
        this.container = container;
        this.data = data;

        this.addSlot(new Slot(container, LatexCentrifugeBlockEntity.SLOT_STRAND, 35, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(CSPItems.IDENTIFIED_LATEX_STRAND.get());
            }
        });
        this.addSlot(new Slot(container, LatexCentrifugeBlockEntity.SLOT_ADDITIVE, 62, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(CSPItems.LATEX_INHIBITOR.get()) || stack.is(CSPItems.STABILIZING_REAGENT.get());
            }
        });
        this.addSlot(new Slot(container, LatexCentrifugeBlockEntity.SLOT_DISINFECTANT, 89, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(CSPItems.DISINFECTANT_SPRAY.get());
            }
        });
        this.addSlot(new Slot(container, LatexCentrifugeBlockEntity.SLOT_LATEX_FEED, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return LatexCentrifugeBlockEntity.isLatexFeed(stack);
            }
        });
        this.addSlot(new OutputSlot(container, LatexCentrifugeBlockEntity.SLOT_OUTPUT, 152, 35));
        addPlayerInventory(inventory);
        addDataSlots(data);
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();
            if (index < 5) {
                if (!moveItemStackTo(stack, 5, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, copy);
            } else if (stack.is(CSPItems.IDENTIFIED_LATEX_STRAND.get())) {
                if (!moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(CSPItems.LATEX_INHIBITOR.get()) || stack.is(CSPItems.STABILIZING_REAGENT.get())) {
                if (!moveItemStackTo(stack, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(CSPItems.DISINFECTANT_SPRAY.get())) {
                if (!moveItemStackTo(stack, 2, 3, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (LatexCentrifugeBlockEntity.isLatexFeed(stack)) {
                if (!moveItemStackTo(stack, 3, 4, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 32) {
                if (!moveItemStackTo(stack, 32, slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 5, 32, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == copy.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return copy;
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + (row + 1) * 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }
}
