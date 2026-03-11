package com.gp.GP_backend.domain.user.controller;

import com.gp.GP_backend.domain.user.dto.UserResponse;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.service.UserService;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ModelMapper modelMapper;

    @GetMapping("/profile/view")
    public ResponseEntity<UserResponse> Viewprofile(@AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();

        User user = userService.getUserByEmail(email);
        UserResponse userResponse = modelMapper.map(user, UserResponse.class);

        return ResponseEntity.ok(userResponse);
    }
}