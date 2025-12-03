package com.weishuo.backend.news;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 新闻备份仓储接口
 */
@Repository
public interface NewsBackupRepository extends JpaRepository<NewsBackup, Long> {

    /**
     * 查询最新的 20 条新闻（按发布时间降序）
     */
    List<NewsBackup> findTop20ByOrderByPublishedAtDesc();

    /**
     * 根据 URL 查找新闻（用于去重）
     */
    NewsBackup findByUrl(String url);
}
