package com.gp.GP_backend.domain.user.controller;

import com.gp.GP_backend.domain.user.dto.UserResponse;
import com.gp.GP_backend.domain.user.service.UserService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;


    @PostMapping("/signup")
    public ResponseEntity<UserResponse> Signup(@RequestBody UserResponse entity) {
        UserResponse savedUser = userService.signup(entity);
        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }
    

    @GetMapping("/{id}/profile/view")
    public ResponseEntity<UserResponse> Viewprofile(@PathVariable("id") Long id) {

        UserResponse userResponse = userService.getuserById(id);

        if(userResponse == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(userResponse);
    }
    
}