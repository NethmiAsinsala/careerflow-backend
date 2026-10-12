package com.careerflow.ai;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.encryption.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.io.ByteArrayOutputStream;
import static org.assertj.core.api.Assertions.*;

class ResumeTextExtractorTest {
    final ResumeTextExtractor extractor = new ResumeTextExtractor();
    static byte[] pdf(String text, int pages, boolean encrypted) throws Exception {
        try (var doc = new PDDocument(); var bytes = new ByteArrayOutputStream()) {
            for(int i=0;i<pages;i++) {
                var page = new PDPage();doc.addPage(page);
                if (!text.isEmpty()) try(var stream = new PDPageContentStream(doc,page)) {
                    stream.beginText();stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),10);
                    stream.newLineAtOffset(40,700);stream.showText(text);stream.endText();
                }
            }
            if(encrypted) doc.protect(new StandardProtectionPolicy("owner","password",new AccessPermission()));
            doc.save(bytes);return bytes.toByteArray();
        }
    }
    @Test void extractsRealPdfText() throws Exception {
        var result=extractor.extract(pdf("Java Spring Boot developer",1,false));
        assertThat(result.text()).contains("Java Spring Boot developer");assertThat(result.pages()).isEqualTo(1);
    }
    @Test void rejectsMalformedAndEmptyPdf() {
        assertThatThrownBy(()->extractor.extract("%PDF-fake %%EOF".getBytes())).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->extractor.extract(new byte[0])).isInstanceOf(ResponseStatusException.class);
    }
    @Test void rejectsScannedOrBlankDocuments() throws Exception {
        byte[] bytes=pdf("",1,false);
        assertThatThrownBy(()->extractor.extract(bytes)).hasMessageContaining("No readable text");
    }
    @Test void rejectsEncryptedPdf() throws Exception {
        byte[] bytes=pdf("Java",1,true);
        assertThatThrownBy(()->extractor.extract(bytes)).hasMessageContaining("unencrypted");
    }
    @Test void enforcesPageAndTextLimits() throws Exception {
        byte[] pages=pdf("Java",21,false),large=pdf("Java ".repeat(6100),1,false);
        assertThatThrownBy(()->extractor.extract(pages)).hasMessageContaining("20 pages");
        assertThatThrownBy(()->extractor.extract(large)).hasMessageContaining("30000");
    }
}
