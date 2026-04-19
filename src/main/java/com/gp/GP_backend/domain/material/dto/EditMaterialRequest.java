package com.gp.GP_backend.domain.material.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class EditMaterialRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
}