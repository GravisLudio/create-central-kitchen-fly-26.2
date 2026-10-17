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

package plus.dragons.createcentralkitchen.integration.farmersdelight;

import com.zurrtum.create.api.packager.unpacking.UnpackingHandler;
import plus.dragons.createcentralkitchen.integration.farmersdelight.packager.CookingPotUnpackingHandler;
import plus.dragons.createcentralkitchen.integration.farmersdelight.recipe.CuttingBoardRecipeConverters;
import plus.dragons.createcentralkitchen.integration.farmersdelight.registry.FDArmInteractionPointTypes;
import vectorwing.farmersdelight.common.registry.ModBlockEntityTypes;

/**
 * Entry point of the Farmer's Delight integration. {@code CCKCommon} calls {@link #init()} only when Farmer's
 * Delight is loaded, so nothing here is touched without it.
 */
public final class FDIntegration {
    private FDIntegration() {}

    public static void init() {
        FDArmInteractionPointTypes.register();
        CuttingBoardRecipeConverters.register();
        // A provider is asked lazily, after every mod has registered its blocks, so the order of mod
        // initialization does not matter.
        var cookingPot = new CookingPotUnpackingHandler();
        UnpackingHandler.REGISTRY.registerProvider(block ->
                ModBlockEntityTypes.COOKING_POT.get().isValid(block.defaultBlockState()) ? cookingPot : null);
    }
}
