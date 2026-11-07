package com.cipher.workspace.DTOs;

import org.springframework.data.domain.Page;

import java.util.List;

public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean empty
) {
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty()
        );
    }

    public static <T> PagedResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        boolean first = page <= 0;
        boolean last = totalPages == 0 || page >= totalPages - 1;
        boolean empty = content == null || content.isEmpty();
        return new PagedResponse<>(
                content != null ? content : List.of(),
                page,
                size,
                totalElements,
                totalPages,
                first,
                last,
                empty
        );
    }
}
