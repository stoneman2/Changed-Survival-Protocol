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
import net.stonenibbler.changed_survive_protocol.common.blockentity.MicroscopeBlockEntity;
import net.stonenibbler.changed_survive_protocol.common.lab.CSPMicroscopeInteractions;
import net.stonenibbler.changed_survive_protocol.common.registry.CSPMenus;

public class MicroscopeMenu extends AbstractContainerMenu {
    public final Container container;
    public final ContainerData data;

    public MicroscopeMenu(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public MicroscopeMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, new SimpleContainer(2), new SimpleContainerData(2));
    }

    public MicroscopeMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(CSPMenus.MICROSCOPE.get(), id);
        checkContainerSize(container, 2);
        checkContainerDataCount(data, 2);
        this.container = container;
        this.data = data;

        this.addSlot(new Slot(container, MicroscopeBlockEntity.SLOT_INPUT, 40, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return CSPMicroscopeInteractions.isSupportedInput(stack);
            }
        });
        this.addSlot(new OutputSlot(container, MicroscopeBlockEntity.SLOT_OUTPUT, 120, 35));
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
            if (index < 2) {
                if (!moveItemStackTo(stack, 2, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, copy);
            } else if (CSPMicroscopeInteractions.isSupportedInput(stack)) {
                if (!moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 29) {
                if (!moveItemStackTo(stack, 29, slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 2, 29, false)) {
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
