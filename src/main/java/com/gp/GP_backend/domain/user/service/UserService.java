package com.gp.GP_backend.domain.user.service;

import com.gp.GP_backend.domain.user.dto.RegisterRequest;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.repository.UserRepository;
import com.gp.GP_backend.shared.util.EmailService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Business logic for user account management.
 *
 * <p>
 * Responsibilities:
 * <ul>
 * <li>Registration with uniqueness validation and password hashing.</li>
 * <li>User lookup by ID or email (used by controllers and security).</li>
 * </ul>
 *
 * <p>
 * Password encoding is intentionally NOT handled in
 * {@link com.gp.GP_backend.config.ModelMapperConfig}
 * because it is a business operation, not a data-mapping concern.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final EmailService emailService;

    // ── Registration ───────────────────────────────────────────────────────────

    /**
     * Registers a new user account.
     *
     * <p>
     * Steps:
     * <ol>
     * <li>Validate email and studentId uniqueness.</li>
     * <li>Map {@link RegisterRequest} → {@link User} (ModelMapper skips
     * passwordHash).</li>
     * <li>Encode and set the password hash.</li>
     * <li>Persist and asynchronously send a welcome email.</li>
     * </ol>
     *
     * @param request validated registration data from the HTTP body
     * @return the saved {@link User} entity (with generated UUID id)
     * @throws IllegalArgumentException if the email or student ID is already taken
     */
    @Transactional
    public User registerUser(RegisterRequest request) {
        // Uniqueness guards — throw early with a clear message before touching the DB
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if (request.getStudentId() != null && !request.getStudentId().isBlank() &&
                userRepository.existsByStudentId(request.getStudentId())) {
            throw new IllegalArgumentException("Student ID is already registered");
        }

        // Map DTO → entity (ModelMapper config skips passwordHash to prevent null/raw
        // value)
        User user = modelMapper.map(request, User.class);

        // Hash the raw password — never store plaintext
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User saved = userRepository.save(user);

        // Fire-and-forget welcome email on a separate thread (@Async in EmailService)
        emailService.sendWelcomeEmail(saved.getEmail(), saved.getFullName());

        return saved;
    }

    // ── Lookups ────────────────────────────────────────────────────────────────

    /**
     * Fetches a user by their UUID primary key.
     *
     * @throws IllegalArgumentException if no user with the given ID exists
     */
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    /**
     * Fetches a user by their email address.
     * Used by {@link UserController} to serve profile requests.
     *
     * @throws IllegalArgumentException if no user with the given email exists
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }
}