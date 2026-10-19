/*
 * Copyright (C) 2025  DragonsPlus
 * SPDX-License-Identifier: LGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package plus.dragons.createcentralkitchen.common.packager;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

public final class UnpackingRecipeCache {
    private static final Cache<RecipeManager, Map<RecipeType<?>, List<RecipeUnpacking.Candidate>>> RECIPES = CacheBuilder.newBuilder().weakKeys().build();

    private UnpackingRecipeCache() {}

    /**
     * Drops the cache whenever recipes or tags change: after a data-pack reload (Fabric fires the event once the new
     * recipes and tags are in place) and when a server stops.
     */
    public static void register() {
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> RECIPES.invalidateAll());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> RECIPES.invalidateAll());
    }

    static List<RecipeUnpacking.Candidate> get(RecipeManager manager, RecipeType<?> type) {
        return RECIPES.asMap().computeIfAbsent(manager, ignored -> new ConcurrentHashMap<>())
                .computeIfAbsent(type, key -> load(manager, key));
    }

    private static List<RecipeUnpacking.Candidate> load(RecipeManager manager, RecipeType<?> type) {
        return manager.getRecipes().stream().map(RecipeHolder::value)
                .filter(recipe -> recipe.getType() == type)
                .map(RecipeUnpacking.Candidate::new)
                .filter(candidate -> !candidate.ingredients().isEmpty() && candidate.ingredients().size() <= 9).toList();
    }
}
