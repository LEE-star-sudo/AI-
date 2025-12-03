package com.weishuo.backend.news.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MediastackResponse {

    private Pagination pagination;
    private List<Article> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pagination {
        private int limit;
        private int offset;
        private int count;
        private int total;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Article {
        private String author;
        private String title;
        private String description;
        private String url;
        private String source;
        private String image;
        private String category;
        private String language;
        private String country;
        private String published_at;
    }
}
