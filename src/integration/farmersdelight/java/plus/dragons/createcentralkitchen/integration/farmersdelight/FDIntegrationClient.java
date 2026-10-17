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

import plus.dragons.createcentralkitchen.integration.farmersdelight.burner.BlazeChefRenderOverrides;
import plus.dragons.createcentralkitchen.integration.farmersdelight.jei.FDJeiExtension;
import plus.dragons.createcentralkitchen.integration.farmersdelight.ponder.FDPonderPlugin;
import plus.dragons.createcentralkitchen.integration.jei.CCKJeiPlugin;

/** Client side of the Farmer's Delight integration. Started by {@code CCKClient} only when Farmer's Delight is loaded. */
public final class FDIntegrationClient {
    private FDIntegrationClient() {}

    public static void init() {
        BlazeChefRenderOverrides.registerDefaults();
        FDPonderPlugin.register();
        CCKJeiPlugin.register(new FDJeiExtension());
    }
}
