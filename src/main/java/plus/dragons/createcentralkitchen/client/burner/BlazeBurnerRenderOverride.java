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

import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Changes how one blaze burner looks while something specific sits above it: it raises its head as if a basin were
 * there and puts a hat on. Only the hat and the raised head are supported on Create-Fly; the other overrides of the
 * NeoForge build (head, rods and flame models and particles, used by Dungeon's Delight) came with an integration
 * that is not ported yet.
 */
public interface BlazeBurnerRenderOverride {
    boolean isValid(Level level, BlockPos pos, BlazeBurnerBlockEntity burner);

    default boolean isValidBlockAbove(boolean original) {
        return original;
    }

    /**
     * @param small whether the head being drawn is the small, inert one
     */
    default @Nullable PartialModel getHatModel(boolean small) {
        return null;
    }

    static boolean isModelLoaded(@Nullable PartialModel model) {
        return model != null && model.get() != null;
    }
}
