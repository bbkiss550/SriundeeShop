package com.sriundee.preorder.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sriundee.preorder.dto.ProductImageDraft;

@Service
public class LocalProductImageReader {
    public static final long MAX_IMAGE_BYTES = 8L * 1024 * 1024;
    private static final long MAX_PIXELS = 16_000_000;
    private final Semaphore slots = new Semaphore(1);
    private final ObjectMapper mapper;
    private final ProductImageTextParser parser;
    private final String python;
    private final String modelDirectory;
    private final Duration timeout;

    public LocalProductImageReader(ObjectMapper mapper, ProductImageTextParser parser,
            @Value("${shop.ocr.python:python}") String python,
            @Value("${shop.ocr.model-directory:}") String modelDirectory,
            @Value("${shop.ocr.timeout-seconds:120}") long timeoutSeconds) {
        this.mapper = mapper;
        this.parser = parser;
        this.python = python;
        this.modelDirectory = modelDirectory;
        this.timeout = Duration.ofSeconds(Math.clamp(timeoutSeconds, 10, 180));
    }

    public ProductImageDraft read(MultipartFile upload) {
        return read(upload, new ReadProgress());
    }

    public static class ReadProgress {
        private int percent;
        private String message = "รอเริ่มอ่านภาพ";
        private boolean cancelled;
        private boolean done;
        private boolean started;
        private final long createdAt = System.nanoTime();
        public synchronized boolean expired() { return done && System.nanoTime() - createdAt > TimeUnit.MINUTES.toNanos(10); }
        public synchronized void begin() { check(); started = true; }
        public synchronized void update(int percent, String message) {
            if (!cancelled && !done && percent >= this.percent) { this.percent = percent; this.message = message; }
        }
        public synchronized void cancel() {
            if (!done) { cancelled = true; message = "หยุดอ่านภาพแล้ว"; if (!started) done = true; }
        }
        public synchronized void check() { if (cancelled) throw new ImageReadException(409, "หยุดอ่านภาพแล้ว"); }
        public synchronized void finish() { done = true; }
        public synchronized ProgressView view() { return new ProgressView(percent, message, done, cancelled); }
    }
    public record ProgressView(int percent, String message, boolean done, boolean cancelled) {}

