package com.weishuo.backend.news.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FeedMedia {
    String type;
    String cover;
    String duration;
}
