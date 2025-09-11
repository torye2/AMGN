package amgn.amu.service.util;

import amgn.amu.common.AppException;
import amgn.amu.common.ErrorCode;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class QrUtil {
    public static String toDataUrlPng(String contents) {
        try {
            int size = 256;
            BitMatrix m = new QRCodeWriter().encode(contents, BarcodeFormat.QR_CODE, size, size);
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
                img.setRGB(x, y, m.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new AppException(ErrorCode.MFA_QR_GENERATION_FAILED, e);
        }
    }
}
