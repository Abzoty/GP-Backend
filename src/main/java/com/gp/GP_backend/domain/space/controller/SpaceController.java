package com.gp.GP_backend.domain.space.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Space endpoints.
 *
 * TODO: Implement CRUD endpoints once SpaceService is complete.
 * All endpoints here are protected by JWT (configured in SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/spaces")
@RequiredArgsConstructor
public class SpaceController {
    // TODO: inject SpaceService
}
