package ic2.neoforge.recipe;

import ic2.neoforge.item.FluidCellItem;
import ic2.neoforge.registration.ModCells;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.transfer.access.ItemAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Legacy RecipeInputFluidContainer: matches any item stack whose fluid container capability can
 * supply at least the requested amount of the named fluid (a filled cell, or a filled bucket).
 * JSON shape mirrors legacy recipe input: {"fluid": "...", "amount": 1000}.
 */
public final class FluidItemIngredient implements ICustomIngredient {
    public static final MapCodec<FluidItemIngredient> MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance
                                    .group(
                                            BuiltInRegistries.FLUID.byNameCodec()
                                                    .fieldOf("fluid")
                                                    .forGetter(FluidItemIngredient::fluid),
                                            Codec.intRange(1, Integer.MAX_VALUE)
                                                    .optionalFieldOf("amount", 1000)
                                                    .forGetter(FluidItemIngredient::amount))
                                    .apply(instance, FluidItemIngredient::new));

    public static final IngredientType<FluidItemIngredient> TYPE =
            new IngredientType<>(MAP_CODEC);

    private final Ingredient vanilla = new Ingredient(this);
    private final Fluid fluid;
    private final int amount;

    public FluidItemIngredient(Fluid fluid, int amount) {
        this.fluid = fluid;
        this.amount = amount;
    }

    public Fluid fluid() {
        return fluid;
    }

    public int amount() {
        return amount;
    }

    @Override
    public boolean test(ItemStack stack) {
        var handler = ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM);
        if (handler == null) return false;
        for (int index = 0; index < handler.size(); index++) {
            if (handler.getResource(index).getFluid() == fluid
                    && handler.getAmountAsInt(index) >= amount) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Stream<Holder<Item>> items() {
        List<Holder<Item>> holders = new ArrayList<>();
        ModCells.CELLS.values().stream()
                .map(cell -> (Item) cell.get())
                .filter(cell -> cell instanceof FluidCellItem cellItem
                        && cellItem.fixedFluid() == fluid)
                .forEach(cell -> holders.add(BuiltInRegistries.ITEM.wrapAsHolder(cell)));
        // The empty cell is fillable with any fluid component, so it is always a candidate.
        holders.add(BuiltInRegistries.ITEM.wrapAsHolder(ModCells.EMPTY.get()));
        return holders.stream();
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return TYPE;
    }

    @Override
    public Ingredient toVanilla() {
        return vanilla;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FluidItemIngredient other
                && other.fluid == fluid
                && other.amount == amount;
    }

    @Override
    public int hashCode() {
        return 31 * fluid.hashCode() + amount;
    }
}
