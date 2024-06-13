/*
 * Copyright 2024 Cosmetica
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.cosmetica.core.builtin;

import gg.cloaks.javaclient.model.PlayerResponse;

/**
 * Duck interface to access and modify the {@link PlayerResponse} on a {@linkplain net.minecraft.client.player.AbstractClientPlayer player}.
 * @implNote I would rather not have to store this additional data alongside Cosmetis on every player, but my design
 * has forced my hand.
 * TODO perhaps I can condense this to just outfit information
 */
public interface CosmeticaPlayerHolder {
	PlayerResponse cosmeticacore$getResponse();
	void cosmeticacore$setResponse(PlayerResponse response);
}
