package cc.cosmetica.core.impl.model;

import com.mojang.math.Vector3f;

public class CosmeticaModel {

    public static class Rotation {
        public Rotation(Vector3f origin, float x, float y, float z) {
            this.origin = origin;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public final Vector3f origin;
        public final float x;
        public final float y;
        public final float z;
    }
}
