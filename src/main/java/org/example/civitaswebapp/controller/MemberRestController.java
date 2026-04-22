package org.example.civitaswebapp.controller;


import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.dto.member.BulkImportResultDto;
import org.example.civitaswebapp.dto.member.MemberDto;
import org.example.civitaswebapp.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/members")
public class MemberRestController {

    @Autowired
    private MemberService memberService;

    @GetMapping
    public List<MemberDto> getAllMembers() {
        return memberService.getAllMembers().stream()
                .map(member -> new MemberDto(member.getId(), member.getFirstName(), member.getLastName()))
                .collect(Collectors.toList());
    }

    @PostMapping("/import")
    public ResponseEntity<BulkImportResultDto> importMembers(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal MyUser currentUser) {
        BulkImportResultDto result = memberService.bulkImport(file, currentUser);
        return ResponseEntity.ok(result);
    }
}
