package com.reliaquest.api.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request parameters for pagination.
 * Provides validation and default values for page-based pagination.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaginationRequest {

    @Min(value = 0, message = "Page number must be non-negative")
    @Builder.Default
    private int page = 0;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size must not exceed 100")
    @Builder.Default
    private int size = 20;

    /**
     * Sort criteria for the results. Can be field names like "name", "salary", etc.
     * Optional - if null or empty, no sorting is applied.
     */
    private String sort;

    /**
     * Calculates the offset for the current page.
     */
    public long getOffset() {
        return (long) page * size;
    }

    /**
     * Creates a pagination request with default values.
     */
    public static PaginationRequest defaultRequest() {
        return PaginationRequest.builder().build();
    }

    /**
     * Creates a pagination request for the next page.
     */
    public PaginationRequest nextPage() {
        return PaginationRequest.builder()
                .page(this.page + 1)
                .size(this.size)
                .sort(this.sort)
                .build();
    }

    /**
     * Creates a pagination request for the previous page.
     */
    public PaginationRequest previousPage() {
        return PaginationRequest.builder()
                .page(Math.max(0, this.page - 1))
                .size(this.size)
                .sort(this.sort)
                .build();
    }
}
