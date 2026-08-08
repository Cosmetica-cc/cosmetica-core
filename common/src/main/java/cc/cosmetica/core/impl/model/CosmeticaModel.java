package cc.cosmetica.core.impl.model;

import com.google.gson.JsonObject;
import com.mojang.math.Vector3f;

import java.util.List;

public class CosmeticaModel {
    private CosmeticaModel(List<Cube> cubes, int textureWidth, int textureHeight) {
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.cubes = cubes;
    }

    private final int textureWidth;
    private final int textureHeight;
    private final List<Cube> cubes;

    private static class Cube {

    }

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

    public static CosmeticaModel parse(JsonObject json) {
        return new CosmeticaModel();
    }
}
