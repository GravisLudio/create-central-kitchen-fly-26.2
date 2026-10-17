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

package plus.dragons.createcentralkitchen.integration.farmersdelight.burner;

import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import plus.dragons.createcentralkitchen.client.burner.BlazeBurnerRenderOverrides;
import plus.dragons.createcentralkitchen.client.registry.CCKPartialModels;

/** The chef hats worn by blaze burners under Farmer's Delight cooking blocks. */
public class BlazeChefRenderOverrides {
    public static final int SPECIAL_PRIORITY = 0;
    public static final int DEFAULT_PRIORITY = -1000;
    private static boolean defaultsRegistered = false;

    public static synchronized void registerDefaults() {
        if (defaultsRegistered)
            return;
        defaultsRegistered = true;
        register(DEFAULT_PRIORITY, null, CCKPartialModels.CHEF_HAT, CCKPartialModels.CHEF_HAT_SMALL);
    }

    public static void register(Identifier targetBlock, PartialModel hat, PartialModel smallHat) {
        register(SPECIAL_PRIORITY, targetBlock, hat, smallHat);
    }

    public static void register(int priority, @Nullable Identifier targetBlock, PartialModel hat, PartialModel smallHat) {
        BlazeBurnerRenderOverrides.register(priority, new BlazeChefRenderOverride(targetBlock, hat, smallHat));
    }
}
