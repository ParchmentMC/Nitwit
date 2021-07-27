package org.parchmentmc.nitwit.discord.pagination;

public record PaginationInfo(int totalItems, boolean hasPreviousPage, String startCursor,
                             String endCursor, boolean hasNextPage) {
}
