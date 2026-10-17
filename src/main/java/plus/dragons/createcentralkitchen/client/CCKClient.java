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

import net.fabricmc.api.ClientModInitializer;
import plus.dragons.createcentralkitchen.config.CCKConfig;

/**
 * Client entrypoint. For now it only loads the client config: the blaze burner heads, Ponder scenes and JEI plugin
 * of the NeoForge build are still to be ported.
 */
public class CCKClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CCKConfig.registerClient();
    }
}
