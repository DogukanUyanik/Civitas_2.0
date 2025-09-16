package org.example.civitaswebapp.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.example.civitaswebapp.domain.Member;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PdfServiceImpl implements PdfService {

    private final TemplateEngine templateEngine;

    public PdfServiceImpl(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public byte[] generateMemberPdf(Member member, String memberStatusLabel, List<Map<String, Object>> transactions, Locale locale) throws Exception {
        Context context = new Context(locale);
        context.setVariable("member", member);
        context.setVariable("memberStatusLabel", memberStatusLabel);
        context.setVariable("transactions", transactions);

        String html = templateEngine.process("members/memberDetailsPdf", context);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();

            // Add font support for Turkish characters
            setupFonts(builder);

            // Use HTML string instead of file
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();

            return os.toByteArray();
        }
    }

    private void setupFonts(PdfRendererBuilder builder) {
        try {
            // Option 1: Try to load DejaVu Sans from classpath
            ClassPathResource fontResource = new ClassPathResource("fonts/DejaVuSans.ttf");
            if (fontResource.exists()) {
                // Create a temporary file for the font
                Path tempFont = Files.createTempFile("dejavu", ".ttf");
                try (InputStream fontStream = fontResource.getInputStream()) {
                    Files.copy(fontStream, tempFont, StandardCopyOption.REPLACE_EXISTING);
                    builder.useFont(tempFont.toFile(), "DejaVu Sans");
                    System.out.println("Successfully loaded DejaVu Sans from classpath");
                    return;
                }
            }
        } catch (Exception e) {
            System.out.println("Could not load DejaVu Sans from classpath: " + e.getMessage());
        }

        // Option 2: Try system fonts (Ubuntu paths)
        try {
            String[] ubuntuFontPaths = {
                    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                    "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
                    "/usr/share/fonts/TTF/DejaVuSans.ttf",
                    "/usr/share/fonts/truetype/ubuntu/Ubuntu-R.ttf"
            };

            for (String fontPath : ubuntuFontPaths) {
                File fontFile = new File(fontPath);
                if (fontFile.exists()) {
                    builder.useFont(fontFile, "SystemFont");
                    System.out.println("Successfully loaded font: " + fontPath);
                    return;
                }
            }

            System.out.println("Warning: No suitable system font file found for Turkish characters");
        } catch (Exception e) {
            System.out.println("Error setting up system fonts: " + e.getMessage());
        }
    }

    // Add this method to your PdfServiceImpl class for debugging
    public void debugFontSetup() {
        System.out.println("=== Font Debug Information ===");

        // Check classpath font
        ClassPathResource fontResource = new ClassPathResource("fonts/DejaVuSans.ttf");
        System.out.println("DejaVu Sans in classpath exists: " + fontResource.exists());

        // Check system fonts
        String[] ubuntuFontPaths = {
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
                "/usr/share/fonts/TTF/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/ubuntu/Ubuntu-R.ttf"
        };

        System.out.println("System fonts check:");
        for (String path : ubuntuFontPaths) {
            File file = new File(path);
            System.out.println("  " + path + ": " + file.exists());
        }

        System.out.println("================================");
    }
}