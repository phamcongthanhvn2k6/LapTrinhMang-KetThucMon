package com.qlsv.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Generic container for paginated query results.
 */
public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<T> items;
    private int currentPage;
    private int totalPages;
    private int totalItems;
    private int pageSize;

    public PageResult() {
        this.items = Collections.emptyList();
    }

    public PageResult(List<T> items, int currentPage, int pageSize, int totalItems) {
        this.items = items != null ? items : Collections.emptyList();
        this.currentPage = currentPage;
        this.pageSize = pageSize > 0 ? pageSize : 20;
        this.totalItems = totalItems;
        this.totalPages = (int) Math.ceil((double) totalItems / this.pageSize);
        if (this.totalPages == 0) {
            this.totalPages = 1;
        }
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public boolean hasPrevious() {
        return currentPage > 1;
    }

    public boolean hasNext() {
        return currentPage < totalPages;
    }
}

