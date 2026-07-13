package net.stonenibbler.changed_survive_protocol.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.stonenibbler.changed_survive_protocol.common.config.CSPConfig;
import net.stonenibbler.changed_survive_protocol.common.item.CSPStrainItems;
import net.stonenibbler.changed_survive_protocol.common.lab.CSPMicroscopeInteractions;
import net.stonenibbler.changed_survive_protocol.common.menu.MicroscopeMenu;
import net.stonenibbler.changed_survive_protocol.common.registry.CSPBlockEntities;
import net.stonenibbler.changed_survive_protocol.common.registry.CSPItems;
import org.jetbrains.annotations.NotNull;

public class MicroscopeBlockEntity extends BaseContainerBlockEntity implements StackedContentsCompatible {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    private static final int CONTAINER_SIZE = 2;
    private static final int SAVE_INTERVAL = 20;

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private int progress;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? MicroscopeBlockEntity.this.progress : processTime();
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                MicroscopeBlockEntity.this.progress = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public MicroscopeBlockEntity(BlockPos pos, BlockState state) {
        super(CSPBlockEntities.MICROSCOPE.get(), pos, state);
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return Component.translatable("container.changed_survive_protocol.microscope");
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory) {
        return new MicroscopeMenu(id, inventory, this, dataAccess);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = ContainerHelper.removeItem(items, slot, amount);
        if (!stack.isEmpty()) {
            setChanged();
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack old = items.get(slot);
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        if (slot == SLOT_INPUT && !ItemStack.isSameItemSameTags(old, stack)) {
            progress = 0;
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_INPUT && CSPMicroscopeInteractions.isSupportedInput(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        items.clear();
        progress = 0;
        setChanged();
    }

    @Override
    public void fillStackedContents(StackedContents contents) {
        for (ItemStack stack : items) {
            contents.accountStack(stack);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        progress = tag.getInt("Progress");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putInt("Progress", progress);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MicroscopeBlockEntity blockEntity) {
        ItemStack input = blockEntity.items.get(SLOT_INPUT);
        ItemStack output = blockEntity.items.get(SLOT_OUTPUT);
        if (!canProcess(input, output)) {
            if (blockEntity.progress != 0) {
                blockEntity.progress = 0;
                setChanged(level, pos, state);
            }
            return;
        }

        blockEntity.progress++;
        if (blockEntity.progress >= processTime()) {
            String strainId = CSPMicroscopeInteractions.strainIdFrom(input);
            if (!strainId.isBlank()) {
                ItemStack result = CSPStrainItems.withStrain(new ItemStack(CSPItems.IDENTIFIED_LATEX_STRAND.get()), strainId);
                if (output.isEmpty()) {
                    blockEntity.items.set(SLOT_OUTPUT, result);
                } else {
                    output.grow(1);
                }
                input.shrink(1);
                if (input.isEmpty()) {
                    blockEntity.items.set(SLOT_INPUT, ItemStack.EMPTY);
                }
            }
            blockEntity.progress = 0;
            setChanged(level, pos, state);
        } else if (blockEntity.progress % SAVE_INTERVAL == 0) {
            setChanged(level, pos, state);
        }
    }

    private static int processTime() {
        return Math.max(1, CSPConfig.COMMON.microscopeProcessTimeTicks.get());
    }

    private static boolean canProcess(ItemStack input, ItemStack output) {
        if (input.isEmpty() || !CSPMicroscopeInteractions.isSupportedInput(input) || CSPMicroscopeInteractions.strainIdFrom(input).isBlank()) {
            return false;
        }
        if (output.isEmpty()) {
            return true;
        }
        if (!output.is(CSPItems.IDENTIFIED_LATEX_STRAND.get()) || output.getCount() >= output.getMaxStackSize()) {
            return false;
        }
        String inputStrain = CSPMicroscopeInteractions.strainIdFrom(input);
        return inputStrain.equals(CSPStrainItems.strainId(output));
    }
}
