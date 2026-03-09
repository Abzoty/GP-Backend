package com.gp.GP_backend.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class CreatePostRequest {

    @NotNull
    private Long spaceId;

    @NotBlank
    @Size(max = 300)
    private String title;

    @NotBlank
    private String body;

    @Pattern(regexp = "QUESTION|DISCUSSION")
    private String postType = "QUESTION";

    @Size(max = 500)
    private String tags;
}
