package com.gp.GP_backend.domain.user.service;

import com.gp.GP_backend.domain.notification.entity.NotificationPreference;
import com.gp.GP_backend.domain.user.dto.RegisterRequest;
import com.gp.GP_backend.domain.user.dto.UpdateProfileRequest;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.repository.UserRepository;
import com.gp.GP_backend.domain.notification.repository.NotificationPreferencesRepository;
import com.gp.GP_backend.shared.exception.ApiException;
import com.gp.GP_backend.shared.util.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Handles user account operations: registration, profile lookup, and profile
 * updates.
 *
 * <p>
 * All write operations are {@code @Transactional} to ensure atomicity.
 * Read operations use {@code readOnly = true} to enable DB-level query
 * optimisations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final NotificationPreferencesRepository notificationPreferencesRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final EmailService emailService;

    /**
     * Registers a new user account.
     *
     * <p>
     * Steps:
     * <ol>
     * <li>Validate email and studentId uniqueness.</li>
     * <li>Map the DTO to a {@link User} entity (password field skipped by
     * ModelMapper config).</li>
     * <li>Encode the raw password with BCrypt and set it on the entity.</li>
     * <li>Persist the user.</li>
     * <li>Send a welcome email asynchronously (non-blocking).</li>
     * </ol>
     *
     * @throws ApiException with 409 CONFLICT if email or studentId is already in
     *                      use.
     */
    @Transactional
    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered");
        }
        if (request.getStudentId() != null && userRepository.existsByStudentId(request.getStudentId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Student ID is already registered");
        }

        // Map all matching fields; passwordHash is skipped (configured in
        // ModelMapperConfig)
        User user = modelMapper.map(request, User.class);

        // Encode the raw password — never store plain text
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User saved;
        try {
            saved = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Email or Student ID is already registered");
        }

        // Fire-and-forget email; failure is logged but does not fail the request
        try {
            emailService.sendWelcomeEmail(saved.getEmail(), saved.getFullName());
        } catch (Exception ex) {
            log.warn("Welcome email failed for user {}", saved.getId(), ex);
        }

        NotificationPreference preference = NotificationPreference.builder()
                .user(user)
                .inApp(true)
                .email(true)
                .build();

        notificationPreferencesRepository.save(preference);

        return saved;
    }

    /**
     * update the user profile using the applyPatch method to account for partial
     * updates.
     */
    @Transactional
    public User updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = getUserById(userId);
        user.applyPatch(request);
        return userRepository.save(user);
    }

    /** @throws ApiException 404 if no user with the given UUID exists. */
    @Transactional(readOnly = true)
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "User not found with id: " + id));
    }

    /** @throws ApiException 404 if no user with the given email exists. */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "User not found with email: " + email));
    }
}
