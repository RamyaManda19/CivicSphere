package com.civicsphere.service;

import com.civicsphere.dao.UserDAO;
import com.civicsphere.model.User;
import com.civicsphere.util.PasswordUtil;

import java.util.regex.Pattern;

/**
 * Service handling user registration, authentication, and credential verification.
 */
public class AuthService {

    private final UserDAO userDAO;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Registers a new citizen.
     */
    public User registerCitizen(String fullName, String email, String phone, String password, String confirmPassword, String address) throws Exception {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("A valid email address is required.");
        }
        if (phone == null || phone.trim().length() < 10) {
            throw new IllegalArgumentException("A valid phone number (at least 10 digits) is required.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        if (userDAO.emailExists(email.trim())) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        String hash = PasswordUtil.hashPassword(password);
        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setPhone(phone.trim());
        user.setPasswordHash(hash);
        user.setAddress(address != null ? address.trim() : "");
        user.setRole("CITIZEN");

        boolean created = userDAO.createUser(user);
        if (!created) {
            throw new RuntimeException("Failed to register user due to a database error.");
        }

        return user;
    }

    /**
     * Authenticates a user with email and password.
     */
    public User authenticate(String email, String password) throws Exception {
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Email and password are required.");
        }

        User user = userDAO.findByEmail(email.trim());
        if (user == null) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        return user;
    }

    public User getUserById(int userId) {
        return userDAO.findById(userId);
    }
}

