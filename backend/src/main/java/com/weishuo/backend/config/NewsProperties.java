package com.weishuo.backend.config;

import java.util.Locale;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConfigurationProperties(prefix = "news.api")
@Getter
@Setter
public class NewsProperties {

    /** API 基础地址，例如 http://api.mediastack.com/v1 */
    private String baseUrl = "http://api.mediastack.com/v1";

    /** 提供方发放的 API Key。 */
    private String apiKey;

    /** 默认国家代码。 */
    private String country = "cn";

    /** 默认语言代码。 */
    private String language = "zh";

    /** 返回的记录数。 */
    private int pageSize = 10;

    public String normalizedCountry() {
        return StringUtils.hasText(country) ? country.toLowerCase(Locale.ROOT) : "cn";
    }

    public String normalizedLanguage() {
        return StringUtils.hasText(language) ? language.toLowerCase(Locale.ROOT) : "zh";
    }

    public boolean isConfigured() {
        return StringUtils.hasText(apiKey);
    }
}
