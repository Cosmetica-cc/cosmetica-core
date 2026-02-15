/*
 * Copyright 2024, 2025 Cosmetica
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

package cc.cosmetica.core.impl;

import cc.cosmetica.core.api.CosmeticManager;
import cc.cosmetica.core.api.Cosmetics;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Manages Cosmetic managers for a cosmetic equipper.
 * (Implements Equip behaviour common to all equippers).
 */
public class CosmeticEquipHelper {
    public CosmeticEquipHelper(Function<CosmeticManager, Cosmetics> cosmeticGetter) {
        this.cosmeticGetter = cosmeticGetter;
    }

    private final Function<CosmeticManager, Cosmetics> cosmeticGetter;

    // allows us to use older cosmetics while newer ones are still loading.
    // this should not get very big.
    private final Queue<Cosmetics> cosmetics = new ArrayDeque<>();
    private final IdentityCache<CosmeticManager> manager = new IdentityCache<>();

    /**
     * Return the current cosmetics.
     * @return the current cosmetics equipped.
     */
    public Optional<Cosmetics> getCosmetics() {
        return Optional.ofNullable(this.cosmetics.peek());
    }

    /**
     * Refresh cosmetics. If the manager has changed, add the new cosmetics to the queue.
     * @param manager the new manager.
     */
    public void refreshCosmetics(CosmeticManager manager, UUID uuid, Consumer<@Nullable Cosmetics> onUpdate) {
        if (this.manager.getValue() == manager) {
            // test if cosmetics are different from the most recently added (other end of the queue)
            Cosmetics next = this.cosmeticGetter.apply(manager);

            if (next != ((Deque<Cosmetics>)this.cosmetics).peekLast()) {
                Logging.getInstance().debug(LoggingCategory.COSMETICS, "New cosmetics detected. Refreshing for {}", uuid);
                // load new cosmetics
                updateCosmetics(manager, onUpdate);
            }
        }
    }

    public void updateCosmetics(CosmeticManager manager, Consumer<@Nullable Cosmetics> onUpdate) {
        if (this.manager.getValue() == manager) {
            if (manager == null) {
                synchronized (this.cosmetics) {
                    this.cosmetics.clear();
                }

                // forward to listeners
                onUpdate.accept(null);
            } else {
                // push a new cosmetics
                Cosmetics next = this.cosmeticGetter.apply(manager);
                Logging.getInstance().debug(LoggingCategory.COSMETICS, "Next cosmetics " + next);

                synchronized (this.cosmetics) {
                    this.cosmetics.add(next);
                }

                next.enqueue(() -> {
                    boolean updated = false;

                    synchronized (this.cosmetics) {
                        // fast-forward to front
                        if (this.cosmetics.contains(next)) {
                            updated = true;

                            while (this.cosmetics.peek() != next)
                                this.cosmetics.remove();
                        }

                        Logging.getInstance().debug(LoggingCategory.COSMETICS, "Loaded Cosmetics {}", this.cosmetics);
                    }

                    if (updated) {
                        // forward to listeners
                        onUpdate.accept(next);
                    }
                }, () -> {
                    synchronized (this.cosmetics) {
                        this.cosmetics.remove(next);
                    }
                });
            }
        }
    }

    public IdentityCache<CosmeticManager> getManager() {
        return this.manager;
    }
}
