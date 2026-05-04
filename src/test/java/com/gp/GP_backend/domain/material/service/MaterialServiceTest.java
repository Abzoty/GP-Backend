package com.gp.GP_backend.domain.material.service;

import com.gp.GP_backend.domain.material.dto.EditMaterialRequest;
import com.gp.GP_backend.domain.material.dto.MaterialResponse;
import com.gp.GP_backend.domain.material.dto.ShareLinkRequest;
import com.gp.GP_backend.domain.material.entity.Material;
import com.gp.GP_backend.domain.material.entity.MaterialLink;
import com.gp.GP_backend.domain.material.repository.MaterialLinkRepository;
import com.gp.GP_backend.domain.material.repository.MaterialRepository;
import com.gp.GP_backend.domain.notification.service.NotificationService;
import com.gp.GP_backend.domain.space.entity.Space;
import com.gp.GP_backend.domain.space.repository.SpaceMembershipRepository;
import com.gp.GP_backend.domain.space.repository.SpaceRepository;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.service.GamificationService;
import com.gp.GP_backend.shared.exception.ApiException;
import com.gp.GP_backend.shared.storage.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private MaterialLinkRepository materialLinkRepository;

    @Mock
    private SpaceMembershipRepository spaceMembershipRepository;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private GamificationService gamificationService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private MaterialService materialService;

    @Test
    void uploadFileShouldSaveForMember() {
        User uploader = user();
        UUID spaceId = UUID.randomUUID();
        Space space = space(spaceId);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "lecture.pdf",
                "application/pdf",
                "content".getBytes());

        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
        when(spaceMembershipRepository.existsBySpaceIdAndUserId(spaceId, uploader.getId())).thenReturn(true);
        when(fileStorageService.store(file)).thenReturn("stored.pdf");

        Material saved = material(space, uploader);
        saved.setTitle("Lecture 1");
        saved.setResourceType("PDF");
        saved.setUrl("stored.pdf");
        when(materialRepository.save(any(Material.class))).thenReturn(saved);

        MaterialResponse response = materialService.uploadFile(spaceId, " Lecture 1 ", "Intro", file, uploader);

        assertEquals("Lecture 1", response.getTitle());
        assertEquals("PDF", response.getResourceType());
        assertEquals("stored.pdf", response.getUrl());
        assertFalse(response.getIsBookmarked());
    }

    @Test
    void uploadFileShouldRejectNonMember() {
        User uploader = user();
        UUID spaceId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "x.pdf", "application/pdf", "x".getBytes());

        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space(spaceId)));
        when(spaceMembershipRepository.existsBySpaceIdAndUserId(spaceId, uploader.getId())).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
                () -> materialService.uploadFile(spaceId, "Title", null, file, uploader));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(fileStorageService, never()).store(any());
    }

    @Test
    void shareLinkShouldSaveForMember() {
        User uploader = user();
        UUID spaceId = UUID.randomUUID();
        Space space = space(spaceId);

        ShareLinkRequest request = new ShareLinkRequest();
        request.setTitle(" Useful resource ");
        request.setDescription("Great article");
        request.setUrl("https://example.com");

        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
        when(spaceMembershipRepository.existsBySpaceIdAndUserId(spaceId, uploader.getId())).thenReturn(true);

        Material saved = material(space, uploader);
        saved.setResourceType("LINK");
        saved.setTitle("Useful resource");
        saved.setUrl("https://example.com");

        when(materialRepository.save(any(Material.class))).thenReturn(saved);

        MaterialResponse response = materialService.shareLink(spaceId, request, uploader);

        assertEquals("LINK", response.getResourceType());
        assertEquals("Useful resource", response.getTitle());
    }

    @Test
    void bookmarkShouldIncrementCount() {
        User user = user();
        UUID materialId = UUID.randomUUID();
        Material material = material(space(UUID.randomUUID()), user());
        material.setId(materialId);
        material.setLinkCount(0);

        when(materialRepository.findById(materialId)).thenReturn(Optional.of(material));
        when(spaceMembershipRepository.existsBySpaceIdAndUserId(material.getSpace().getId(), user.getId())).thenReturn(true);
        when(materialLinkRepository.save(any(MaterialLink.class))).thenAnswer(invocation -> {
            MaterialLink link = invocation.getArgument(0);
            link.setId(UUID.randomUUID());
            return link;
        });

        materialService.bookmark(materialId, user);

        verify(materialLinkRepository).save(any(MaterialLink.class));
        verify(materialLinkRepository).incrementLinkCount(materialId);
    }

    @Test
    void bookmarkShouldThrowConflictIfAlreadyBookmarked() {
        User user = user();
        UUID materialId = UUID.randomUUID();
        Material material = material(space(UUID.randomUUID()), user());
        material.setId(materialId);

        when(materialRepository.findById(materialId)).thenReturn(Optional.of(material));
        when(spaceMembershipRepository.existsBySpaceIdAndUserId(material.getSpace().getId(), user.getId())).thenReturn(true);
        when(materialLinkRepository.save(any(MaterialLink.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate"));

        ApiException ex = assertThrows(ApiException.class, () -> materialService.bookmark(materialId, user));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(materialLinkRepository, never()).incrementLinkCount(any());
    }

    @Test
    void bookmarkShouldThrowConflictWhenSaveHitsUniqueConstraint() {
        User user = user();
        UUID materialId = UUID.randomUUID();
        Material material = material(space(UUID.randomUUID()), user());
        material.setId(materialId);

        when(materialRepository.findById(materialId)).thenReturn(Optional.of(material));
        when(spaceMembershipRepository.existsBySpaceIdAndUserId(material.getSpace().getId(), user.getId())).thenReturn(true);
        when(materialLinkRepository.save(any(MaterialLink.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        ApiException ex = assertThrows(ApiException.class, () -> materialService.bookmark(materialId, user));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(materialLinkRepository, never()).incrementLinkCount(any());
    }

    @Test
    void bookmarkOwnMaterialShouldNotAwardXp() {
        User user = user();
        UUID materialId = UUID.randomUUID();
        Material material = material(space(UUID.randomUUID()), user);
        material.setId(materialId);

        when(materialRepository.findById(materialId)).thenReturn(Optional.of(material));
        when(spaceMembershipRepository.existsBySpaceIdAndUserId(material.getSpace().getId(), user.getId())).thenReturn(true);
        when(materialLinkRepository.save(any(MaterialLink.class))).thenAnswer(invocation -> {
            MaterialLink link = invocation.getArgument(0);
            link.setId(UUID.randomUUID());
            return link;
        });

        materialService.bookmark(materialId, user);

        verify(gamificationService, never()).awardXp(any(), any(), anyInt(), any(), any());
    }

    @Test
    void unbookmarkShouldThrowNotFoundIfMissingBookmark() {
        User user = user();
        UUID materialId = UUID.randomUUID();
        Material material = material(space(UUID.randomUUID()), user());
        material.setId(materialId);

        when(materialRepository.findById(materialId)).thenReturn(Optional.of(material));
        when(spaceMembershipRepository.existsBySpaceIdAndUserId(material.getSpace().getId(), user.getId())).thenReturn(true);
        when(materialLinkRepository.findByMaterialIdAndUserId(materialId, user.getId())).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> materialService.unbookmark(materialId, user));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void editMaterialShouldRejectNonUploader() {
        User owner = user();
        User other = User.builder().id(UUID.randomUUID()).build();
        UUID materialId = UUID.randomUUID();

        Material material = material(space(UUID.randomUUID()), owner);
        material.setId(materialId);

        when(materialRepository.findById(materialId)).thenReturn(Optional.of(material));

        ApiException ex = assertThrows(ApiException.class,
                () -> materialService.editMaterial(materialId, new EditMaterialRequest(), other));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void deleteMaterialShouldDeleteFileAndRowForFileMaterial() {
        User owner = user();
        UUID materialId = UUID.randomUUID();
        Material material = material(space(UUID.randomUUID()), owner);
        material.setId(materialId);
        material.setResourceType("PDF");
        material.setUrl("stored.pdf");

        when(materialRepository.findById(materialId)).thenReturn(Optional.of(material));

        materialService.deleteMaterial(materialId, owner);

        verify(materialLinkRepository).deleteByMaterialId(materialId);
        verify(fileStorageService).delete("stored.pdf");
        verify(materialRepository).deleteById(materialId);
    }

    @Test
    void deleteMaterialShouldNotDeletePhysicalFileForLinkResource() {
        User owner = user();
        UUID materialId = UUID.randomUUID();
        Material material = material(space(UUID.randomUUID()), owner);
        material.setId(materialId);
        material.setResourceType("LINK");
        material.setUrl("https://example.com");

        when(materialRepository.findById(materialId)).thenReturn(Optional.of(material));

        materialService.deleteMaterial(materialId, owner);

        verify(fileStorageService, never()).delete(any());
        verify(materialRepository).deleteById(materialId);
    }

    @Test
    void getMaterialShouldRejectNonMember() {
        User requester = user();
        UUID materialId = UUID.randomUUID();
        Material material = material(space(UUID.randomUUID()), user());
        material.setId(materialId);

        when(materialRepository.findById(materialId)).thenReturn(Optional.of(material));
        when(spaceMembershipRepository.existsBySpaceIdAndUserId(material.getSpace().getId(), requester.getId())).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> materialService.getMaterial(materialId, requester));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void getMaterialsBySpaceShouldMapBookmarkStatePerMaterial() {
        User requester = user();
        UUID spaceId = UUID.randomUUID();
        Space space = space(spaceId);
        Material m1 = material(space, user());
        Material m2 = material(space, user());

        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
        when(spaceMembershipRepository.existsBySpaceIdAndUserId(spaceId, requester.getId())).thenReturn(true);
        when(materialRepository.findBySpaceIdOrderByCreatedAtDesc(spaceId)).thenReturn(List.of(m1, m2));
        when(materialLinkRepository.findByUserIdAndSpaceId(requester.getId(), spaceId))
            .thenReturn(List.of(MaterialLink.builder().material(m1).user(requester).build()));

        List<MaterialResponse> response = materialService.getMaterialsBySpace(spaceId, requester);

        assertEquals(2, response.size());
        assertTrue(response.get(0).getIsBookmarked());
        assertFalse(response.get(1).getIsBookmarked());
    }

    @Test
    void getMaterialsBySpacePagedShouldReturnPagedMappedResponse() {
        User requester = user();
        UUID spaceId = UUID.randomUUID();
        Space space = space(spaceId);
        Material m1 = material(space, user());
        Material m2 = material(space, user());

        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
        when(spaceMembershipRepository.existsBySpaceIdAndUserId(spaceId, requester.getId())).thenReturn(true);
        when(materialLinkRepository.findByUserIdAndSpaceId(requester.getId(), spaceId))
                .thenReturn(List.of(MaterialLink.builder().material(m1).user(requester).build()));

        Page<Material> page = new PageImpl<>(List.of(m1, m2), PageRequest.of(0, 2), 2);
        when(materialRepository.findBySpaceIdOrderByCreatedAtDesc(spaceId, PageRequest.of(0, 2)))
                .thenReturn(page);

        Page<MaterialResponse> response = materialService.getMaterialsBySpacePaged(spaceId, requester, 0, 2);

        assertEquals(2, response.getContent().size());
        assertTrue(response.getContent().get(0).getIsBookmarked());
        assertFalse(response.getContent().get(1).getIsBookmarked());
    }

    @Test
    void uploadFileShouldPropagateUnexpectedStorageFailure() {
        User uploader = user();
        UUID spaceId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "x.pdf", "application/pdf", "x".getBytes());

        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space(spaceId)));
        when(spaceMembershipRepository.existsBySpaceIdAndUserId(spaceId, uploader.getId())).thenReturn(true);
        when(fileStorageService.store(file)).thenThrow(new RuntimeException("disk error"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> materialService.uploadFile(spaceId, "Title", null, file, uploader));

        assertEquals("disk error", ex.getMessage());
    }

    private User user() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("user@gp.com")
                .fullName("User")
                .build();
    }

    private Space space(UUID id) {
        return Space.builder()
                .id(id)
                .name("Space")
                .slug("space")
                .isActive(true)
                .build();
    }

    private Material material(Space space, User uploader) {
        return Material.builder()
                .id(UUID.randomUUID())
                .space(space)
                .uploadedBy(uploader)
                .title("Material")
                .description("Desc")
                .resourceType("PDF")
                .url("stored.pdf")
                .fileSizeKb(42)
                .linkCount(0)
                .build();
    }
}
