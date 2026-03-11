package com.gp.GP_backend.domain.post.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Post endpoints.
 *
 * TODO: Implement CRUD endpoints once PostService is complete.
 * All endpoints here are protected by JWT (configured in SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {
    // TODO: inject PostService
}
