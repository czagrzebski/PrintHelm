package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.web.domain.BusinessSettings;
import com.czagrzebski.printhelm.web.domain.GcodeFilamentInfo;
import com.czagrzebski.printhelm.web.domain.GcodeMetadata;
import com.czagrzebski.printhelm.web.domain.JobOrder;
import com.czagrzebski.printhelm.web.repository.GcodeMetadataRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class InvoiceService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMMM d, yyyy");
    private static final Color HEADER_BG = new Color(30, 41, 59);
    private static final Color ACCENT    = new Color(6, 182, 212);
    private static final Color ROW_ALT   = new Color(241, 245, 249);
    private static final Color BORDER    = new Color(203, 213, 225);

    private final BusinessSettingsService businessSettingsService;
    private final GcodeMetadataRepository gcodeMetadataRepository;

    public InvoiceService(BusinessSettingsService businessSettingsService, GcodeMetadataRepository gcodeMetadataRepository) {
        this.businessSettingsService = businessSettingsService;
        this.gcodeMetadataRepository = gcodeMetadataRepository;
    }

    public byte[] generate(JobOrder order) {
        BusinessSettings settings = businessSettingsService.getSettings();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font titleFont   = new Font(Font.HELVETICA, 22, Font.BOLD, Color.WHITE);
        Font subFont     = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(148, 163, 184));
        Font labelFont   = new Font(Font.HELVETICA, 9, Font.BOLD, new Color(71, 85, 105));
        Font valueFont   = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(30, 41, 59));
        Font sectionFont = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(30, 41, 59));
        Font tableHFont  = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
        Font tableVFont  = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(30, 41, 59));
        Font totalFont   = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(30, 41, 59));
        Font accentFont  = new Font(Font.HELVETICA, 10, Font.BOLD, ACCENT);

        // ── Header banner ──────────────────────────────────────────────────────
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{2f, 1f});

        PdfPCell nameCell = new PdfPCell();
        nameCell.setBorder(Rectangle.NO_BORDER);
        nameCell.setBackgroundColor(HEADER_BG);
        nameCell.setPadding(16);
        String businessName = settings.getBusinessName() != null ? settings.getBusinessName() : "";
        nameCell.addElement(new Phrase(businessName, titleFont));
        if (settings.getBusinessAddress() != null && !settings.getBusinessAddress().isBlank())
            nameCell.addElement(new Phrase(settings.getBusinessAddress(), subFont));
        if (settings.getBusinessEmail() != null && !settings.getBusinessEmail().isBlank())
            nameCell.addElement(new Phrase(settings.getBusinessEmail(), subFont));
        if (settings.getBusinessPhone() != null && !settings.getBusinessPhone().isBlank())
            nameCell.addElement(new Phrase(settings.getBusinessPhone(), subFont));
        header.addCell(nameCell);

        Font invoiceTagFont = new Font(Font.HELVETICA, 13, Font.BOLD, ACCENT);
        Font invoiceMetaFont = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(148, 163, 184));
        PdfPCell metaCell = new PdfPCell();
        metaCell.setBorder(Rectangle.NO_BORDER);
        metaCell.setBackgroundColor(HEADER_BG);
        metaCell.setPadding(16);
        metaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        metaCell.addElement(new Phrase("INVOICE", invoiceTagFont));
        metaCell.addElement(new Phrase("# " + order.getOrderId(), invoiceMetaFont));
        LocalDateTime dateTime = order.getInvoicedAt() != null ? order.getInvoicedAt() : LocalDateTime.now();
        metaCell.addElement(new Phrase("Date: " + dateTime.format(DATE_FMT), invoiceMetaFont));
        header.addCell(metaCell);

        doc.add(header);
        doc.add(Chunk.NEWLINE);

        // ── Customer section ───────────────────────────────────────────────────
        doc.add(sectionParagraph("BILL TO", sectionFont));
        doc.add(infoLine("Customer", order.getCustomerName(), labelFont, valueFont));
        if (order.getCustomerEmail() != null && !order.getCustomerEmail().isBlank())
            doc.add(infoLine("Email", order.getCustomerEmail(), labelFont, valueFont));
        doc.add(Chunk.NEWLINE);

        // ── Order detail section ───────────────────────────────────────────────
        doc.add(sectionParagraph("ORDER DETAILS", sectionFont));
        if (order.getDescription() != null && !order.getDescription().isBlank())
            doc.add(infoLine("Description", order.getDescription(), labelFont, valueFont));
        if (order.getRequirements() != null && !order.getRequirements().isBlank())
            doc.add(infoLine("Requirements", order.getRequirements(), labelFont, valueFont));
        if (order.getGcodeFilename() != null)
            doc.add(infoLine("GCode File", order.getGcodeFilename(), labelFont, valueFont));

        if (order.getMongoGcodeMetadataId() != null) {
            gcodeMetadataRepository.findById(order.getMongoGcodeMetadataId()).ifPresent(meta -> {
                String filamentSummary = buildFilamentSummary(meta);
                if (!filamentSummary.isBlank()) {
                    try { doc.add(infoLine("Filaments", filamentSummary, labelFont, valueFont)); }
                    catch (DocumentException ignored) {}
                }
            });
        }
        doc.add(Chunk.NEWLINE);

        // ── Cost breakdown table ───────────────────────────────────────────────
        doc.add(sectionParagraph("COST BREAKDOWN", sectionFont));

        PdfPTable costTable = new PdfPTable(2);
        costTable.setWidthPercentage(60);
        costTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        costTable.setWidths(new float[]{3f, 1.2f});

        addTableHeader(costTable, tableHFont, "Item", "Amount");

        BigDecimal subtotal = BigDecimal.ZERO;
        int rowIdx = 0;

        if (order.getMaterialCost() != null && order.getMaterialCost().compareTo(BigDecimal.ZERO) > 0) {
            addTableRow(costTable, tableVFont, "Material Cost", formatMoney(order.getMaterialCost()), rowIdx++ % 2 == 1);
            subtotal = subtotal.add(order.getMaterialCost());
        }
        if (order.getLaborCost() != null && order.getLaborCost().compareTo(BigDecimal.ZERO) > 0) {
            addTableRow(costTable, tableVFont, "Labor / Design Fee", formatMoney(order.getLaborCost()), rowIdx++ % 2 == 1);
            subtotal = subtotal.add(order.getLaborCost());
        }
        if (order.getSetupFee() != null && order.getSetupFee().compareTo(BigDecimal.ZERO) > 0) {
            addTableRow(costTable, tableVFont, "Setup Fee", formatMoney(order.getSetupFee()), rowIdx++ % 2 == 1);
            subtotal = subtotal.add(order.getSetupFee());
        }

        // Subtotal row
        addDividerRow(costTable);
        addTableRow(costTable, tableVFont, "Subtotal", formatMoney(subtotal), false);

        BigDecimal discountAmt = order.getDiscount() != null ? order.getDiscount() : BigDecimal.ZERO;
        if (discountAmt.compareTo(BigDecimal.ZERO) > 0) {
            Font discountFont = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(220, 38, 38));
            addColoredTableRow(costTable, discountFont, "Discount", "- " + formatMoney(discountAmt));
        }

        // Total row
        addDividerRow(costTable);
        BigDecimal total = subtotal.subtract(discountAmt).max(BigDecimal.ZERO);
        addTotalRow(costTable, totalFont, accentFont, "TOTAL", formatMoney(total));

        doc.add(costTable);

        // ── Notes section ──────────────────────────────────────────────────────
        if (order.getInvoiceNotes() != null && !order.getInvoiceNotes().isBlank()) {
            doc.add(Chunk.NEWLINE);
            doc.add(sectionParagraph("NOTES", sectionFont));
            Paragraph notes = new Paragraph(order.getInvoiceNotes(), valueFont);
            notes.setSpacingBefore(4);
            doc.add(notes);
        }

        // ── Footer ─────────────────────────────────────────────────────────────
        doc.add(Chunk.NEWLINE);
        doc.add(Chunk.NEWLINE);
        Font footerFont = new Font(Font.HELVETICA, 8, Font.ITALIC, new Color(148, 163, 184));
        Paragraph footer = new Paragraph("Thank you for your business.", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);

        doc.close();
        return out.toByteArray();
    }

    private Paragraph sectionParagraph(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setSpacingBefore(8);
        p.setSpacingAfter(4);
        return p;
    }

    private Paragraph infoLine(String label, String value, Font labelFont, Font valueFont) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + ": ", labelFont));
        p.add(new Chunk(value, valueFont));
        p.setSpacingAfter(2);
        return p;
    }

    private void addTableHeader(PdfPTable table, Font font, String... headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, font));
            cell.setBackgroundColor(HEADER_BG);
            cell.setBorderColor(HEADER_BG);
            cell.setPadding(6);
            table.addCell(cell);
        }
    }

    private void addTableRow(PdfPTable table, Font font, String label, String value, boolean alt) {
        Color bg = alt ? ROW_ALT : Color.WHITE;
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBackgroundColor(bg);
        labelCell.setBorderColor(BORDER);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setBackgroundColor(bg);
        valueCell.setBorderColor(BORDER);
        valueCell.setPadding(5);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private void addColoredTableRow(PdfPTable table, Font font, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBorderColor(BORDER);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setBorderColor(BORDER);
        valueCell.setPadding(5);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private void addDividerRow(PdfPTable table) {
        for (int i = 0; i < 2; i++) {
            PdfPCell cell = new PdfPCell(new Phrase(""));
            cell.setBorderWidthTop(0.5f);
            cell.setBorderColorTop(BORDER);
            cell.setBorderWidthBottom(0);
            cell.setBorderWidthLeft(0);
            cell.setBorderWidthRight(0);
            cell.setFixedHeight(1f);
            table.addCell(cell);
        }
    }

    private void addTotalRow(PdfPTable table, Font labelFont, Font valueFont, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBackgroundColor(new Color(241, 245, 249));
        labelCell.setBorderColor(BORDER);
        labelCell.setPadding(6);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBackgroundColor(new Color(241, 245, 249));
        valueCell.setBorderColor(BORDER);
        valueCell.setPadding(6);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private String buildFilamentSummary(GcodeMetadata meta) {
        if (meta.getFilaments() == null || meta.getFilaments().isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        List<GcodeFilamentInfo> filaments = meta.getFilaments();
        for (int i = 0; i < filaments.size(); i++) {
            GcodeFilamentInfo f = filaments.get(i);
            if (i > 0) sb.append(", ");
            sb.append("Slot ").append(f.getSlotIndex() + 1).append(": ").append(f.getType());
            if (f.getColor() != null) sb.append(" (").append(f.getColor()).append(")");
        }
        return sb.toString();
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "$0.00";
        return "$" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
