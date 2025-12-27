package cc.cosmetica.core.util;

import cc.cosmetica.core.impl.Logging;
import net.minecraft.world.phys.Vec2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

/**
 * 99% pure AI.
 */
public class VP8X {
    public static final int MARK_LIMIT = 30;

    /**
     * Get Webp canvas dimensions from an input stream.
     * @param stream the input stream from which to read. To rewind the stream, mark with the limit in {@link VP8X#MARK_LIMIT}.
     * @return an empty optional if any error occurs (such as providing a non-animated webp, or a different format).
     * @throws IOException if an IOException occurs while reading data.
     */
    public static Optional<int[]> getWebpDimensions(InputStream stream) throws IOException {
        byte[] header = new byte[MARK_LIMIT];
        if (stream.read(header) != MARK_LIMIT) {
            Logging.getInstance().warn("Image data too short");
            return Optional.empty();
        }

        ByteBuffer buffer = ByteBuffer.wrap(header);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // 1. Check RIFF header
        // Bytes 0-3 must be "RIFF"
        // Bytes 8-11 must be "WEBP"
        if (!isMatch(header, 0, "RIFF") || !isMatch(header, 8, "WEBP")) {
            // Not a webp
            return Optional.empty();
        }

        // 2. Check for VP8X chunk
        // Animated WebPs MUST have the 'VP8X' chunk first (Bytes 12-15)
        if (!isMatch(header, 12, "VP8X")) {
            // Simple static WebPs might be VP8 (lossy) or VP8L (lossless)
            // and store dimensions differently.
            return Optional.empty();
        }

        // 3. Read Canvas Dimensions
        // Width is at offset 24 (3 bytes)
        // Height is at offset 27 (3 bytes)
        // Values are 1-based (stored value is width - 1)

        int widthMinusOne = get24BitInt(header, 24);
        int heightMinusOne = get24BitInt(header, 27);

        int width = widthMinusOne + 1;
        int height = heightMinusOne + 1;
        return Optional.of(new int[]{width, height});
    }

    private static boolean isMatch(byte[] data, int offset, String match) {
        for (int i = 0; i < match.length(); i++) {
            if (data[offset + i] != match.charAt(i)) return false;
        }
        return true;
    }

    // Helper to read 3-byte integer (Little Endian)
    private static int get24BitInt(byte[] data, int offset) {
        return (data[offset] & 0xFF) |
                ((data[offset + 1] & 0xFF) << 8) |
                ((data[offset + 2] & 0xFF) << 16);
    }
}
