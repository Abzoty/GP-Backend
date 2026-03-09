package com.gp.GP_backend.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class CreateAnswerRequest {

    @NotNull
    private Long postId;

    @NotBlank
    private String body;
}
