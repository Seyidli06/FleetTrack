package com.fleettrack.report.pdf;

import com.fleettrack.report.dto.MaintenanceReportRow;
import com.fleettrack.vehicle.dto.VehicleResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Component
public class VehicleMaintenancePdfGenerator {

    private static final PDRectangle PAGE_SIZE =
            new PDRectangle(
                    PDRectangle.A4.getHeight(),
                    PDRectangle.A4.getWidth()
            );

    private static final float MARGIN = 36F;

    private static final float TITLE_SIZE = 18F;
    private static final float NORMAL_SIZE = 10F;
    private static final float TABLE_SIZE = 8F;

    private static final float ROW_HEIGHT = 20F;

    private static final float[] COLUMN_WIDTHS = {
            55F,
            145F,
            85F,
            85F,
            90F,
            90F,
            110F
    };

    private final PDType1Font normalFont =
            new PDType1Font(
                    Standard14Fonts.FontName.HELVETICA
            );

    private final PDType1Font boldFont =
            new PDType1Font(
                    Standard14Fonts.FontName.HELVETICA_BOLD
            );

    public byte[] generate(
            VehicleResponse vehicle,
            List<MaintenanceReportRow> rows
    ) {
        try (
                PDDocument document = new PDDocument();
                ByteArrayOutputStream output =
                        new ByteArrayOutputStream()
        ) {
            PdfCanvas canvas =
                    new PdfCanvas(document);

            canvas.newPage();

            writeReportHeader(
                    canvas,
                    vehicle
            );

            writeTableHeader(canvas);

            for (
                    MaintenanceReportRow row
                    : rows
            ) {
                if (
                        canvas.getY()
                                < MARGIN + ROW_HEIGHT + 40F
                ) {
                    canvas.newPage();

                    writeContinuationHeader(
                            canvas,
                            vehicle
                    );

                    writeTableHeader(canvas);
                }

                writeTableRow(
                        canvas,
                        row
                );
            }

            writeSummary(
                    canvas,
                    rows
            );

            canvas.close();

            document.save(output);

            return output.toByteArray();

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to generate maintenance PDF report",
                    exception
            );
        }
    }

    private void writeReportHeader(
            PdfCanvas canvas,
            VehicleResponse vehicle
    ) throws IOException {

        canvas.text(
                "FleetTrack",
                MARGIN,
                canvas.getY(),
                boldFont,
                TITLE_SIZE
        );

        canvas.moveDown(25F);

        canvas.text(
                "Vehicle Maintenance Report",
                MARGIN,
                canvas.getY(),
                boldFont,
                14F
        );

        canvas.moveDown(28F);

        writeVehicleLine(
                canvas,
                "Vehicle ID",
                String.valueOf(vehicle.id())
        );

        writeVehicleLine(
                canvas,
                "VIN",
                vehicle.vin()
        );

        writeVehicleLine(
                canvas,
                "License Plate",
                vehicle.licensePlate()
        );

        writeVehicleLine(
                canvas,
                "Make / Model",
                vehicle.make()
                        + " "
                        + vehicle.model()
        );

        writeVehicleLine(
                canvas,
                "Manufacture Year",
                String.valueOf(
                        vehicle.manufactureYear()
                )
        );

        writeVehicleLine(
                canvas,
                "Vehicle Status",
                vehicle.status().name()
        );

        canvas.moveDown(12F);

        canvas.horizontalLine(
                MARGIN,
                PAGE_SIZE.getWidth() - MARGIN,
                canvas.getY()
        );

        canvas.moveDown(22F);
    }

    private void writeContinuationHeader(
            PdfCanvas canvas,
            VehicleResponse vehicle
    ) throws IOException {

        canvas.text(
                "FleetTrack - Vehicle Maintenance Report",
                MARGIN,
                canvas.getY(),
                boldFont,
                12F
        );

        canvas.moveDown(18F);

        canvas.text(
                safeText(
                        "Vehicle "
                                + vehicle.id()
                                + " | "
                                + vehicle.licensePlate()
                                + " | "
                                + vehicle.vin()
                ),
                MARGIN,
                canvas.getY(),
                normalFont,
                NORMAL_SIZE
        );

        canvas.moveDown(24F);
    }

    private void writeVehicleLine(
            PdfCanvas canvas,
            String label,
            String value
    ) throws IOException {

        canvas.text(
                safeText(label + ":"),
                MARGIN,
                canvas.getY(),
                boldFont,
                NORMAL_SIZE
        );

        canvas.text(
                safeText(value),
                MARGIN + 120F,
                canvas.getY(),
                normalFont,
                NORMAL_SIZE
        );

        canvas.moveDown(16F);
    }

    private void writeTableHeader(
            PdfCanvas canvas
    ) throws IOException {

        String[] headers = {
                "ID",
                "Type",
                "Service Date",
                "Next Service",
                "Odometer",
                "Cost",
                "Status"
        };

        writeCells(
                canvas,
                headers,
                boldFont
        );
    }

    private void writeTableRow(
            PdfCanvas canvas,
            MaintenanceReportRow row
    ) throws IOException {

        String[] values = {
                String.valueOf(
                        row.maintenanceId()
                ),
                nullable(
                        row.maintenanceType()
                ),
                nullable(
                        row.serviceDate()
                ),
                nullable(
                        row.nextServiceDate()
                ),
                row.odometer() == null
                        ? "-"
                        : row.odometer().toString(),
                formatMoney(
                        row.cost()
                ),
                row.status() == null
                        ? "-"
                        : row.status().name()
        };

        writeCells(
                canvas,
                values,
                normalFont
        );
    }

    private void writeCells(
            PdfCanvas canvas,
            String[] values,
            PDType1Font font
    ) throws IOException {

        float rowTop =
                canvas.getY();

        float rowBottom =
                rowTop - ROW_HEIGHT;

        float x =
                MARGIN;

        for (
                int i = 0;
                i < COLUMN_WIDTHS.length;
                i++
        ) {
            float width =
                    COLUMN_WIDTHS[i];

            canvas.rectangle(
                    x,
                    rowBottom,
                    width,
                    ROW_HEIGHT
            );

            String text =
                    fitText(
                            safeText(
                                    values[i]
                            ),
                            font,
                            TABLE_SIZE,
                            width - 8F
                    );

            canvas.text(
                    text,
                    x + 4F,
                    rowBottom + 6F,
                    font,
                    TABLE_SIZE
            );

            x += width;
        }

        canvas.setY(
                rowBottom
        );
    }

    private void writeSummary(
            PdfCanvas canvas,
            List<MaintenanceReportRow> rows
    ) throws IOException {

        float requiredSpace = 85F;

        if (
                canvas.getY()
                        < MARGIN + requiredSpace
        ) {
            canvas.newPage();
        }

        canvas.moveDown(24F);

        BigDecimal totalCost =
                rows.stream()
                        .map(
                                MaintenanceReportRow::cost
                        )
                        .filter(
                                cost -> cost != null
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        )
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        canvas.text(
                "Summary",
                MARGIN,
                canvas.getY(),
                boldFont,
                11F
        );

        canvas.moveDown(18F);

        canvas.text(
                "Maintenance records: "
                        + rows.size(),
                MARGIN,
                canvas.getY(),
                normalFont,
                NORMAL_SIZE
        );

        canvas.moveDown(16F);

        canvas.text(
                "Total maintenance cost: "
                        + totalCost.toPlainString(),
                MARGIN,
                canvas.getY(),
                normalFont,
                NORMAL_SIZE
        );

        canvas.moveDown(16F);

        canvas.text(
                "Generated at: "
                        + Instant.now(),
                MARGIN,
                canvas.getY(),
                normalFont,
                NORMAL_SIZE
        );
    }

    private String formatMoney(
            BigDecimal value
    ) {
        if (value == null) {
            return "-";
        }

        return value
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                )
                .toPlainString();
    }

    private String nullable(
            Object value
    ) {
        return value == null
                ? "-"
                : value.toString();
    }

    private String fitText(
            String value,
            PDType1Font font,
            float fontSize,
            float maxWidth
    ) throws IOException {

        if (
                textWidth(
                        value,
                        font,
                        fontSize
                ) <= maxWidth
        ) {
            return value;
        }

        String suffix = "...";

        String result =
                value;

        while (
                !result.isEmpty()
                        && textWidth(
                        result + suffix,
                        font,
                        fontSize
                ) > maxWidth
        ) {
            result =
                    result.substring(
                            0,
                            result.length() - 1
                    );
        }

        return result + suffix;
    }

    private float textWidth(
            String text,
            PDType1Font font,
            float fontSize
    ) throws IOException {

        return font.getStringWidth(text)
                / 1000F
                * fontSize;
    }

    /*
     * Standard PDF Helvetica full Unicode font deyil.
     * Phase 15-də report-un generation prosesinin
     * platformdan asılı olmaması üçün common Azerbaijani
     * simvollarını ASCII counterpart-a çeviririk.
     *
     * Production hardening zamanı embedded Unicode font
     * əlavə edə bilərik.
     */
    private String safeText(
            String value
    ) {
        if (value == null) {
            return "-";
        }

        String normalized =
                value
                        .replace('Ə', 'E')
                        .replace('ə', 'e')
                        .replace('Ş', 'S')
                        .replace('ş', 's')
                        .replace('Ç', 'C')
                        .replace('ç', 'c')
                        .replace('Ğ', 'G')
                        .replace('ğ', 'g')
                        .replace('Ö', 'O')
                        .replace('ö', 'o')
                        .replace('Ü', 'U')
                        .replace('ü', 'u')
                        .replace('İ', 'I')
                        .replace('ı', 'i');

        StringBuilder result =
                new StringBuilder();

        for (
                char character
                : normalized.toCharArray()
        ) {
            if (
                    character >= 32
                            && character <= 126
            ) {
                result.append(
                        character
                );
            } else {
                result.append('?');
            }
        }

        return result.toString();
    }

    private static final class PdfCanvas
            implements AutoCloseable {

        private final PDDocument document;

        private PDPageContentStream contentStream;

        private float y;

        private PdfCanvas(
                PDDocument document
        ) {
            this.document =
                    document;
        }

        private void newPage()
                throws IOException {

            closeCurrentStream();

            PDPage page =
                    new PDPage(
                            PAGE_SIZE
                    );

            document.addPage(
                    page
            );

            contentStream =
                    new PDPageContentStream(
                            document,
                            page
                    );

            y =
                    PAGE_SIZE.getHeight()
                            - MARGIN;
        }

        private void text(
                String text,
                float x,
                float y,
                PDType1Font font,
                float size
        ) throws IOException {

            contentStream.beginText();

            contentStream.setFont(
                    font,
                    size
            );

            contentStream.newLineAtOffset(
                    x,
                    y
            );

            contentStream.showText(
                    text
            );

            contentStream.endText();
        }

        private void rectangle(
                float x,
                float y,
                float width,
                float height
        ) throws IOException {

            contentStream.addRect(
                    x,
                    y,
                    width,
                    height
            );

            contentStream.stroke();
        }

        private void horizontalLine(
                float fromX,
                float toX,
                float y
        ) throws IOException {

            contentStream.moveTo(
                    fromX,
                    y
            );

            contentStream.lineTo(
                    toX,
                    y
            );

            contentStream.stroke();
        }

        private void moveDown(
                float amount
        ) {
            y -= amount;
        }

        private float getY() {
            return y;
        }

        private void setY(
                float y
        ) {
            this.y =
                    y;
        }

        @Override
        public void close()
                throws IOException {

            closeCurrentStream();
        }

        private void closeCurrentStream()
                throws IOException {

            if (
                    contentStream != null
            ) {
                contentStream.close();

                contentStream = null;
            }
        }
    }
}