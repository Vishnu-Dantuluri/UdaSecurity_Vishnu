package com.udacity.webcrawler;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.udacity.webcrawler.json.CrawlerConfiguration;
import com.udacity.webcrawler.IgnoredUrls;
import com.udacity.webcrawler.IgnoredWords;
import com.udacity.webcrawler.MaxDepth;


import java.util.List;
import java.util.regex.Pattern;

/**
 * Guice module to bind configuration values for dependency injection.
 */
public class CrawlerModule extends AbstractModule {
    private final CrawlerConfiguration config;

    public CrawlerModule(CrawlerConfiguration config) {
        this.config = config;
    }

    @Override
    protected void configure() {
        // Additional bindings can go here if needed
    }

    @Provides
    @Singleton
    @IgnoredUrls
    List<Pattern> provideIgnoredUrls() {
        return config.getIgnoredUrls();
    }

    @Provides
    @Singleton
    @IgnoredWords
    List<Pattern> provideIgnoredWords() {
        return config.getIgnoredWords();
    }

    @Provides
    @Singleton
    @MaxDepth
    int provideMaxDepth() {
        return config.getMaxDepth();
    }
}
