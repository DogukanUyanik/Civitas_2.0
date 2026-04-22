package org.example.civitaswebapp.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MemberStatus;
import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.Union;
import org.example.civitaswebapp.dto.member.BulkImportResultDto;
import org.example.civitaswebapp.dto.member.MemberSavedEventDto;
import org.example.civitaswebapp.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MemberServiceImpl implements MemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberServiceImpl.class);
    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;



    private Union getCurrentUserUnion() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof MyUser) {
            return ((MyUser) principal).getUnion();
        }
        throw new RuntimeException("No user logged in or user is not of type MyUser");
    }

    @Override
    public Page<Member> getMembers(Pageable pageable) {
        return memberRepository.findAllByUnion(getCurrentUserUnion(), pageable);
    }




    @Override
    public Page<Member> getMembers(Pageable pageable, String search, String status) {
        Union currentUnion = getCurrentUserUnion();
        boolean hasSearch = (search != null && !search.isBlank());
        boolean hasStatus = (status != null && !status.isBlank());

        if (!hasSearch && !hasStatus) {
            // Case 1: Show All (Scoped to Union)
            return memberRepository.findAllByUnion(currentUnion, pageable);

        } else if (hasStatus && !hasSearch) {
            // Case 2: Filter by Status Only (Scoped to Union)
            return memberRepository.findByMemberStatusAndUnion(
                    MemberStatus.valueOf(status), currentUnion, pageable);

        } else if (!hasStatus && hasSearch) {
            // Case 3: Search Text Only (Scoped to Union)
            return memberRepository.searchByUnion(currentUnion, search, pageable);

        } else {
            // Case 4: Search + Status (Scoped to Union)
            return memberRepository.searchByStatusAndUnion(
                    currentUnion, MemberStatus.valueOf(status), search, pageable);
        }
    }

    @Transactional
    @Override
    public void saveMember(Member member, MyUser createdByUser) {
        member.setUnion(createdByUser.getUnion());

        boolean exists = memberRepository.existsByEmailAndUnionAndIdNot(
                member.getEmail(),
                createdByUser.getUnion(),
                member.getId() == null ? -1L : member.getId()
        );

        if (exists) {
            throw new IllegalArgumentException("Email already exists in this Union.");
        }

        boolean isNew = member.getId() == null;
        memberRepository.save(member);

        var dto = new MemberSavedEventDto(
                member.getId(),
                member.getFirstName(),
                member.getLastName(),
                createdByUser.getId(),
                isNew
        );
        eventPublisher.publishEvent(dto);
    }

    @Override
    public void deleteMember(Member member) {
        // 🔒 SECURE: Check before delete
        verifyMemberBelongsToUnion(member);
        memberRepository.delete(member);
    }

    @Override
    public Optional<Member> findById(Long id) {
        Optional<Member> memberOpt = memberRepository.findById(id);

        // 🔒 SECURE: If found, check if it belongs to us. If not, return Empty.
        if (memberOpt.isPresent()) {
            try {
                verifyMemberBelongsToUnion(memberOpt.get());
            } catch (AccessDeniedException e) {
                return Optional.empty(); // Treat "Unauthorized" as "Not Found" to hide data
            }
        }
        return memberOpt;
    }

    @Override
    public Member getIdForPdf(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        verifyMemberBelongsToUnion(member);
        return member;
    }

    @Override
    public List<Member> getAllMembers() {
        return memberRepository.findAllByUnion(getCurrentUserUnion());
    }

    @Override
    @Transactional
    public BulkImportResultDto bulkImport(MultipartFile file, MyUser currentUser) {
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Only .xlsx files are supported.");
        }

        Union union = currentUser.getUnion();
        List<Member> validMembers = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int skippedCount = 0;

        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            var sheet = workbook.getSheetAt(0);

            // --- Header mapping ---
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return new BulkImportResultDto(0, 0, List.of("The file is empty or has no header row."));
            }

            int colFirstName   = -1, colLastName  = -1, colEmail    = -1;
            int colPhone       = -1, colAddress   = -1, colDob      = -1, colStatus = -1;

            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                String header = cellAsString(headerRow.getCell(c)).toLowerCase().trim();
                if (matches(header, "voornaam", "first name", "firstname", "ad"))                       colFirstName = c;
                else if (matches(header, "achternaam", "last name", "lastname", "soyad"))               colLastName  = c;
                else if (matches(header, "email", "e-mail", "mail"))                                    colEmail     = c;
                else if (matches(header, "telefoon", "phone", "gsm", "telefon"))                        colPhone     = c;
                else if (matches(header, "adres", "address", "street"))                                 colAddress   = c;
                else if (matches(header, "geboortedatum", "birthday", "birth", "doğum"))                colDob       = c;
                else if (matches(header, "status"))                                                     colStatus    = c;
            }

            // Log the discovered column map for debugging
            Map<String, Integer> colMap = new LinkedHashMap<>();
            colMap.put("firstName", colFirstName);
            colMap.put("lastName",  colLastName);
            colMap.put("email",     colEmail);
            colMap.put("phone",     colPhone);
            colMap.put("address",   colAddress);
            colMap.put("dob",       colDob);
            colMap.put("status",    colStatus);
            log.info("Detected columns: {}", colMap);

            // Validate required headers
            List<String> missingHeaders = new ArrayList<>();
            if (colFirstName == -1) missingHeaders.add("firstName  — accepted: voornaam / first name / firstname / ad");
            if (colLastName  == -1) missingHeaders.add("lastName   — accepted: achternaam / last name / lastname / soyad");
            if (colEmail     == -1) missingHeaders.add("email      — accepted: email / e-mail / mail");
            if (colPhone     == -1) missingHeaders.add("phone      — accepted: telefoon / phone / gsm / telefon");
            if (colAddress   == -1) missingHeaders.add("address    — accepted: adres / address / street");
            if (!missingHeaders.isEmpty()) {
                List<String> result = new ArrayList<>();
                result.add("Missing required column(s):");
                result.addAll(missingHeaders);
                return new BulkImportResultDto(0, 0, result);
            }

            // --- Data rows ---
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String firstName   = cellAsString(row.getCell(colFirstName));
                String lastName    = cellAsString(row.getCell(colLastName));
                String email       = cellAsString(row.getCell(colEmail));
                String phoneNumber = colPhone   >= 0 ? cellAsString(row.getCell(colPhone))   : "";
                String address     = colAddress >= 0 ? cellAsString(row.getCell(colAddress)) : "";
                String dobRaw      = colDob     >= 0 ? cellAsString(row.getCell(colDob))     : "";
                String statusRaw   = colStatus  >= 0 ? cellAsString(row.getCell(colStatus))  : "";

                // Skip completely empty rows
                if (firstName.isBlank() && lastName.isBlank() && email.isBlank()) continue;

                log.debug("Row {}: firstName='{}' lastName='{}' email='{}' phone='{}' address='{}' dob='{}' status='{}'",
                        i + 1, firstName, lastName, email, phoneNumber, address, dobRaw, statusRaw);

                // Validate required fields
                if (firstName.isBlank() || lastName.isBlank()) {
                    log.warn("Row {} skipped — missing name. Raw: firstName='{}' lastName='{}'", i + 1, firstName, lastName);
                    errors.add("Row " + (i + 1) + ": First and last name are required.");
                    skippedCount++;
                    continue;
                }
                if (email.isBlank() || !email.contains("@")) {
                    log.warn("Row {} skipped — invalid email. Raw: email='{}'", i + 1, email);
                    errors.add("Row " + (i + 1) + ": Valid email is required.");
                    skippedCount++;
                    continue;
                }
                if (phoneNumber.isBlank()) {
                    log.warn("Row {} skipped — missing phone. Raw: phone='{}'", i + 1, phoneNumber);
                    errors.add("Row " + (i + 1) + ": Phone number is required.");
                    skippedCount++;
                    continue;
                }
                if (address.isBlank()) {
                    log.warn("Row {} skipped — missing address. Raw: address='{}' (colAddress={})", i + 1, address, colAddress);
                    errors.add("Row " + (i + 1) + ": Address is required.");
                    skippedCount++;
                    continue;
                }

                // Check duplicate email within this union
                if (memberRepository.existsByEmailAndUnionAndIdNot(email, union, -1L)) {
                    errors.add("Row " + (i + 1) + ": Email '" + email + "' already exists in this union.");
                    skippedCount++;
                    continue;
                }

                // Parse optional fields — silently fall back on bad input
                LocalDate dateOfBirth = null;
                if (!dobRaw.isBlank()) {
                    try { dateOfBirth = LocalDate.parse(dobRaw); } catch (DateTimeParseException ignored) {}
                }

                MemberStatus status = MemberStatus.ACTIVE;
                if (!statusRaw.isBlank()) {
                    try { status = MemberStatus.valueOf(statusRaw.toUpperCase()); } catch (IllegalArgumentException ignored) {}
                }

                validMembers.add(Member.builder()
                        .firstName(firstName)
                        .lastName(lastName)
                        .email(email)
                        .phoneNumber(phoneNumber)
                        .address(address)
                        .dateOfBirth(dateOfBirth)
                        .memberStatus(status)
                        .union(union)
                        .build());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse Excel file: " + e.getMessage(), e);
        }

        memberRepository.saveAll(validMembers);
        return new BulkImportResultDto(validMembers.size(), skippedCount, errors);
    }

    private boolean matches(String header, String... keywords) {
        for (String kw : keywords) {
            if (header.equals(kw)) return true;
        }
        return false;
    }

    private String cellAsString(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.BLANK) return "";
        // DataFormatter respects the cell's format mask, so dates stay as dates,
        // phone numbers stay as text, and we always get a clean string back.
        return DATA_FORMATTER.formatCellValue(cell).trim();
    }

    private void verifyMemberBelongsToUnion(Member member) {
        Union currentUnion = getCurrentUserUnion();
        if (!member.getUnion().getId().equals(currentUnion.getId())) {
            throw new AccessDeniedException("ACCESS DENIED: You do not have permission to view/edit this member.");
        }
    }
}