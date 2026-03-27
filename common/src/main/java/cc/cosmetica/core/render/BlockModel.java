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

package cc.cosmetica.core.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Unbaked block model.
 */
public final class BlockModel {
    private BlockModel(List<Element> elements) {
        this.elements = elements;
    }

    private final List<Element> elements;

    public Iterable<Element> getElements() {
        return this.elements;
    }

    public static final class Element {
        private Element(Face north, Face east, Face south, Face west, Face up, Face down,
                       Vector3f from, Vector3f to, @Nullable String name) {
            this.north = north;
            this.east = east;
            this.south = south;
            this.west = west;
            this.up = up;
            this.down = down;
            this.from = from;
            this.to = to;
            this.name = name;
        }

        private final Face north;
        private final Face east;
        private final Face south;
        private final Face west;
        private final Face up;
        private final Face down;
        private final Vector3f from;
        private final Vector3f to;
        private final String name;
        private String group;

        public Face getFace(@NotNull Direction direction) {
            return switch (direction) {
                case DOWN -> this.down;
                case UP -> this.up;
                case NORTH -> this.north;
                case SOUTH -> this.south;
                case WEST -> this.west;
                case EAST -> this.east;
            };
        }

        public Vector3f from() {
            return this.from;
        }

        public Vector3f to() {
            return this.to;
        }

        public @Nullable String name() {
            return this.name;
        }

        public @Nullable String group() {
            return this.group;
        }
    }

    public static final class Face {
        private Face(Vector4f uv, Identifier texture) {
            this.uv = uv;
            this.texture = texture;
        }

        public final Vector4f uv;
        public final Identifier texture;
    }

    // Factories

    /**
     * Parse a block model.
     * @param json the json element to read from.
     * @return a block model.
     * @throws IllegalStateException invalid model.
     */
    public static BlockModel fromJson(JsonElement json) throws IllegalStateException
    {
        // Contents
        List<Element> elements = new ArrayList<>();

        // Textures
        JsonObject object = json.getAsJsonObject();
        JsonArray textureSize = object.get("texture_size").getAsJsonArray();
        if (textureSize.size() != 2) {
            throw new IllegalStateException("Invalid model: Texture size must have 2 elements");
        }

        int textureWidth = textureSize.get(0).getAsInt();
        int textureHeight = textureSize.get(1).getAsInt();

        Map<String, Identifier> textures = new HashMap<>();
        Identifier missingTexture = Identifier.withDefaultNamespace("missing");

        if (object.has("textures")) {
            JsonObject jTextures = object.getAsJsonObject("textures");
            for (Map.Entry<String, JsonElement> entry : jTextures.asMap().entrySet()) {
                textures.put("#" + entry.getKey(), Identifier.tryBySeparator(entry.getValue().getAsString(), ':'));
            }
        }

        // Object
        JsonArray jElements = object.getAsJsonArray("elements");

        for (JsonElement element : jElements) {
            elements.add(elementFromJson(element, textureWidth, textureHeight, id -> textures.getOrDefault(id, missingTexture)));
        }

        // Groups
        if (object.has("groups")) {
            JsonArray jGroups = object.getAsJsonArray("groups");

            for (JsonElement jGroup : jGroups) {
                attachGroups(elements, jGroup, "");
            }
        }

        return new BlockModel(elements);
    }

    private static void attachGroups(List<Element> elements, JsonElement jGroup, String parent) {
        JsonObject object = jGroup.getAsJsonObject();

        String name = object.get("name").getAsString();
        String translatedName = parent.isEmpty() ? name : (parent + "/" + name);

        if (object.has("children")) {
            for (JsonElement jChild : object.getAsJsonArray("children")) {
                if (jChild.isJsonObject()) {
                    // child group
                    attachGroups(elements, jChild, translatedName);
                } else {
                    // element
                    int index = jChild.getAsInt();
                    try {
                        elements.get(index).group = translatedName;
                    } catch (IndexOutOfBoundsException e) {
                        throw new IllegalStateException("Element index out of bounds for group " + translatedName, e);
                    }
                }
            }
        }
    }

    private static Element elementFromJson(JsonElement json, int textureWidth, int textureHeight, Function<String, Identifier> textures) {
        JsonObject jElement = json.getAsJsonObject();
        JsonObject jFaces = jElement.getAsJsonObject("faces");

        Face north = faceFromJson(jFaces.get("north"), textureWidth, textureHeight, textures);
        Face east = faceFromJson(jFaces.get("east"), textureWidth, textureHeight, textures);
        Face south = faceFromJson(jFaces.get("south"), textureWidth, textureHeight, textures);
        Face west = faceFromJson(jFaces.get("west"), textureWidth, textureHeight, textures);
        Face up = faceFromJson(jFaces.get("up"), textureWidth, textureHeight, textures);
        Face down = faceFromJson(jFaces.get("down"), textureWidth, textureHeight, textures);

        @Nullable String name = jElement.has("name") ? jElement.get("name").getAsString() : null;

        Vector3f from = vertexFromJson(jElement.getAsJsonArray("from"));
        Vector3f to = vertexFromJson(jElement.getAsJsonArray("to"));

        return new Element(north, east, south, west, up, down, from, to, name);
    }

    private static Vector3f vertexFromJson(JsonArray json) {
        if (json.size() != 3) {
            throw new IllegalStateException("Vertex must have 3 coordinates (x, y, z). Got: " + json);
        }

        return new Vector3f(json.get(0).getAsFloat(), json.get(1).getAsFloat(), json.get(2).getAsFloat());
    }

    private static Face faceFromJson(JsonElement json, int textureWidth, int textureHeight, Function<String, Identifier> textures) {
        JsonObject object = json.getAsJsonObject();
        JsonArray jUV = object.getAsJsonArray("uv");
        if (jUV.size() != 4) {
            throw new IllegalStateException("UV must be an array of length 4 (u0, v0, u1, v1). Got: " + jUV);
        }

        // TODO UV rotation
        Vector4f uv = new Vector4f(
                jUV.get(0).getAsInt() / (float)textureWidth,
                jUV.get(1).getAsInt() / (float)textureHeight,
                jUV.get(2).getAsInt() / (float)textureWidth,
                jUV.get(3).getAsInt() / (float)textureHeight
        );

        Identifier texture = textures.apply(object.get("texture").getAsString());

        return new Face(uv, texture);
    }
}
