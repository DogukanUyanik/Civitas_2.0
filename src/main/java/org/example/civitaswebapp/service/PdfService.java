package org.example.civitaswebapp.service;

public interface PdfService {
    byte[] generateMemberPdf(Object member) throws Exception;
}
