package com.gp.GP_backend.domain.material.controller;

import com.gp.GP_backend.domain.material.dto.MaterialResponse;
import com.gp.GP_backend.domain.material.service.MaterialService;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.shared.exception.ApiException;
import com.gp.GP_backend.shared.storage.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialControllerTest {

    @Mock
    private MaterialService materialService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private Resource resource;

    @InjectMocks
    private MaterialController controller;

    @Test
    void downloadFileShouldReturnAttachmentResponse() {
        UUID materialId = UUID.randomUUID();
        User user = user(UUID.randomUUID());
        MaterialResponse material = MaterialResponse.builder()
                .resourceType("PDF")
                .url("files/notes.pdf")
                .build();

        when(materialService.getMaterial(materialId, user)).thenReturn(material);
        when(fileStorageService.loadAsResource("files/notes.pdf")).thenReturn(resource);
        when(resource.getFilename()).thenReturn("notes.pdf");

        ResponseEntity<Resource> entity = controller.downloadFile(materialId, user);

        assertThat(entity.getStatusCode().value()).isEqualTo(200);
        assertThat(entity.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(entity.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).isEqualTo("attachment; filename=\"notes.pdf\"");
        assertThat(entity.getBody()).isEqualTo(resource);

        verify(fileStorageService).loadAsResource("files/notes.pdf");
    }

    @Test
    void downloadFileShouldRejectExternalLinks() {
        UUID materialId = UUID.randomUUID();
        User user = user(UUID.randomUUID());
        MaterialResponse material = MaterialResponse.builder()
                .resourceType("LINK")
                .url("https://example.com")
                .build();

        when(materialService.getMaterial(materialId, user)).thenReturn(material);

        assertThatThrownBy(() -> controller.downloadFile(materialId, user))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot be downloaded");
    }

    private static User user(UUID id) {
        return User.builder()
                .id(id)
                .email("student@example.com")
                .passwordHash("hash")
                .fullName("Student Name")
                .build();
    }
}