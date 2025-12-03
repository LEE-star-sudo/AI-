package com.weishuo.backend.news.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NewsFeedItem {

    String id;
    String tag;
    FeedAuthor author;
    String content;
    FeedMedia media;
    FeedStats stats;
    String createdAt;
    String source;
    String link;
}
