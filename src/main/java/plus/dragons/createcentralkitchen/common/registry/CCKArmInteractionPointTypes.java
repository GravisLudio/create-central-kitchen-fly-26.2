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

package plus.dragons.createcentralkitchen.common.registry;

import com.zurrtum.create.api.registry.CreateRegistries;
import com.zurrtum.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import net.minecraft.core.Registry;
import plus.dragons.createcentralkitchen.common.CCKCommon;

/**
 * Registers mechanical-arm interaction point types in Create-Fly's registry under this mod's namespace.
 * Create-Fly registers its own types by calling {@link Registry#register} directly, so no deferred register is needed.
 */
public final class CCKArmInteractionPointTypes {
    private CCKArmInteractionPointTypes() {}

    public static <T extends ArmInteractionPointType> T register(String name, T type) {
        return Registry.register(CreateRegistries.ARM_INTERACTION_POINT_TYPE, CCKCommon.asResource(name), type);
    }
}
