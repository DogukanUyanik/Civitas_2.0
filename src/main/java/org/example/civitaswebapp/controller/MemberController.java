package org.example.civitaswebapp.controller;


import org.example.civitaswebapp.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/members")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @GetMapping
    public String showMembers(Model model) {
        model.addAttribute("members", memberService.getMembers());
        return "membersList";
    }


}
