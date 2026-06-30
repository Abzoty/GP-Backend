package com.gp.GP_backend.domain.user.service;

import com.gp.GP_backend.domain.user.dto.RegisterRequest;
import com.gp.GP_backend.domain.user.dto.UpdateProfileRequest;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.repository.UserRepository;
import com.gp.GP_backend.shared.exception.ApiException;
import com.gp.GP_backend.shared.util.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    private RegisterRequest validRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Asran Test");
        request.setEmail("asran@test.com");
        request.setPassword("RawPass1!");
        request.setStudentId("12345678");
        request.setAcademicYear(2);
        request.setCurrentSemester(3);
        return request;
    }

    @Test
    void registerUserShouldHashPasswordAndNeverStoreRawPassword() {
        RegisterRequest request = validRequest();
        User mapped = User.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .studentId(request.getStudentId())
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByStudentId(request.getStudentId())).thenReturn(false);
        when(modelMapper.map(request, User.class)).thenReturn(mapped);
        when(passwordEncoder.encode("RawPass1!")).thenReturn("hashed-value");
        when(userRepository.save(mapped)).thenReturn(mapped);

        User saved = userService.registerUser(request);

        assertEquals("hashed-value", saved.getPasswordHash());
        assertEquals("RawPass1!", request.getPassword(), "sanity check: raw password is untouched on the DTO");
        verify(userRepository).save(mapped);
    }

    @Test
    void registerUserShouldRejectDuplicateEmailWithConflict() {
        RegisterRequest request = validRequest();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> userService.registerUser(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
    }

    @Test
    void registerUserShouldRejectDuplicateStudentIdWithConflict() {
        RegisterRequest request = validRequest();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByStudentId(request.getStudentId())).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> userService.registerUser(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
    }

    @Test
    void registerUserShouldNotFailRequestWhenWelcomeEmailThrows() {
        RegisterRequest request = validRequest();
        User mapped = User.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByStudentId(request.getStudentId())).thenReturn(false);
        when(modelMapper.map(request, User.class)).thenReturn(mapped);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-value");
        when(userRepository.save(mapped)).thenReturn(mapped);
        doThrow(new RuntimeException("SMTP down"))
                .when(emailService).sendWelcomeEmail(mapped.getEmail(), mapped.getFullName());

        User saved = userService.registerUser(request);

        // Registration must succeed even though the welcome email blew up.
        assertEquals(mapped, saved);
        verify(userRepository).save(mapped);
    }

    @Test
    void registerUserShouldMapDbRaceConditionToConflict() {
        RegisterRequest request = validRequest();
        User mapped = User.builder().email(request.getEmail()).build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByStudentId(request.getStudentId())).thenReturn(false);
        when(modelMapper.map(request, User.class)).thenReturn(mapped);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-value");
        when(userRepository.save(mapped))
                .thenThrow(new DataIntegrityViolationException("unique constraint violated"));

        ApiException ex = assertThrows(ApiException.class, () -> userService.registerUser(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
    }

    @Test
    void getUserByIdShouldThrow404WhenUserDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> userService.getUserById(id));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void updateProfileShouldOnlyApplyNonNullFields() {
        UUID id = UUID.randomUUID();
        User existing = User.builder()
                .id(id)
                .fullName("Old Name")
                .academicYear(1)
                .currentSemester(1)
                .department("CS")
                .bio("Old bio")
                .build();

        UpdateProfileRequest patch = new UpdateProfileRequest();
        patch.setFullName("New Name");
        // academicYear, currentSemester, department, imageUrl, bio left null on purpose

        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User updated = userService.updateProfile(id, patch);

        assertEquals("New Name", updated.getFullName());
        // Untouched fields must survive the partial patch unchanged.
        assertEquals(1, updated.getAcademicYear());
        assertEquals(1, updated.getCurrentSemester());
        assertEquals("CS", updated.getDepartment());
        assertEquals("Old bio", updated.getBio());
        verify(userRepository).save(existing);
    }
}