package com.weishuo.backend.news;

import com.weishuo.backend.news.dto.NewsFeedItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @GetMapping("/latest")
    public List<NewsFeedItem> latest(@RequestParam(defaultValue = "hot") String channel) {
        return newsService.fetchLatest(channel);
    }
}
