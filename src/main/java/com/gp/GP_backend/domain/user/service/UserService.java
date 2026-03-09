package com.gp.GP_backend.domain.user.service;

import com.gp.GP_backend.shared.util.EmailService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.gp.GP_backend.domain.user.repository.UserRepository;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import  com.gp.GP_backend.domain.user.dto.*;
import com.gp.GP_backend.domain.user.entity.User;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmailService emailService;

    @Autowired
    private ModelMapper modelMapper;

    public UserResponse getuserById(Long id) {
        User user  =  userRepository.findById(id).orElse(null);

        if (user == null) {
            return null;
        }
        return modelMapper.map(user, UserResponse.class);

    }

    public UserResponse signup(UserResponse entity) {
        User user = modelMapper.map(entity, User.class);
        User savedUser = userRepository.save(user);

        emailService.sendWelecomeEmail(user.getEmail());

        return modelMapper.map(savedUser, UserResponse.class);

    }
}