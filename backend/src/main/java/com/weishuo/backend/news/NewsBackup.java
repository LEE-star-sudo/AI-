package com.weishuo.backend.news;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 新闻备份表 - 用于缓存远程 API 的新闻数据
 * 当远程 API 不可用时，从此表读取最新的缓存新闻
 */
@Entity
@Table(name = "news_backup")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsBackup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 100)
    private String source;

    @Column(length = 1000)
    private String url;

    @Column(length = 50)
    private String category;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
