package com.weishuo.backend.news;

import com.weishuo.backend.config.NewsProperties;
import com.weishuo.backend.news.dto.FeedAuthor;
import com.weishuo.backend.news.dto.FeedMedia;
import com.weishuo.backend.news.dto.FeedStats;
import com.weishuo.backend.news.dto.MediastackResponse;
import com.weishuo.backend.news.dto.NewsFeedItem;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsService {

    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");
        private static final Map<String, String> CATEGORY_MAP = Map.of(
            "hot", "general",
            "tech", "technology",
            "video", "general",
            "society", "general",
            "headline", "general"
        );

    private final WebClient.Builder webClientBuilder;
    private final NewsProperties properties;

    public List<NewsFeedItem> fetchLatest(String channel) {
        if (!properties.isConfigured()) {
            log.warn("新闻 API Key 未配置，返回空列表");
            return Collections.emptyList();
        }

        try {
            String category = CATEGORY_MAP.getOrDefault(channel, "general");
                MediastackResponse response = webClientBuilder
                    .baseUrl(properties.getBaseUrl())
                    .build()
                    .get()
                    .uri(uriBuilder -> buildNewsUri(uriBuilder, category))
                    .retrieve()
                    .bodyToMono(MediastackResponse.class)
                    .block();

                if (response == null || response.getData() == null) {
                return Collections.emptyList();
            }

                return response.getData().stream()
                    .map(article -> mapToFeedItem(article, channel))
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.error("拉取新闻失败: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

            private java.net.URI buildNewsUri(UriBuilder uriBuilder, String category) {
        return uriBuilder
                .path("/news")
                .queryParam("access_key", properties.getApiKey())
                .queryParam("countries", properties.normalizedCountry())
                .queryParam("languages", properties.normalizedLanguage())
                .queryParam("limit", properties.getPageSize())
                .queryParam("categories", category)
                .build();
    }

            private NewsFeedItem mapToFeedItem(MediastackResponse.Article article, String channel) {
            String id = article.getUrl() != null ? Integer.toHexString(article.getUrl().hashCode()) : String.valueOf(System.nanoTime());
            String title = StringUtils.hasText(article.getTitle()) ? article.getTitle().trim() : "实时资讯快报";
            String description = StringUtils.hasText(article.getDescription()) ? article.getDescription().trim() : title;
            String sourceName = StringUtils.hasText(article.getSource()) ? article.getSource() : "实时热搜";

        FeedAuthor author = FeedAuthor.builder()
                .name(sourceName)
            .handle("@" + sourceName.replaceAll("\\s+", ""))
                .avatar("https://i.pravatar.cc/120?img=" + Math.abs(id.hashCode()) % 70)
                .verified(true)
                .badge("资讯")
                .build();

        FeedMedia media = null;
        if (StringUtils.hasText(article.getImage())) {
            media = FeedMedia.builder()
                    .type("image")
                .cover(article.getImage())
                    .build();
        }

        FeedStats stats = FeedStats.builder()
                .reposts(ThreadLocalRandom.current().nextInt(20, 120))
                .comments(ThreadLocalRandom.current().nextInt(40, 260))
                .likes(ThreadLocalRandom.current().nextInt(200, 1800))
                .build();

        return NewsFeedItem.builder()
                .id(id)
                .tag("#" + truncateTitle(title) + "#")
                .author(author)
                .content(buildContent(title, description))
                .media(media)
                .stats(stats)
                .createdAt(formatPublishedAt(article.getPublished_at()))
                .source(sourceName)
                .link(article.getUrl())
                .build();
    }

    private String buildContent(String title, String description) {
        if (title.equals(description)) {
            return title;
        }
        return title + "\n" + description;
    }

    private String truncateTitle(String title) {
        if (title.length() <= 20) {
            return title;
        }
        return title.substring(0, 20);
    }

    private String formatPublishedAt(String publishedAt) {
        if (!StringUtils.hasText(publishedAt)) {
            return "刚刚";
        }
        try {
            OffsetDateTime odt = OffsetDateTime.parse(publishedAt);
            return odt.atZoneSameInstant(CHINA_ZONE).format(TIME_FMT);
        } catch (Exception ex) {
            return "刚刚";
        }
    }
}
