package com.gp.GP_backend.shared.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {

    // The items on this page.
    private List<T> content;

    // Zero-based current page number.
    private int page;

    // Maximum number of items per page.
    private int size;

    // Total number of items across all pages.
    private long totalElements;

    // Total number of pages available.
    private int totalPages;

    // True if this is the last page.
    private boolean last;

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
