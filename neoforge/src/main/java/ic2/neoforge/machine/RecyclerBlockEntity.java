package ic2.neoforge.machine;

import ic2.core.machine.MachineProcess;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.List;

/** Potential output reserves space; the one-in-eight roll happens only on actual completion. */
public final class RecyclerBlockEntity extends ProcessingBlockEntity {
    public static final TagKey<Item> BLACKLIST =
            TagKey.create(Registries.ITEM, Identifier.parse("ic2:recycler_blacklist"));
    public static final TagKey<Item> WHITELIST =
            TagKey.create(Registries.ITEM, Identifier.parse("ic2:recycler_whitelist"));
    private ItemResource previousInput = ItemResource.EMPTY;

    public RecyclerBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(MachineKind.RECYCLER), pos, state);
    }

    @Override
    protected boolean acceptsInput(ItemResource resource, ServerLevel level) {
        return !resource.isEmpty();
    }

    public static boolean producesScrap(ItemResource resource) {
        boolean whitelistActive =
                BuiltInRegistries.ITEM.get(WHITELIST).map(tag -> tag.size() > 0).orElse(false);
        return whitelistActive ? resource.is(WHITELIST) : !resource.is(BLACKLIST);
    }

    @Override
    protected Job findJob(ServerLevel level) {
        var input = inventory.getResource(INPUT);
        if (!input.equals(previousInput)) {
            process.restore(new MachineProcess.State("", 0));
            previousInput = input;
            setChanged();
        }
        if (input.isEmpty()) return null;
        var outputs =
                producesScrap(input)
                        ? List.of(
                                new ItemStackTemplate(
                                        ModItems.MATERIALS.get(MaterialDefinition.SCRAP).get(), 1))
                        : List.<ItemStackTemplate>of();
        return new Job("ic2:recycling", 1, outputs, 0);
    }

    @Override
    protected List<ItemStackTemplate> outputsOnCompletion(Job job, ServerLevel level) {
        return level.getRandom().nextInt(8) == 0 ? job.outputs() : List.of();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        previousInput =
                input.read("recyclingInput", ItemResource.OPTIONAL_CODEC)
                        .orElse(ItemResource.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("recyclingInput", ItemResource.OPTIONAL_CODEC, previousInput);
    }
}
