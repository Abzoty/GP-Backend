package com.gp.GP_backend.domain.user.controller;

import com.gp.GP_backend.domain.user.dto.UserResponse;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.service.UserService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;




@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;
    
    @GetMapping("/{id}/profile/view")
    public ResponseEntity<UserResponse> Viewprofile(@PathVariable Long id) {

        User user = userService.getUserById(id);

        if(user == null) {
            return ResponseEntity.notFound().build();
        }

        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setEmail(user.getEmail());
        userResponse.setFullName(user.getFullName());
        userResponse.setStudentId(user.getStudentId());
        userResponse.setAcademicYear(user.getAcademicYear());
        userResponse.setCurrentSemester(user.getCurrentSemester());
        userResponse.setGpa(user.getGpa());
        userResponse.setDepartment(user.getDepartment());
        userResponse.setImageUrl(user.getImageUrl());
        userResponse.setBio(user.getBio());

        return ResponseEntity.ok(userResponse);
    }
    
}