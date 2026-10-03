package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.MyUserRole;

import java.util.List;

public interface MyUserService {
    MyUser getLoggedInUser();
    MyUser findByUsername(String name);

    /** All users of the logged-in user's union, ordered by username. */
    List<MyUser> listUsersInMyUnion();

    /**
     * Changes the role of a user in the logged-in user's union. A target outside that union is
     * reported as not found. Refuses to demote the union's last ADMIN. The target's active
     * sessions are expired so the new role applies on their next request.
     */
    MyUser changeRole(Long targetUserId, MyUserRole newRole);

    /** Creates a user in the logged-in user's union; the password is BCrypt-encoded. */
    MyUser createUser(String username, String rawPassword, MyUserRole role);
}
