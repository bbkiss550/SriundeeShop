package com.sriundee.preorder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.mock.web.MockMultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sriundee.preorder.service.LocalProductImageReader.ImageReadException;

class LocalProductImageReaderTest {
    @Test
    @EnabledIfSystemProperty(named = "ocr.price-crop.sample", matches = ".+")
    void reportedRightColumnCropKeepsLeadingDigitAndDeposit() throws Exception {
        BufferedImage pixels = ImageIO.read(Path.of(System.getProperty("ocr.price-crop.sample")).toFile());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(pixels.getSubimage(595, 415, 605, 785), "png", bytes);
        var result = reader.read(new MockMultipartFile("image", "selected-crop.png", "image/png", bytes.toByteArray()));
        Path report = Path.of(".runtime/product-setup-qa/right-column-ocr.txt");
        Files.createDirectories(report.getParent()); Files.writeString(report, result.rawText());
        Files.writeString(report.resolveSibling("right-column-draft.json"), new ObjectMapper().writeValueAsString(result));
        var offer = result.versions().getFirst().groups().getFirst();
        assertThat(offer.price()).as("OCR text: %s", result.rawText()).isEqualByComparingTo("590");
        assertThat(offer.deposit()).as("OCR text: %s", result.rawText()).isEqualByComparingTo("300");
        assertThat(result.versions().stream().flatMap(version -> version.groups().stream().map(group ->
                version.name() + "|" + group.name() + "|" + group.price() + "|" + group.deposit())).toList())
                .containsExactly("PHOTOBOOK VER.|สุ่ม|590|300", "PHOTOBOOK VER.|SET(3)|1750|900",
                    "ENGENE VER.|สุ่ม|400|200", "ENGENE VER.|DAY SET(6)|2390|1200", "ENGENE VER.|NIGHT SET(6)|2390|1200",
                    "WEVERSE ALBUMS VER.|เดี่ยว|300|150");
    }
    @Test
    @EnabledIfSystemProperty(named = "ocr.set.sample", matches = ".+")
    void readsSinglesAndAllSetOptionsFromTheUploadedExample() throws Exception {
        BufferedImage pixels = ImageIO.read(Path.of(System.getProperty("ocr.set.sample")).toFile());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(pixels, "png", bytes);
        var result = reader.read(new MockMultipartFile("image", "full-image.png", "image/png", bytes.toByteArray()));
        Path report = Path.of(".runtime/product-setup-qa/full-set-ocr.txt");
        Files.createDirectories(report.getParent());
        Files.writeString(report, result.rawText());
        Files.writeString(report.resolveSibling("full-set-draft.json"), new ObjectMapper().writeValueAsString(result));
        assertThat(result.versions()).as("OCR text: %s", result.rawText()).hasSize(3);
        var photo = result.versions().getFirst().groups();
        assertThat(photo.getFirst().price()).as("OCR text: %s", result.rawText()).isEqualByComparingTo("590");
        assertThat(photo.getFirst().deposit()).as("OCR text: %s", result.rawText()).isEqualByComparingTo("300");
        assertThat(photo).as("OCR text: %s", result.rawText()).extracting(group -> group.name()).containsExactly("สุ่ม", "SET(3)");
        assertThat(photo.get(1).price()).isEqualByComparingTo("1750");
        assertThat(photo.get(1).deposit()).isEqualByComparingTo("900");
        var engene = result.versions().get(1).groups();
        assertThat(engene).as("OCR text: %s", result.rawText()).extracting(group -> group.name()).containsExactly("สุ่ม", "DAY SET(6)", "NIGHT SET(6)");
        assertThat(engene.get(1).price()).isEqualByComparingTo("2390");
        assertThat(engene.get(2).deposit()).isEqualByComparingTo("1200");
        assertThat(result.versions().stream().flatMap(version -> version.groups().stream()).toList())
                .allSatisfy(group -> assertThat(group.websites()).containsExactly("Weverse Shop"));
    }
    @Test
    void stopsRunningPythonAndReleasesTheOcrSlot() throws Exception {
        var progress = new LocalProductImageReader.ReadProgress();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB), "png", bytes);
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var future = executor.submit(() -> reader.read(new MockMultipartFile("image", "sample.png", "image/png", bytes.toByteArray()), progress));
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
            while (progress.view().percent() < 20 && !future.isDone() && System.nanoTime() < deadline) Thread.sleep(10);
            assertThat(progress.view().percent()).isGreaterThanOrEqualTo(20);
            progress.cancel();
            assertThatThrownBy(() -> future.get(5, java.util.concurrent.TimeUnit.SECONDS))
                    .hasCauseInstanceOf(ImageReadException.class).hasStackTraceContaining("หยุดอ่านภาพ");
            assertThat(progress.view().done()).isTrue();
            assertThat(progress.view().percent()).isLessThan(100);
            // Invalid image now reaches validation, proving the single OCR slot was released.
            assertThatThrownBy(() -> reader.read(new MockMultipartFile("image", "bad".getBytes())))
                    .hasMessageContaining("PNG");
        } finally { executor.shutdownNow(); }
    }
    private final LocalProductImageReader reader = new LocalProductImageReader(new ObjectMapper(),
            new ProductImageTextParser(), "python", "", 120);

    @Test
    void rejectsAnEmptyUpload() {
        assertThatThrownBy(() -> reader.read(new MockMultipartFile("image", new byte[0])))
                .isInstanceOf(ImageReadException.class).hasMessageContaining("เลือกภาพ");
    }

    @Test
    void rejectsNonImagesRegardlessOfFilenameOrMimeType() {
        assertThatThrownBy(() -> reader.read(new MockMultipartFile("image", "looks-like.png", "image/png", "not an image".getBytes())))
                .isInstanceOf(ImageReadException.class).hasMessageContaining("PNG");
    }

    @Test
    void rejectsOversizedUploadsBeforeStartingPython() {
        assertThatThrownBy(() -> reader.read(new MockMultipartFile("image", new byte[(int) LocalProductImageReader.MAX_IMAGE_BYTES + 1])))
                .isInstanceOf(ImageReadException.class).hasMessageContaining("8 MB");
    }

    @Test
    void rejectsUnsupportedImageFormatEvenWithPngExtension() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB), "gif", bytes);
        assertThatThrownBy(() -> reader.read(new MockMultipartFile("image", "fake.png", "image/png", bytes.toByteArray())))
                .isInstanceOf(ImageReadException.class).hasMessageContaining("PNG");
    }

    @Test
    void reportsMissingPythonWithoutSendingAnythingElsewhere() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB), "png", bytes);
        var unavailable = new LocalProductImageReader(new ObjectMapper(), new ProductImageTextParser(), "nonexistent-ocr-python-executable", "", 10);
        assertThatThrownBy(() -> unavailable.read(new MockMultipartFile("image", "sample.png", "image/png", bytes.toByteArray())))
                .isInstanceOf(ImageReadException.class).hasMessageContaining("ยังไม่พร้อม");
    }

    @Test
    @EnabledIfSystemProperty(named = "ocr.sample", matches = ".+")
    void readsTheSelectedAnnouncementWithRealLocalOcr() throws Exception {
        Path selectedImage = Path.of(System.getProperty("ocr.sample"));
        var result = reader.read(new MockMultipartFile("image", "upload.png", "image/png", Files.readAllBytes(selectedImage)));
        assertThat(result.versions()).hasSize(2);
        assertThat(result.versions().getFirst().groups()).hasSize(2);
        assertThat(result.versions().getFirst().groups().getFirst().price()).isEqualByComparingTo("1350");
        assertThat(result.versions().getFirst().groups().get(1).price()).isEqualByComparingTo("1450");
        assertThat(result.versions().get(1).groups().getFirst().price()).isEqualByComparingTo("950");
        assertThat(result.versions().get(1).groups().getFirst().websites()).containsExactly(WebsiteQuickAddService.NO_WEBSITE);
    }

    @Test
    @EnabledIfSystemProperty(named = "ocr.no-version.sample", matches = ".+")
    void readsTheReportedNoVersionCropWithRealLocalOcr() throws Exception {
        Path screenshot = Path.of(System.getProperty("ocr.no-version.sample"));
        BufferedImage pixels = ImageIO.read(screenshot.toFile());
        // The supplied screenshot shows the selected image preview in this rectangle.
        BufferedImage uploadedRegion = pixels.getSubimage(18, 9, 230, 180);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(uploadedRegion, "png", bytes);
        var result = reader.read(new MockMultipartFile("image", "selected-crop.png", "image/png", bytes.toByteArray()));
        assertThat(result.versions()).hasSize(1);
        assertThat(result.versions().getFirst().name()).isEqualTo("Standard");
        var groups = result.versions().getFirst().groups();
        assertThat(groups).hasSize(2);
        assertThat(groups.getFirst().price()).isEqualByComparingTo("1390");
        assertThat(groups.getFirst().deposit()).isEqualByComparingTo("700");
        assertThat(groups.getFirst().websites()).as("OCR text: %s", result.rawText()).contains("YES24", "FNC STORE", "Plus Chat", "Mnet Plus Merch");
        assertThat(groups.get(1).price()).isEqualByComparingTo("1290");
        assertThat(groups.get(1).deposit()).isEqualByComparingTo("600");
        assertThat(groups.get(1).websites()).containsExactly("Ktown4u");
    }
}
