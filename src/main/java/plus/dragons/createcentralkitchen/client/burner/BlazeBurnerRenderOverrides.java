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

package plus.dragons.createcentralkitchen.client.burner;

import com.zurrtum.create.content.processing.burner.BlazeBurnerBlockEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.level.Level;

/** Overrides in priority order (higher first); the first valid one wins. */
public final class BlazeBurnerRenderOverrides {
    private static final List<Entry> OVERRIDES = new ArrayList<>();
    private static volatile List<BlazeBurnerRenderOverride> sorted = List.of();

    private BlazeBurnerRenderOverrides() {}

    public static synchronized void register(int priority, BlazeBurnerRenderOverride override) {
        OVERRIDES.add(new Entry(priority, override));
        sorted = OVERRIDES.stream()
                .sorted(Comparator.comparingInt(Entry::priority).reversed())
                .map(Entry::override)
                .toList();
    }

    /** Chooses the override of a burner for this tick. Called from the burner's client animation tick. */
    public static void update(BlazeBurnerBlockEntity burner) {
        Level level = burner.getLevel();
        if (level == null)
            return;
        BlazeBurnerRenderOverride chosen = null;
        for (var override : sorted) {
            if (override.isValid(level, burner.getBlockPos(), burner)) {
                chosen = override;
                break;
            }
        }
        ((BlazeBurnerClientExtension) burner).setRenderOverride(chosen);
    }

    private record Entry(int priority, BlazeBurnerRenderOverride override) {}
}
