package com.gp.GP_backend.domain.material.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Material endpoints.
 *
 * TODO: Implement CRUD endpoints once MaterialService is complete.
 * All endpoints here are protected by JWT (configured in SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/materials")
@RequiredArgsConstructor
public class MaterialController {
    // TODO: inject MaterialService
}
