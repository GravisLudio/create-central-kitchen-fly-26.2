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

package plus.dragons.createcentralkitchen.client;

import com.zurrtum.create.client.ponder.foundation.PonderIndex;
import java.util.Map;
import net.fabricmc.api.ClientModInitializer;
import plus.dragons.createcentralkitchen.client.ponder.CCKPonderPlugin;
import plus.dragons.createcentralkitchen.client.registry.CCKPartialModels;
import plus.dragons.createcentralkitchen.common.CCKCommon;
import plus.dragons.createcentralkitchen.config.CCKConfig;
import plus.dragons.createcentralkitchen.integration.ModIntegration;

public class CCKClient implements ClientModInitializer {
    /** Client entry class of each ported integration, started by name and only when its mod is loaded. */
    private static final Map<ModIntegration, String> INTEGRATIONS = Map.of(
            ModIntegration.FARMERSDELIGHT, "plus.dragons.createcentralkitchen.integration.farmersdelight.FDIntegrationClient");

    @Override
    public void onInitializeClient() {
        CCKConfig.registerClient();
        CCKPartialModels.register();
        INTEGRATIONS.forEach((integration, className) -> {
            if (integration.enabled())
                startIntegration(integration, className);
        });
        // After the integrations: they add their scenes and tags to the plugin's lists.
        PonderIndex.addPlugin(new CCKPonderPlugin());
    }

    private static void startIntegration(ModIntegration integration, String className) {
        try {
            Class.forName(className).getMethod("init").invoke(null);
        } catch (ClassNotFoundException exception) {
            CCKCommon.LOGGER.info("{} is loaded but this build has no client side for its integration", integration.id());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to start the client side of the " + integration.id() + " integration", exception);
        }
    }
}
