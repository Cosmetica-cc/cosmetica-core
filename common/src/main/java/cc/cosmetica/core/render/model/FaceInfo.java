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

package cc.cosmetica.core.render.model;

import com.mojang.math.Vector3f;

/**
 * Better than 1.16.5's faceinfo.
 */
public enum FaceInfo {
    DOWN(
            new Vertex(Extent.MIN_X, Extent.MIN_Y, Extent.MAX_Z),
            new Vertex(Extent.MIN_X, Extent.MIN_Y, Extent.MIN_Z),
            new Vertex(Extent.MAX_X, Extent.MIN_Y, Extent.MIN_Z),
            new Vertex(Extent.MAX_X, Extent.MIN_Y, Extent.MAX_Z)
    ),
    UP(
            new Vertex(Extent.MIN_X, Extent.MAX_Y, Extent.MIN_Z),
            new Vertex(Extent.MIN_X, Extent.MAX_Y, Extent.MAX_Z),
            new Vertex(Extent.MAX_X, Extent.MAX_Y, Extent.MAX_Z),
            new Vertex(Extent.MAX_X, Extent.MAX_Y, Extent.MIN_Z)
    ),
    NORTH(
            new Vertex(Extent.MAX_X, Extent.MAX_Y, Extent.MIN_Z),
            new Vertex(Extent.MAX_X, Extent.MIN_Y, Extent.MIN_Z),
            new Vertex(Extent.MIN_X, Extent.MIN_Y, Extent.MIN_Z),
            new Vertex(Extent.MIN_X, Extent.MAX_Y, Extent.MIN_Z)
    ),
    SOUTH(
            new Vertex(Extent.MIN_X, Extent.MAX_Y, Extent.MAX_Z),
            new Vertex(Extent.MIN_X, Extent.MIN_Y, Extent.MAX_Z),
            new Vertex(Extent.MAX_X, Extent.MIN_Y, Extent.MAX_Z),
            new Vertex(Extent.MAX_X, Extent.MAX_Y, Extent.MAX_Z)
    ),
    WEST(
            new Vertex(Extent.MIN_X, Extent.MAX_Y, Extent.MIN_Z),
            new Vertex(Extent.MIN_X, Extent.MIN_Y, Extent.MIN_Z),
            new Vertex(Extent.MIN_X, Extent.MIN_Y, Extent.MAX_Z),
            new Vertex(Extent.MIN_X, Extent.MAX_Y, Extent.MAX_Z)
    ),
    EAST(
            new Vertex(Extent.MAX_X, Extent.MAX_Y, Extent.MAX_Z),
            new Vertex(Extent.MAX_X, Extent.MIN_Y, Extent.MAX_Z),
            new Vertex(Extent.MAX_X, Extent.MIN_Y, Extent.MIN_Z),
            new Vertex(Extent.MAX_X, Extent.MAX_Y, Extent.MIN_Z)
    );

    FaceInfo(Vertex ...vertices) {
        this.vertexOrder = vertices;
    }

    public final Vertex[] vertexOrder;

    public enum Extent {
        MIN_X, MAX_X,
        MIN_Y, MAX_Y,
        MIN_Z, MAX_Z;

        public float select(final Vector3f min, final Vector3f max) {
            switch (this) {
                case MIN_X: return min.x();
                case MIN_Y: return min.y();
                case MIN_Z: return min.z();
                case MAX_X: return max.x();
                case MAX_Y: return max.y();
                case MAX_Z: return max.z();
                default: return 0;
            }
        }
    }

    public static final class Vertex {
        public Vertex(Extent x, Extent y, Extent z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        Extent x, y, z;

        public Extent x() {
            return this.x;
        }

        public Extent y() {
            return this.y;
        }

        public Extent z() {
            return this.z;
        }

        public Vector3f select(final Vector3f min, final Vector3f max) {
            return new Vector3f(this.x.select(min, max), this.y.select(min, max), this.z.select(min, max));
        }
    }

    static FaceInfo fromVanilla(net.minecraft.client.renderer.FaceInfo faceInfo) {
        switch (faceInfo) {
        case DOWN: return DOWN;
        case UP:   return UP;
        case NORTH: return NORTH;
        case SOUTH: return SOUTH;
        case WEST: return WEST;
        case EAST: return EAST;
        default: return null;
        }
    }
}
