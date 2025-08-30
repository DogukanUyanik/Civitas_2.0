package org.example.civitaswebapp.controller;


import jakarta.validation.Valid;
import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.service.MemberService;
import org.example.civitaswebapp.validator.MemberEmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/members")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberEmailValidator memberEmailValidator;

    @Autowired
    private MessageSource messageSource;

    @GetMapping
    public String showMembersList(Model model,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  @RequestParam(defaultValue = "") String search,
                                  @RequestParam(defaultValue = "") String status) {

        var memberPage = memberService.getMembers(PageRequest.of(page, size), search, status);
        model.addAttribute("members", memberPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", memberPage.getTotalPages());
        model.addAttribute("search", search);
        model.addAttribute("status", status); // <-- keep the status selected

        return "members/membersList";
    }


    @GetMapping("/add")
    public String showAddMemberForm(Model model) {
        model.addAttribute("member", new Member());
        return "members/memberForm";
    }

    @GetMapping("/edit/{id}")
    public String showEditMemberForm(@PathVariable Long id, Model model) {
        Member member = memberService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid id"));
        model.addAttribute("member", member);
        return "members/memberForm";
    }

    @PostMapping
    public String saveMember(@Valid @ModelAttribute("member") Member member,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        memberEmailValidator.validate(member, result);

        if (result.hasErrors()) {
            return "members/memberForm";
        }

        boolean isNew = member.getId() == null;

        memberService.saveMember(member);

        if (isNew) {
            String message = messageSource.getMessage(
                    "member.success.create",
                    new Object[]{member.getFirstName(), member.getLastName()},
                    LocaleContextHolder.getLocale()
            );
            redirectAttributes.addFlashAttribute("success", message);
        } else {
            String message = messageSource.getMessage(
                    "member.success.update",
                    new Object[]{member.getFirstName(), member.getLastName()},
                    LocaleContextHolder.getLocale()
            );
            redirectAttributes.addFlashAttribute("success", message);
        }

        return "redirect:/members";
    }

    @GetMapping("/view/{id}")
    public String showMemberDetails(@PathVariable Long id, Model model) {
        Member member = memberService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid id"));
        model.addAttribute("member", member);
        return "members/memberDetails";
    }



}
