package org.example.civitaswebapp.controller;


import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.dto.MemberDTO;
import org.example.civitaswebapp.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/members")
public class MemberRestController {

    @Autowired
    private MemberService memberService;

    @GetMapping
    public List<MemberDTO> getAllMembers() {
        return memberService.getAllMembers().stream()
                .map(member -> new MemberDTO(member.getId(), member.getFirstName(), member.getLastName()))
                .collect(Collectors.toList());
    }

}
