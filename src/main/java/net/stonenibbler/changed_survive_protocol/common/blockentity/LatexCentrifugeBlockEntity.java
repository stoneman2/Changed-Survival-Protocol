package net.stonenibbler.changed_survive_protocol.common.blockentity;

import net.ltxprogrammer.changed.init.ChangedItems;
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
import net.stonenibbler.changed_survive_protocol.common.item.CSPStrainItems;
import net.stonenibbler.changed_survive_protocol.common.menu.LatexCentrifugeMenu;
import net.stonenibbler.changed_survive_protocol.common.registry.CSPBlockEntities;
import net.stonenibbler.changed_survive_protocol.common.registry.CSPItems;
import org.jetbrains.annotations.NotNull;

public class LatexCentrifugeBlockEntity extends BaseContainerBlockEntity implements StackedContentsCompatible {
    public static final int SLOT_STRAND = 0;
    public static final int SLOT_ADDITIVE = 1;
    public static final int SLOT_DISINFECTANT = 2;
    public static final int SLOT_LATEX_FEED = 3;
    public static final int SLOT_OUTPUT = 4;
    private static final int CONTAINER_SIZE = 5;
    private static final int PROCESS_TIME = 240;

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private int progress;
    private int recipeMode;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> LatexCentrifugeBlockEntity.this.progress;
                case 1 -> PROCESS_TIME;
                case 2 -> LatexCentrifugeBlockEntity.this.recipeMode;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                LatexCentrifugeBlockEntity.this.progress = value;
            } else if (index == 2) {
                LatexCentrifugeBlockEntity.this.recipeMode = value;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public LatexCentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(CSPBlockEntities.LATEX_CENTRIFUGE.get(), pos, state);
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return Component.translatable("container.changed_survive_protocol.latex_centrifuge");
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory) {
        return new LatexCentrifugeMenu(id, inventory, this, dataAccess);
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
        if (slot != SLOT_OUTPUT && !ItemStack.isSameItemSameTags(old, stack)) {
            progress = 0;
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_STRAND -> stack.is(CSPItems.IDENTIFIED_LATEX_STRAND.get());
            case SLOT_ADDITIVE -> stack.is(CSPItems.LATEX_INHIBITOR.get()) || stack.is(CSPItems.STABILIZING_REAGENT.get());
            case SLOT_DISINFECTANT -> stack.is(CSPItems.DISINFECTANT_SPRAY.get());
            case SLOT_LATEX_FEED -> isLatexFeed(stack);
            default -> false;
        };
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
        recipeMode = tag.getInt("RecipeMode");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putInt("Progress", progress);
        tag.putInt("RecipeMode", recipeMode);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LatexCentrifugeBlockEntity blockEntity) {
        Recipe recipe = blockEntity.currentRecipe();
        blockEntity.recipeMode = recipe == null ? 0 : recipe.mode();
        if (recipe == null) {
            if (blockEntity.progress != 0) {
                blockEntity.progress = 0;
                setChanged(level, pos, state);
            }
            return;
        }

        blockEntity.progress++;
        if (blockEntity.progress >= PROCESS_TIME) {
            blockEntity.items.get(SLOT_STRAND).shrink(1);
            blockEntity.items.get(SLOT_ADDITIVE).shrink(1);
            if (recipe.mode() == 1) {
                blockEntity.items.get(SLOT_DISINFECTANT).shrink(1);
            }
            blockEntity.items.get(SLOT_LATEX_FEED).shrink(1);
            if (blockEntity.items.get(SLOT_OUTPUT).isEmpty()) {
                blockEntity.items.set(SLOT_OUTPUT, recipe.output());
            } else {
                blockEntity.items.get(SLOT_OUTPUT).grow(recipe.output().getCount());
            }
            blockEntity.compactEmptyInputs();
            blockEntity.progress = 0;
        }
        setChanged(level, pos, state);
    }

    private Recipe currentRecipe() {
        ItemStack strand = items.get(SLOT_STRAND);
        ItemStack additive = items.get(SLOT_ADDITIVE);
        ItemStack disinfectant = items.get(SLOT_DISINFECTANT);
        ItemStack feed = items.get(SLOT_LATEX_FEED);
        ItemStack output = items.get(SLOT_OUTPUT);
        String strainId = CSPStrainItems.strainId(strand);
        if (strand.isEmpty() || !strand.is(CSPItems.IDENTIFIED_LATEX_STRAND.get()) || strainId.isBlank() || feed.isEmpty() || !isLatexFeed(feed)) {
            return null;
        }
        if (additive.is(CSPItems.LATEX_INHIBITOR.get())) {
            if (!disinfectant.is(CSPItems.DISINFECTANT_SPRAY.get())) {
                return null;
            }
            ItemStack result = CSPStrainItems.withStrain(new ItemStack(CSPItems.STRAND_CURE_DOSE.get(), 3), strainId);
            return canAcceptOutput(output, result) ? new Recipe(1, result) : null;
        }
        if (additive.is(CSPItems.STABILIZING_REAGENT.get())) {
            if (!disinfectant.isEmpty()) {
                return null;
            }
            ItemStack result = CSPStrainItems.withAttunement(CSPStrainItems.withStrain(new ItemStack(CSPItems.CULTURED_LATEX_STRAND.get()), strainId), 0.0D);
            return canAcceptOutput(output, result) ? new Recipe(2, result) : null;
        }
        return null;
    }

    private void compactEmptyInputs() {
        for (int slot = SLOT_STRAND; slot <= SLOT_LATEX_FEED; slot++) {
            if (items.get(slot).isEmpty()) {
                items.set(slot, ItemStack.EMPTY);
            }
        }
    }

    public static boolean isLatexFeed(ItemStack stack) {
        return stack.is(ChangedItems.LATEX_BASE.get()) || stack.is(ChangedItems.DARK_LATEX_GOO.get()) || stack.is(ChangedItems.WHITE_LATEX_GOO.get());
    }

    private static boolean canAcceptOutput(ItemStack output, ItemStack result) {
        if (output.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameTags(output, result)) {
            return false;
        }
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private record Recipe(int mode, ItemStack output) {
    }
}
