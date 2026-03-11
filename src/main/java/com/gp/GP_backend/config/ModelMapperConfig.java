package com.gp.GP_backend.config;

import com.gp.GP_backend.domain.user.dto.RegisterRequest;
import com.gp.GP_backend.domain.user.dto.UserResponse;
import com.gp.GP_backend.domain.user.entity.User;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

/**
 * Configures the {@link ModelMapper} bean used throughout the application for
 * DTO <=> Entity conversions.
 *
 * <h3>Strategy</h3>
 * {@link MatchingStrategies#STRICT} is used so that fields are only mapped when
 * their names match exactly (no fuzzy/partial matching). This prevents
 * accidental
 * mapping of unrelated fields and makes the configuration explicit and
 * predictable.
 *
 * <h3>Custom mappings</h3>
 * <ul>
 * <li>{@link RegisterRequest} to {@link User}: passwordHash is skipped because
 * the field names differ (password vs passwordHash) AND encoding is handled
 * in UserService, not here.</li>
 * <li>{@link User} to {@link UserResponse}: id (UUID) is converted to String
 * via a typed Converter. A plain lambda like src -> src.getId().toString()
 * does NOT work -- ModelMapper uses bytecode proxying to record property access
 * and calling .toString() on the proxy throws an ErrorsException at
 * startup.</li>
 * </ul>
 */
@Configuration
public class ModelMapperConfig {

        @Bean
        public ModelMapper modelMapper() {
                ModelMapper mapper = new ModelMapper();

                // Strict: only map fields whose names are identical -- no fuzzy matching
                mapper.getConfiguration()
                                .setMatchingStrategy(MatchingStrategies.STRICT);

                // -- RegisterRequest -> User -------------------------------------------
                // Skip passwordHash -- set manually after BCrypt encoding in UserService.
                mapper.typeMap(RegisterRequest.class, User.class)
                                .addMappings(m -> m.skip(User::setPasswordHash));

                // -- User -> UserResponse ----------------------------------------------
                // UUID -> String conversion using a typed Converter.
                //
                // WHY a Converter and not m.map(src -> src.getId().toString(), ...)?
                // ModelMapper's addMappings() uses a proxy of the source class to "record"
                // which getter was called. Calling .toString() on that proxy fails because
                // ModelMapper cannot intercept it -- it throws an ErrorsException at bean
                // creation time. A Converter receives the already-resolved UUID value, so
                // calling .toString() on it is safe.
                Converter<UUID, String> uuidToString = ctx -> ctx.getSource() != null ? ctx.getSource().toString()
                                : null;

                mapper.typeMap(User.class, UserResponse.class)
                                .addMappings(m -> m.using(uuidToString).map(User::getId, UserResponse::setId));

                return mapper;
        }
}