package com.gp.GP_backend.config;

import com.gp.GP_backend.domain.user.dto.RegisterRequest;
import com.gp.GP_backend.domain.user.entity.User;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicit type maps are defined here for cases where field names differ
 * or certain fields must be skipped (e.g. the password field).
 */
@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT);

        mapper.typeMap(RegisterRequest.class, User.class)
                .addMappings(m -> m.skip(User::setPasswordHash));

        return mapper;
    }
}
