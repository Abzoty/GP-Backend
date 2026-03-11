package com.gp.GP_backend.config;

import com.gp.GP_backend.domain.user.dto.RegisterRequest;
import com.gp.GP_backend.domain.user.entity.User;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the {@link ModelMapper} bean used for DTO ↔ Entity mapping.
 *
 * <p>
 * We use {@link MatchingStrategies#STRICT} to prevent accidental field
 * mappings (the default STANDARD strategy can match fields with similar names
 * even if types differ, leading to subtle bugs).
 *
 * <p>
 * Explicit type maps are defined here for cases where field names differ
 * or certain fields must be skipped (e.g. the password field).
 */
@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();

        // STRICT: only map fields where both name AND type match exactly
        mapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT);

        // RegisterRequest → User
        // Skip the passwordHash field — it is not present in RegisterRequest.
        // The raw password is encoded manually in UserService before being set.
        mapper.typeMap(RegisterRequest.class, User.class)
                .addMappings(m -> m.skip(User::setPasswordHash));

        // User → UserResponse
        // No explicit mapping needed: all UserResponse fields are present in User
        // with the same names and types (UUID id, String email, etc.).
        // ModelMapper STRICT finds them automatically.

        return mapper;
    }
}
