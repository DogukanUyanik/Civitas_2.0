package org.example.civitaswebapp.dto.user;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.MyUserRole;

/**
 * Row shown on the user-management screen. Deliberately excludes the password hash and the
 * entity's lazy collections so the view never touches them.
 */
public record UserSummaryDto(Long id, String username, MyUserRole role) {

    public static UserSummaryDto from(MyUser user) {
        return new UserSummaryDto(user.getId(), user.getUsername(), user.getRole());
    }
}
