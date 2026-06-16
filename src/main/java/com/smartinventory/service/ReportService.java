package com.smartinventory.service;

import com.smartinventory.dto.response.DashboardResponse;
import com.smartinventory.enums.ReportFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final DashboardService dashboardService;
    private final SmartInventoryService smartInventoryService;

    public ReportFile generate(String type, ReportFormat format) {
        String reportText = buildReport(type);
        byte[] bytes = switch (format) {
            case CSV -> csv(reportText);
            case PDF -> pdf(reportText);
            case DOCX -> docx(reportText);
            case XLSX -> xlsx(reportText);
        };
        String extension = format.name().toLowerCase();
        return new ReportFile(bytes, contentType(format),
                "smart-inventory-" + type.toLowerCase() + "-report." + extension);
    }

    private String buildReport(String type) {
        DashboardResponse dashboard = dashboardService.currentDashboard();
        Map<String, Object> analytics = smartInventoryService.analyticsSummary();
        StringBuilder builder = new StringBuilder();
        builder.append("Smart Inventory ").append(type).append(" Report\n");
        builder.append("Generated At,").append(LocalDateTime.now()).append("\n\n");
        builder.append("Metric,Value\n");
        dashboard.getTotals().forEach((key, value) -> builder.append(key).append(',').append(value).append('\n'));
        dashboard.getRevenue().forEach((key, value) -> builder.append(key).append(',').append(value).append('\n'));
        builder.append("inventoryHealthScore,").append(analytics.get("inventoryHealthScore")).append('\n');
        builder.append("\nInsights\n");
        dashboard.getInsights().forEach(insight -> builder.append("- ").append(insight).append('\n'));
        return builder.toString();
    }

    private byte[] csv(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] pdf(String text) {
        String escaped = text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
        String content = "BT /F1 10 Tf 50 780 Td (" + escaped.replace("\n", ") Tj 0 -14 Td (") + ") Tj ET";
        String pdf = "%PDF-1.4\n"
                + "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n"
                + "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n"
                + "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >> endobj\n"
                + "4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n"
                + "5 0 obj << /Length " + content.length() + " >> stream\n"
                + content + "\nendstream endobj\n"
                + "xref\n0 6\n0000000000 65535 f \n"
                + "trailer << /Root 1 0 R /Size 6 >>\nstartxref\n0\n%%EOF";
        return pdf.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] docx(String text) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(out)) {
                put(zip, "[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                        + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                        + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                        + "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
                        + "</Types>");
                put(zip, "_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                        + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>"
                        + "</Relationships>");
                put(zip, "word/document.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>"
                        + paragraphs(text)
                        + "</w:body></w:document>");
            }
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate DOCX report", e);
        }
    }

    private byte[] xlsx(String text) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(out)) {
                put(zip, "[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                        + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                        + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                        + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                        + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                        + "</Types>");
                put(zip, "_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                        + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                        + "</Relationships>");
                put(zip, "xl/workbook.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                        + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                        + "<sheets><sheet name=\"Report\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
                put(zip, "xl/_rels/workbook.xml.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                        + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                        + "</Relationships>");
                put(zip, "xl/worksheets/sheet1.xml", sheet(text));
            }
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate XLSX report", e);
        }
    }

    private void put(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String paragraphs(String text) {
        StringBuilder builder = new StringBuilder();
        for (String line : text.split("\\R")) {
            builder.append("<w:p><w:r><w:t>")
                    .append(xml(line))
                    .append("</w:t></w:r></w:p>");
        }
        return builder.toString();
    }

    private String sheet(String text) {
        StringBuilder rows = new StringBuilder();
        String[] lines = text.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            rows.append("<row r=\"").append(i + 1).append("\"><c t=\"inlineStr\"><is><t>")
                    .append(xml(lines[i]))
                    .append("</t></is></c></row>");
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>"
                + rows
                + "</sheetData></worksheet>";
    }

    private String xml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String contentType(ReportFormat format) {
        return switch (format) {
            case CSV -> "text/csv";
            case PDF -> "application/pdf";
            case DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        };
    }

    public record ReportFile(byte[] bytes, String contentType, String fileName) {
    }
}
