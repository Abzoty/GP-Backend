package com.gp.GP_backend.domain.material.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class CreateMaterialRequest {

    @NotNull
    private Long spaceId;

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 1000)
    private String description;

    @Pattern(regexp = "PDF|LINK|IMAGE|VIDEO")
    private String resourceType;

    @Size(max = 1024)
    private String url;

    private Integer fileSizeKb;
}
