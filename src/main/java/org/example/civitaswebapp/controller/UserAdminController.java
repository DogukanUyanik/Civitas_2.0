package org.example.civitaswebapp.controller;

import jakarta.validation.Valid;
import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.MyUserRole;
import org.example.civitaswebapp.dto.user.CreateUserForm;
import org.example.civitaswebapp.dto.user.UserSummaryDto;
import org.example.civitaswebapp.exceptions.UserManagementException;
import org.example.civitaswebapp.service.MyUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Lets an ADMIN list the users of their own union, change their roles and add new users. The
 * class-level {@code @PreAuthorize} covers every handler here (role); union scoping is enforced
 * manually in {@link MyUserService}.
 */
@Controller
@RequestMapping("/settings/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {

    @Autowired
    private MyUserService myUserService;

    @Autowired
    private MessageSource messageSource;

    @GetMapping
    public String showUsers(Model model) {
        if (!model.containsAttribute("createUserForm")) {
            model.addAttribute("createUserForm", new CreateUserForm());
        }
        populateUsers(model);
        return "settings/users";
    }

    @PostMapping
    public String createUser(@Valid @ModelAttribute("createUserForm") CreateUserForm form,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            populateUsers(model);
            return "settings/users";
        }

        try {
            MyUser created = myUserService.createUser(form.getUsername(), form.getPassword(), form.getRole());
            redirectAttributes.addFlashAttribute("success",
                    message("users.success.created", created.getUsername(), roleLabel(created.getRole())));
            return "redirect:/settings/users";
        } catch (UserManagementException e) {
            model.addAttribute("error", message(e.getMessageKey()));
            populateUsers(model);
            return "settings/users";
        }
    }

    @PostMapping("/{id}/role")
    public String changeRole(@PathVariable Long id,
                             @RequestParam MyUserRole role,
                             RedirectAttributes redirectAttributes) {
        try {
            MyUser updated = myUserService.changeRole(id, role);
            redirectAttributes.addFlashAttribute("success",
                    message("users.success.roleChanged", updated.getUsername(), roleLabel(updated.getRole())));
        } catch (UserManagementException e) {
            redirectAttributes.addFlashAttribute("error", message(e.getMessageKey()));
        }
        return "redirect:/settings/users";
    }

    private void populateUsers(Model model) {
        model.addAttribute("users", myUserService.listUsersInMyUnion().stream()
                .map(UserSummaryDto::from)
                .toList());
        model.addAttribute("roles", MyUserRole.values());
        model.addAttribute("currentUserId", myUserService.getLoggedInUser().getId());
    }

    private String roleLabel(MyUserRole role) {
        return message("users.role." + role.name());
    }

    private String message(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }
}
