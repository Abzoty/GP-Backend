package com.gp.GP_backend.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.gp.GP_backend.domain.user.dto.RegisterRequest;
import com.gp.GP_backend.domain.user.entity.User;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();

        mapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT);

        // RegisterRequest → User
        // Skip password field — names differ and encoding is handled manually in
        // UserService
        mapper.typeMap(RegisterRequest.class, User.class)
                .addMappings(m -> m.skip(User::setPasswordHash));

        return mapper;
    }
}