    public ProductImageDraft read(MultipartFile upload, ReadProgress progress) {
        progress.begin();
        if (upload == null || upload.isEmpty()) throw new ImageReadException(400, "กรุณาเลือกภาพที่ต้องการอ่าน");
        if (upload.getSize() > MAX_IMAGE_BYTES) throw new ImageReadException(413, "ภาพต้องมีขนาดไม่เกิน 8 MB");
        if (!slots.tryAcquire()) throw new ImageReadException(429, "กำลังอ่านภาพอีกคำขอหนึ่ง กรุณาลองใหม่เมื่ออ่านเสร็จ");
        Path directory = null;
        Process process = null;
        try {
            directory = Files.createTempDirectory("sriundee-upload-ocr-");
            Path image = directory.resolve("upload.png");
            normalize(upload, image);
            progress.check();
            progress.update(10, "ตรวจสอบภาพส่วนที่เลือกแล้ว");
            Path script = directory.resolve("read_product_image.py");
            Path output = directory.resolve("result.json");
            Path progressFile = directory.resolve("progress.txt");
            try (InputStream source = new ClassPathResource("ocr/read_product_image.py").getInputStream()) {
                Files.copy(source, script, StandardCopyOption.REPLACE_EXISTING);
            }
            List<String> command = new ArrayList<>(List.of(python, "-I", script.toString(),
                    "--input", image.toString(), "--output", output.toString(), "--progress", progressFile.toString()));
            if (!modelDirectory.isBlank()) command.addAll(List.of("--model-dir", modelDirectory));
            // No shell, URL, filename or user-supplied command is executed. Only this upload is read.
            process = new ProcessBuilder(command).redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            progress.update(20, "กำลังเริ่มโปรแกรมอ่านภาพในเครื่อง");
            long deadline = System.nanoTime() + timeout.toNanos();
            while (!process.waitFor(250, TimeUnit.MILLISECONDS)) {
                progress.check();
                if (System.nanoTime() >= deadline) throw new ImageReadException(504, "อ่านภาพนานเกินกำหนด ลองใช้ภาพที่เล็กลงหรือครอปเฉพาะส่วนข้อความ");
                if (Files.isRegularFile(progressFile)) {
                    try {
                        int value = Integer.parseInt(Files.readString(progressFile).strip());
                        if (value >= 30 && value <= 90) progress.update(value,
                            value < 40 ? "กำลังโหลดโมเดลภาษาไทยและอังกฤษ" : value < 90 ? "กำลังอ่านข้อความในภาพ" : "อ่านข้อความแล้ว กำลังจัดกลุ่มข้อมูล");
                    } catch (NumberFormatException | IOException ignored) { /* Retry an in-progress file write next tick. */ }
                }
            }
            progress.check();
            if (process.exitValue() == 2) throw notReady();
            if (process.exitValue() != 0 || !Files.isRegularFile(output) || Files.size(output) > 1_000_000) {
                throw new ImageReadException(422, "อ่านภาพนี้ไม่ได้ ลองใช้ภาพที่ชัดขึ้นหรือครอปเฉพาะส่วนข้อความ");
            }
            JsonNode result = mapper.readTree(Files.readString(output, StandardCharsets.UTF_8));
            List<String> lines = new ArrayList<>();
            for (JsonNode row : result.path("lines")) lines.add(row.path("text").asText());
            progress.update(95, "กำลังจัดกลุ่มเวอร์ชัน เว็บไซต์ และราคา");
            ProductImageDraft draft = parser.parse(String.join("\n", lines));
            progress.check();
            progress.update(100, "อ่านภาพเสร็จแล้ว");
            return draft.withReadInfo("OCR","อ่านภาพด้วย OCR ภายในเครื่อง");
        } catch (ImageReadException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ImageReadException(503, "การอ่านภาพถูกหยุด กรุณาลองใหม่");
        } catch (IOException e) {
            throw notReady();
        } finally {
            if (process != null && process.isAlive()) {
                process.descendants().forEach(child -> child.destroyForcibly());
                process.destroyForcibly();
                try { process.waitFor(5, TimeUnit.SECONDS); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            if (directory != null) {
                // Only known request-local files are removed; no image is retained in the application.
                for (String name : List.of("upload.png", "read_product_image.py", "result.json", "progress.txt")) {
                    try { Files.deleteIfExists(directory.resolve(name)); } catch (IOException ignored) { }
                }
                try { Files.deleteIfExists(directory); } catch (IOException ignored) { }
            }
            slots.release();
            progress.finish();
        }
    }

    private void normalize(MultipartFile upload, Path destination) throws IOException {
        Files.write(destination,normalizedImage(upload));
    }
    public byte[] normalizedImage(MultipartFile upload) throws IOException {
        if(upload==null || upload.isEmpty())throw new ImageReadException(400,"กรุณาเลือกภาพที่ต้องการอ่าน");
        if(upload.getSize()>MAX_IMAGE_BYTES)throw new ImageReadException(413,"ภาพต้องมีขนาดไม่เกิน 8 MB");
        try (InputStream stream = upload.getInputStream(); ImageInputStream source = ImageIO.createImageInputStream(stream)) {
            if (source == null) throw new ImageReadException(400, "ไฟล์นี้ไม่ใช่ภาพ PNG หรือ JPEG");
            var candidates = ImageIO.getImageReaders(source);
            if (!candidates.hasNext()) throw new ImageReadException(400, "รองรับเฉพาะภาพ PNG และ JPEG");
            ImageReader reader = candidates.next();
            try {
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("png") && !format.equalsIgnoreCase("jpeg")) {
                    throw new ImageReadException(400, "รองรับเฉพาะภาพ PNG และ JPEG");
                }
                reader.setInput(source);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 20 || height < 20 || (long) width * height > MAX_PIXELS) {
                    throw new ImageReadException(400, "ภาพต้องมีขนาดอย่างน้อย 20 × 20 และไม่เกิน 16 ล้านพิกเซล");
                }
                BufferedImage pixels = reader.read(0);
                var bytes=new java.io.ByteArrayOutputStream();ImageIO.write(pixels,"png",bytes);return bytes.toByteArray();
            } catch (javax.imageio.IIOException e) {
                throw new ImageReadException(400, "ไฟล์ภาพเสียหายหรืออ่านไม่ได้");
            } finally { reader.dispose(); }
        }
    }

    private ImageReadException notReady() {
        return new ImageReadException(503, "ยังไม่พร้อมอ่านภาพในเครื่อง กรุณาติดตั้ง Python, EasyOCR และโมเดลภาษาไทย/อังกฤษตามคู่มือ local OCR");
    }

    public static class ImageReadException extends RuntimeException {
        private final int status;
        public ImageReadException(int status, String message) { super(message); this.status = status; }
        public int status() { return status; }
    }
}
