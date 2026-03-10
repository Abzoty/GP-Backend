package com.gp.GP_backend.domain.user.service;

import org.modelmapper.ModelMapper;
import com.gp.GP_backend.domain.user.dto.RegisterRequest;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    @Transactional
    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }
        if (request.getStudentId() != null &&
                userRepository.existsByStudentId(request.getStudentId())) {
            throw new IllegalArgumentException("Student ID already registered");
        }

        User user = modelMapper.map(request, User.class);

        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        // User user = User.builder()
        //         .fullName(request.getFullName())
        //         .email(request.getEmail())
        //         .passwordHash(passwordEncoder.encode(request.getPassword()))
        //         .studentId(request.getStudentId())
        //         .academicYear(request.getAcademicYear())
        //         .currentSemester(request.getCurrentSemester())
        //         .build();

        return userRepository.save(user);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
    }
}