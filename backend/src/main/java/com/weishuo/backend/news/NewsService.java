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
    private final NewsBackupRepository newsBackupRepository;

    /**
     * 获取最新新闻 - 优先从远程 API，失败时从数据库缓存读取
     */
    public List<NewsFeedItem> fetchLatest(String channel) {
        try {
            // 第一步：尝试从远程 API 获取新闻
            List<NewsFeedItem> freshNews = fetchFromRemoteApi(channel);
            
            if (!freshNews.isEmpty()) {
                // 成功获取到新闻，保存到数据库作为缓存
                saveToCache(freshNews, channel);
                log.info("成功从远程 API 获取 {} 条新闻", freshNews.size());
                return freshNews;
            }
        } catch (Exception ex) {
            // 远程 API 失败，记录日志
            log.warn("远程新闻 API 失败，切换到数据库缓存: {}", ex.getMessage());
        }

        // 第二步：从数据库缓存读取
        return fetchFromCache();
    }

    /**
     * 从远程 API 获取新闻
     */
    private List<NewsFeedItem> fetchFromRemoteApi(String channel) {
        if (!properties.isConfigured()) {
            log.warn("新闻 API Key 未配置");
            throw new RuntimeException("API Key not configured");
        }

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
            throw new RuntimeException("Empty response from API");
        }

                return response.getData().stream()
                    .map(article -> mapToFeedItem(article, channel))
                    .collect(Collectors.toList());
    }

    /**
     * 保存新闻到数据库缓存
     */
    private void saveToCache(List<NewsFeedItem> newsItems, String category) {
        try {
            int savedCount = 0;
            for (NewsFeedItem item : newsItems) {
                // 跳过没有 URL 的新闻（无法去重）
                if (!StringUtils.hasText(item.getLink())) {
                    continue;
                }
                
                // 检查是否已存在（根据 URL 去重）
                NewsBackup existing = newsBackupRepository.findByUrl(item.getLink());
                if (existing == null) {
                    NewsBackup backup = NewsBackup.builder()
                            .title(item.getAuthor() != null ? item.getAuthor().getName() : "未知来源")
                            .summary(item.getTag())
                            .content(item.getContent())
                            .source(item.getSource())
                            .url(item.getLink())
                            .category(category)
                            .publishedAt(parsePublishedAt(item.getCreatedAt()))
                            .build();
                    newsBackupRepository.save(backup);
                    savedCount++;
                }
            }
            log.info("已缓存 {} 条新闻到数据库", savedCount);
        } catch (Exception ex) {
            log.error("保存新闻缓存失败: {}", ex.getMessage());
        }
    }

    /**
     * 从数据库缓存读取新闻
     */
    private List<NewsFeedItem> fetchFromCache() {
        try {
            List<NewsBackup> cachedNews = newsBackupRepository.findTop20ByOrderByPublishedAtDesc();
            if (cachedNews.isEmpty()) {
                log.warn("数据库缓存为空，返回空列表");
                return Collections.emptyList();
            }

            log.info("从数据库缓存读取 {} 条新闻", cachedNews.size());
            return cachedNews.stream()
                    .map(this::convertToFeedItem)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.error("读取缓存失败: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 将数据库实体转换为前端 DTO
     */
    private NewsFeedItem convertToFeedItem(NewsBackup backup) {
        String id = backup.getUrl() != null ? Integer.toHexString(backup.getUrl().hashCode()) : String.valueOf(backup.getId());

        FeedAuthor author = FeedAuthor.builder()
                .name(backup.getSource() != null ? backup.getSource() : "实时热搜")
                .handle("@" + (backup.getSource() != null ? backup.getSource().replaceAll("\\s+", "") : "news"))
                .avatar("https://i.pravatar.cc/120?img=" + Math.abs(id.hashCode()) % 70)
                .verified(true)
                .badge("资讯")
                .build();

        FeedStats stats = FeedStats.builder()
                .reposts(ThreadLocalRandom.current().nextInt(20, 120))
                .comments(ThreadLocalRandom.current().nextInt(40, 260))
                .likes(ThreadLocalRandom.current().nextInt(200, 1800))
                .build();

        return NewsFeedItem.builder()
                .id(id)
                .tag(backup.getSummary() != null ? backup.getSummary() : "#新闻#")
                .author(author)
                .content(backup.getContent() != null ? backup.getContent() : backup.getTitle())
                .media(null)
                .stats(stats)
                .createdAt(formatLocalDateTime(backup.getPublishedAt()))
                .source(backup.getSource())
                .link(backup.getUrl())
                .build();
    }

    private java.time.LocalDateTime parsePublishedAt(String createdAt) {
        if (!StringUtils.hasText(createdAt)) {
            return java.time.LocalDateTime.now();
        }
        try {
            // 尝试解析格式: "MM-dd HH:mm"
            String currentYear = String.valueOf(java.time.Year.now().getValue());
            String fullDateString = currentYear + "-" + createdAt.trim();
            java.time.format.DateTimeFormatter formatter = 
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return java.time.LocalDateTime.parse(fullDateString, formatter);
        } catch (Exception ex) {
            // 解析失败则返回当前时间
            log.debug("解析时间失败: {}, 使用当前时间", createdAt);
            return java.time.LocalDateTime.now();
        }
    }

    private String formatLocalDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return "刚刚";
        }
        return dateTime.atZone(CHINA_ZONE).format(TIME_FMT);
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
