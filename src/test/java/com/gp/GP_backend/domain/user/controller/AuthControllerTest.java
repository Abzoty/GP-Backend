package com.gp.GP_backend.domain.user.controller;

import com.gp.GP_backend.domain.user.dto.AuthResponse;
import com.gp.GP_backend.domain.user.dto.LoginRequest;
import com.gp.GP_backend.domain.user.dto.UserResponse;
import com.gp.GP_backend.domain.user.entity.RefreshToken;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.service.GamificationService;
import com.gp.GP_backend.domain.user.service.PasswordResetService;
import com.gp.GP_backend.domain.user.service.RefreshTokenService;
import com.gp.GP_backend.domain.user.service.UserService;
import com.gp.GP_backend.security.JwtTokenProvider;
import com.gp.GP_backend.shared.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PasswordResetService passwordResetService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private GamificationService gamificationService;

    @InjectMocks
    private AuthController controller;

    @Test
    void loginShouldReturnJwtTokensAndMappedUser() {
        LoginRequest request = new LoginRequest();
        request.setEmail("student@example.com");
        request.setPassword("Password123!");
        User user = user(UUID.randomUUID());
        Authentication authentication = authentication(user);
        RefreshToken refreshToken = RefreshToken.builder().token("refresh-token").user(user).build();
        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setEmail(user.getEmail());
        userResponse.setFullName(user.getFullName());

        when(authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateToken(user)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);
        when(modelMapper.map(user, UserResponse.class)).thenReturn(userResponse);

        ResponseEntity<ApiResponse<AuthResponse>> entity = controller.login(request);

        assertThat(entity.getStatusCode().value()).isEqualTo(200);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getData().getToken()).isEqualTo("access-token");
        assertThat(entity.getBody().getData().getRefreshToken()).isEqualTo("refresh-token");
        assertThat(entity.getBody().getData().getUser().getEmail()).isEqualTo("student@example.com");

        verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        verify(jwtTokenProvider).generateToken(user);
        verify(refreshTokenService).createRefreshToken(user);
        verify(gamificationService).trackDailyLogin(user.getId());
    }

    @Test
    void loginShouldStillSucceedWhenGamificationFails() {
        LoginRequest request = new LoginRequest();
        request.setEmail("student@example.com");
        request.setPassword("Password123!");
        User user = user(UUID.randomUUID());
        Authentication authentication = authentication(user);
        RefreshToken refreshToken = RefreshToken.builder().token("refresh-token").user(user).build();
        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setEmail(user.getEmail());
        userResponse.setFullName(user.getFullName());

        when(authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateToken(user)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);
        when(modelMapper.map(user, UserResponse.class)).thenReturn(userResponse);
        doThrow(new RuntimeException("boom")).when(gamificationService).trackDailyLogin(user.getId());

        ResponseEntity<ApiResponse<AuthResponse>> entity = controller.login(request);

        assertThat(entity.getStatusCode().value()).isEqualTo(200);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getData().getToken()).isEqualTo("access-token");
    }

    private static Authentication authentication(User user) {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        return authentication;
    }

    private static User user(UUID id) {
        return User.builder()
                .id(id)
                .email("student@example.com")
                .passwordHash("hash")
                .fullName("Student Name")
                .build();
    }
}