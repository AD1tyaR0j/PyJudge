package com.pyjudge.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import com.pyjudge.dao.UserDao;
import com.pyjudge.model.Role;
import com.pyjudge.model.User;

/** Account creation, login, profile changes, and the validation rules shared by all of them. */
public class AuthService {

    public static final int MIN_PASSWORD = 8;
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,30}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserDao users;
    private final PasswordHasher hasher;
    private final SettingsService settings;

    public AuthService(UserDao users, PasswordHasher hasher, SettingsService settings) {
        this.users = users;
        this.hasher = hasher;
        this.settings = settings;
    }

    /** Self sign-up: always creates a student. */
    public User register(String username, String name, String email, String password, String confirm) {
        if (!settings.flag(Setting.REGISTRATION_OPEN)) {
            throw new ForbiddenException("Sign-ups are closed. Ask an administrator for an account.");
        }
        List<String> errors = validate(username, name, email, 0L);
        validatePassword(password, confirm, errors);
        ValidationException.throwIfAny(errors);
        return create(username, name, email, password, Role.STUDENT);
    }

    /** Admin-created account with any role. */
    public User createUser(String username, String name, String email, String password, Role role) {
        List<String> errors = validate(username, name, email, 0L);
        validatePassword(password, null, errors);
        ValidationException.throwIfAny(errors);
        return create(username, name, email, password, role);
    }

    private User create(String username, String name, String email, String password, Role role) {
        User u = User.forRole(role);
        u.setUsername(username.trim());
        u.setName(name.trim());
        u.setEmail(email.trim().toLowerCase());
        u.setPasswordHash(hasher.hash(password));
        return users.save(u);
    }

    /** Returns the user if the credentials match. Blocked accounts are rejected with a clear message. */
    public User authenticate(String identifier, String password) {
        Optional<User> found = identifier == null ? Optional.empty() : users.findByLogin(identifier.trim());
        if (found.isEmpty() || !hasher.verify(password, found.get().getPasswordHash())) {
            throw new ValidationException("Invalid username/email or password.");
        }
        if (found.get().isBlocked()) {
            throw new ForbiddenException("This account has been blocked. Contact an administrator.");
        }
        return found.get();
    }

    public void updateProfile(User user, String name, String email, String bio) {
        List<String> errors = validate(null, name, email, user.getId());
        ValidationException.throwIfAny(errors);
        user.setName(name.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setBio(bio == null ? "" : bio.trim().substring(0, Math.min(bio.trim().length(), 1000)));
        users.save(user);
    }

    public void changePassword(User user, String current, String next, String confirm) {
        if (!hasher.verify(current, user.getPasswordHash())) {
            throw new ValidationException("Your current password is incorrect.");
        }
        List<String> errors = new ArrayList<>();
        validatePassword(next, confirm, errors);
        ValidationException.throwIfAny(errors);
        user.setPasswordHash(hasher.hash(next));
        users.save(user);
    }

    /** Admin edit of another account. Password is optional (blank keeps the old one). */
    public void adminUpdate(User target, String name, String email, Role role, boolean blocked, String password, User actor) {
        List<String> errors = validate(null, name, email, target.getId());
        if (password != null && !password.isEmpty()) {
            validatePassword(password, null, errors);
        }
        if (target.getId() == actor.getId() && (role != Role.ADMIN || blocked)) {
            errors.add("You can't remove your own admin access or block yourself.");
        }
        ValidationException.throwIfAny(errors);
        target.setName(name.trim());
        target.setEmail(email.trim().toLowerCase());
        target.setBlocked(blocked);
        if (password != null && !password.isEmpty()) {
            target.setPasswordHash(hasher.hash(password));
        }
        users.save(target);
        if (role != target.getRole()) {
            users.updateRole(target.getId(), role);
        }
    }

    public boolean verifyPassword(User user, String password) {
        return hasher.verify(password, user.getPasswordHash());
    }

    private List<String> validate(String username, String name, String email, long exceptId) {
        List<String> errors = new ArrayList<>();
        if (username != null) {
            if (!USERNAME.matcher(username.trim()).matches()) {
                errors.add("Username must be 3–30 characters: letters, numbers, or underscores.");
            } else if (users.usernameTaken(username.trim(), exceptId)) {
                errors.add("That username is taken.");
            }
        }
        if (name == null || name.isBlank() || name.trim().length() > 100) {
            errors.add("Enter a name (up to 100 characters).");
        }
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            errors.add("Enter a valid email address.");
        } else if (users.emailTaken(email.trim(), exceptId)) {
            errors.add("An account with that email already exists.");
        }
        return errors;
    }

    private static void validatePassword(String password, String confirm, List<String> errors) {
        if (password == null || password.length() < MIN_PASSWORD) {
            errors.add("Password must be at least " + MIN_PASSWORD + " characters.");
        } else if (confirm != null && !password.equals(confirm)) {
            errors.add("Passwords do not match.");
        }
    }
}
