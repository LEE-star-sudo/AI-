package com.weishuo.backend.news.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FeedStats {
    int reposts;
    int comments;
    int likes;
}
