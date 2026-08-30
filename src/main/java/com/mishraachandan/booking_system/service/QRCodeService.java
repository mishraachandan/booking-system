package com.mishraachandan.booking_system.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

@Service
@Slf4j
public class QRCodeService {

    public byte[] generateQRCodeBytes(String content, int width, int height) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 2);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (WriterException | IOException e) {
            log.error("Failed to generate QR code for content: {}", content, e);
            throw new RuntimeException("QR code generation failed", e);
        }
    }
    
    public byte[] generateBookingQR(Long bookingId, String userEmail) {
        // Content: BMS-TICKET:{bookingId}:{userEmail}:{timestamp}
        String content = String.format("BMS-TICKET:%d:%s:%d", bookingId, userEmail, System.currentTimeMillis());
        return generateQRCodeBytes(content, 300, 300);
    }
}
