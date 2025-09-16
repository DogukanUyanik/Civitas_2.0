package org.example.civitaswebapp.controller;


import jakarta.validation.Valid;
import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.service.MemberService;
import org.example.civitaswebapp.service.PdfService;
import org.example.civitaswebapp.service.PdfServiceImpl;
import org.example.civitaswebapp.validator.MemberEmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.context.Context;


import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/members")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberEmailValidator memberEmailValidator;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private PdfService pdfService;

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

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> generatePdf(@PathVariable Long id) throws Exception {
        Member member = memberService.getIdForPdf(id);
        Locale currentLocale = LocaleContextHolder.getLocale();

        // Resolve localized member status
        String memberStatusLabel = messageSource.getMessage(
                "member.status." + member.getMemberStatus().name(),
                null,
                currentLocale
        );

        // Format and localize transactions
        List<Map<String, Object>> txList = member.getTransactions().stream().map(tx -> {
            Map<String, Object> map = new HashMap<>();
            map.put("date", DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .format(tx.getCreatedAt()));
            map.put("typeLabel", messageSource.getMessage(
                    "transaction.type." + tx.getType().name(),
                    null, currentLocale));
            map.put("amount", tx.getAmount());
            map.put("currency", tx.getCurrency());
            map.put("statusLabel", messageSource.getMessage(
                    "transaction.status." + tx.getStatus().name(),
                    null, currentLocale));
            return map;
        }).toList();

        // Pass locale to PDF service
        byte[] pdfBytes = pdfService.generateMemberPdf(member, memberStatusLabel, txList, currentLocale);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "member_" + id + ".pdf");

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }


}
