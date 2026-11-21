package com.qlsv.model;

import java.io.Serializable;

/**
 * Request payload for paginated queries over UDP.
 */
public class PageRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private int page = 1;
    private int pageSize = 20;
    private String query = "";

    public PageRequest() {}

    public PageRequest(int page, int pageSize) {
        this.page = page > 0 ? page : 1;
        this.pageSize = pageSize > 0 ? pageSize : 20;
    }

    public PageRequest(int page, int pageSize, String query) {
        this.page = page > 0 ? page : 1;
        this.pageSize = pageSize > 0 ? pageSize : 20;
        this.query = query != null ? query.trim() : "";
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
