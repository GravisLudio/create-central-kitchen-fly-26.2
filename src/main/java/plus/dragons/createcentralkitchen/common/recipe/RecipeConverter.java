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

package plus.dragons.createcentralkitchen.common.recipe;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Converts one recipe into another and remembers the result until the next data-pack reload.
 * <p>
 * Taken from {@code plus.dragons.createdragonsplus.common.recipe.RecipeConverter} of Create: Dragons Plus
 * (DragonsPlus, LGPL-3.0-or-later), which this Fabric port used to need as a separate mod. Changes: it lives in this
 * mod, and the caches are emptied by Fabric's lifecycle events (after a data-pack reload, when a server stops)
 * instead of a Dragons Plus reload listener. {@link #register()} is called once from this mod's initializer;
 * integrations only call {@link #cached}.
 */
public interface RecipeConverter<K extends Recipe<?>, V extends Recipe<?>> extends Function<RecipeHolder<K>, RecipeHolder<V>> {
    /** Cache invalidators of every converter created through {@link #cached}, keyed by identity. */
    Map<RecipeConverter<?, ?>, Runnable> CACHE_INVALIDATORS = Collections.synchronizedMap(new IdentityHashMap<>());

    /** Empties every converter cache after a data-pack reload and when a server stops. Called once by this mod. */
    static void register() {
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> invalidateAll());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> invalidateAll());
    }

    /** Empties every converter cache now. */
    static void invalidateAll() {
        ArrayList<Runnable> invalidators;
        synchronized (CACHE_INVALIDATORS) {
            invalidators = new ArrayList<>(CACHE_INVALIDATORS.values());
        }
        invalidators.forEach(Runnable::run);
    }

    static <K extends Recipe<?>, V extends Recipe<?>> RecipeConverter<K, V> cached(CacheBuilder<Object, Object> cacheBuilder, RecipeConverter<K, V> converter) {
        var cache = cacheBuilder.build(new CacheLoader<RecipeHolder<K>, RecipeHolder<V>>() {
            @Override
            public RecipeHolder<V> load(RecipeHolder<K> key) {
                return converter.apply(key);
            }
        });
        RecipeConverter<K, V> result = cache::getUnchecked;
        CACHE_INVALIDATORS.put(result, cache::invalidateAll);
        return result;
    }
}
