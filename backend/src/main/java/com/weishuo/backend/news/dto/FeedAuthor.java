package com.weishuo.backend.news.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FeedAuthor {
    String name;
    String handle;
    String avatar;
    boolean verified;
    String badge;
}
