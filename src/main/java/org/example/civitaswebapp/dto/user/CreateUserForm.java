package org.example.civitaswebapp.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.example.civitaswebapp.domain.MyUserRole;

/**
 * Form backing the "Add user" panel. The union is never part of the form: it is always stamped
 * from the logged-in ADMIN in the service layer.
 */
@Data
public class CreateUserForm {

    @NotBlank(message = "{users.form.username.required}")
    @Size(min = 3, max = 50, message = "{users.form.username.size}")
    private String username;

    // BCrypt only uses the first 72 bytes, so longer passwords would silently be truncated.
    @NotBlank(message = "{users.form.password.required}")
    @Size(min = 8, max = 72, message = "{users.form.password.size}")
    private String password;

    @NotNull(message = "{users.form.role.required}")
    private MyUserRole role = MyUserRole.VIEWER;
}
