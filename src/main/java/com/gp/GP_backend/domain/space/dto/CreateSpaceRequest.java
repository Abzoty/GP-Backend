package com.gp.GP_backend.domain.space.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class CreateSpaceRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 1000)
    private String description;

    @Size(max = 80)
    private String category;

    @Size(max = 30)
    private String courseCode;
}
