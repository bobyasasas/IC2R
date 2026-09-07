package ic2.neoforge.client;

import ic2.neoforge.registration.ModFluids;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;

final class FluidModels {
    static void register(RegisterFluidModelsEvent event) {
        ModFluids.FAMILIES.forEach(
                (definition, family) ->
                        event.register(
                                new FluidModel.Unbaked(
                                        new Material(
                                                Identifier.fromNamespaceAndPath(
                                                        "ic2", definition.stillSprite()),
                                                true),
                                        new Material(
                                                Identifier.fromNamespaceAndPath(
                                                        "ic2", definition.flowingSprite()),
                                                true),
                                        null,
                                        FluidTintSources.constant(definition.color())),
                                family.source(),
                                family.flowing()));
    }

    private FluidModels() {}
}
