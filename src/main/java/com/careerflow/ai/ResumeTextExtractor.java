package com.careerflow.ai;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.io.*;

@Component
public class ResumeTextExtractor {
    public static final int MAX_PAGES = 20;
    public static final int MAX_CHARACTERS = 30000;
    public record ExtractedText(String text, int pages) { }

    public ExtractedText extract(byte[] content) {
        if (content == null || content.length == 0 || content.length > 5 * 1024 * 1024)
            throw invalid("Resume must contain a PDF of at most 5 MB");
        try (var document = Loader.loadPDF(content)) {
            if (document.isEncrypted()) throw invalid("Use an unencrypted PDF for analysis");
            if (document.getNumberOfPages() == 0 || document.getNumberOfPages() > MAX_PAGES)
                throw invalid("Resume analysis supports 1 to 20 pages");
            var writer = new BoundedWriter();
            var stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.writeText(document, writer);
            String text = writer.toString().strip();
            if (text.isBlank()) throw invalid("No readable text found. Upload a text-based PDF; scanned images require OCR");
            return new ExtractedText(text, document.getNumberOfPages());
        } catch (InvalidPasswordException exception) {
            throw invalid("Use an unencrypted PDF for analysis");
        } catch (TextLimitException exception) {
            throw invalid("Resume text exceeds the 30000 character analysis limit");
        } catch (IOException exception) {
            throw invalid("Resume could not be read as a PDF");
        }
    }
    private ResponseStatusException invalid(String detail) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, detail);
    }
    private static class TextLimitException extends IOException { }
    private static class BoundedWriter extends Writer {
        private final StringBuilder text = new StringBuilder();
        @Override public void write(char[] chars, int offset, int length) throws IOException {
            if (text.length() + length > MAX_CHARACTERS) throw new TextLimitException();
            text.append(chars, offset, length);
        }
        @Override public void flush() { }
        @Override public void close() { }
        @Override public String toString() { return text.toString(); }
    }
}
