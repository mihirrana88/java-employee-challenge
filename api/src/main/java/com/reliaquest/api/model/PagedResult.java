package com.reliaquest.api.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic paginated response wrapper for API responses.
 * Provides pagination metadata along with the actual data.
 *
 * @param <T> Type of the data being paginated
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagedResult<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean hasNext;
    private boolean hasPrevious;

    /**
     * Constructor that automatically calculates pagination metadata.
     */
    public PagedResult(List<T> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = (int) Math.ceil((double) totalElements / size);
        this.first = page == 0;
        this.last = page >= totalPages - 1;
        this.hasNext = page < totalPages - 1;
        this.hasPrevious = page > 0;
    }

    /**
     * Returns the number of elements in the current page.
     */
    public int getNumberOfElements() {
        return content != null ? content.size() : 0;
    }

    /**
     * Checks if the current page is empty.
     */
    public boolean isEmpty() {
        return content == null || content.isEmpty();
    }
}
