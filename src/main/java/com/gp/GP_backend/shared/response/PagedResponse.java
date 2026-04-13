package com.gp.GP_backend.shared.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Paginated list response included inside {@link ApiResponse#getData()}.
 *
 * <p>
 * Wraps Spring Data's {@link Page} into a clean, serializable DTO
 * that avoids exposing internal Spring types to the API client.
 *
 * <p>
 * Usage in a service/controller:
 * 
 * <pre>{@code
 * Page<Post> page = postRepository.findBySpaceId(spaceId, pageable);
 * return ApiResponse.ok("Posts retrieved", PagedResponse.of(page, PostResponse::fromEntity));
 * }</pre>
 *
 * @param <T> the type of each item in the page.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {

    /** The items on this page. */
    private List<T> content;

    /** Zero-based current page number. */
    private int page;

    /** Maximum number of items per page. */
    private int size;

    /** Total number of items across all pages. */
    private long totalElements;

    /** Total number of pages available. */
    private int totalPages;

    /** True if this is the last page. */
    private boolean last;

    /**
     * Builds a {@link PagedResponse} from a Spring Data {@link Page}.
     *
     * @param springPage the page returned by a repository method.
     * @param <T>        the item type.
     * @return a serialisable paged response.
     */
    public static <T> PagedResponse<T> of(Page<T> springPage) {
        return PagedResponse.<T>builder()
                .content(springPage.getContent())
                .page(springPage.getNumber())
                .size(springPage.getSize())
                .totalElements(springPage.getTotalElements())
                .totalPages(springPage.getTotalPages())
                .last(springPage.isLast())
                .build();
    }
}
