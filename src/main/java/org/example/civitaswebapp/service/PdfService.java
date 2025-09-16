package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.Member;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public interface PdfService {
    byte[] generateMemberPdf(Member member, String memberStatusLabel, List<Map<String, Object>> transactions, Locale locale) throws Exception;
}
