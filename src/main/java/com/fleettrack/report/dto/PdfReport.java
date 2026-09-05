package com.fleettrack.report.dto;

public record PdfReport(

        String fileName,

        byte[] content
) {
}