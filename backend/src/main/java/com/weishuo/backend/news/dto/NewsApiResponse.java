package com.weishuo.backend.news.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsApiResponse {

    private String status;
    private int totalResults;
    private List<Article> articles;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Article {
        private Source source;
        private String author;
        private String title;
        private String description;
        private String url;
        private String urlToImage;
        private String publishedAt;
        private String content;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Source {
        @JsonProperty("name")
        private String name;
    }
}
