package org.teacon.slides.texture;

import com.luciad.imageio.webp.WebPReadParam;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import net.minecraft.client.texture.NativeImage;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

import static org.lwjgl.opengl.GL11C.*;

public final class WebPDecoder {
    public static boolean checkMagic(byte @NotNull [] buf) {
        if (buf.length >= 12) {
            var wr = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
            var riff = wr.getInt() == 0x46464952;
            var size = wr.getInt() == buf.length - 8;
            var webp = wr.getInt() == 0x50424557;
            var vp8_ = ArrayUtils.contains(new int[]{0x58385056, 0x4C385056, 0x20385056}, wr.getInt());
            return riff && size && webp && vp8_;
        }
        return false;
    }

    public static NativeImage toNativeImage(byte @NotNull [] buf) {
        int[] rgbaSwizzle = new int[]{GL_RED, GL_GREEN, GL_BLUE, GL_ALPHA};
        try (var stream = new ByteArrayInputStream(buf)) {
            try (var imageStream = ImageIO.createImageInputStream(stream)) {
                var readParam = new WebPReadParam();
                readParam.setBypassFiltering(true);
                var reader = ImageIO.getImageReadersByMIMEType("image/webp").next();
                reader.setInput(imageStream);
                var image = reader.read(0, readParam);
                if (!(image.getColorModel() instanceof DirectColorModel imageColorModel)) {
                    var colorModelType = image.getColorModel().getClass();
                    throw new IOException("unrecognized color model type: " + colorModelType.getName());
                }
                var bigEndian = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
                for (var i = 0; i < rgbaSwizzle.length; ++i) {
                    var mask = switch (i) {
                        case 0 -> imageColorModel.getRedMask();
                        case 1 -> imageColorModel.getGreenMask();
                        case 2 -> imageColorModel.getBlueMask();
                        case 3 -> imageColorModel.getAlphaMask();
                        default -> 0x00000000;
                    };
                    rgbaSwizzle[i] = switch (mask) {
                        case 0x00000000 -> GL_ZERO;
                        case 0xFF000000 -> bigEndian ? GL_RED : GL_ALPHA;
                        case 0x00FF0000 -> bigEndian ? GL_GREEN : GL_BLUE;
                        case 0x0000FF00 -> bigEndian ? GL_BLUE : GL_GREEN;
                        case 0x000000FF -> bigEndian ? GL_ALPHA : GL_RED;
                        default -> throw new IOException("unrecognized rgba mask[%d]: 0x%08X".formatted(i, mask));
                    };
                }
                if (!(image.getData().getDataBuffer() instanceof DataBufferInt imageDataBuffer)) {
                    var bufferType = image.getData().getDataBuffer().getClass();
                    throw new IOException("unrecognized data buffer type: " + bufferType.getName());
                }
                var nativeImage = new NativeImage(image.getWidth(), image.getHeight(), false);
                int size = image.getWidth() * image.getHeight() * 4;
                var nativeBuffer = MemoryUtil.memByteBuffer(nativeImage.pointer, Math.toIntExact(size));
                Objects.requireNonNull(nativeBuffer).asIntBuffer().put(imageDataBuffer.getData());
                return nativeImage;
            }
        }
        catch (Throwable e) {
            return null;
        }
    }
}