package ic2.neoforge.uu;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Datapack loader for UU seed sets: every {@code uu_values/*.json} file is an array of
 * {@code {"item": id, "value": n}} entries. Merged across packs; a non-empty set replaces the
 * built-in defaults.
 */
public final class UuValueReloadListener
        extends net.minecraft.server.packs.resources.SimplePreparableReloadListener<
                Map<Identifier, JsonElement>> {
    @Override
    protected Map<Identifier, JsonElement> prepare(
            ResourceManager manager, ProfilerFiller profiler) {
        var jsons = new HashMap<Identifier, JsonElement>();
        for (var entry : manager
                .listResources("uu_values", path -> path.getPath().endsWith(".json"))
                .entrySet()) {
            try (var reader = entry.getValue().openAsReader()) {
                jsons.put(entry.getKey(), JsonParser.parseReader(reader));
            } catch (Exception error) {
                // malformed seed files are skipped; built-in seeds remain
            }
        }
        return jsons;
    }

    @Override
    protected void apply(
            Map<Identifier, JsonElement> jsons, ResourceManager manager,
            ProfilerFiller profiler) {
        var seeds = new ArrayList<UuSeed>();
        for (var entry : jsons.entrySet()) {
            if (!entry.getValue().isJsonArray()) continue;
            for (var element : entry.getValue().getAsJsonArray()) {
                var obj = element.getAsJsonObject();
                if (obj.has("item") && obj.has("value")) {
                    seeds.add(new UuSeed(
                            obj.get("item").getAsString(),
                            obj.get("value").getAsInt()));
                }
            }
        }
        UuValues.applySeeds(seeds);
    }
}
