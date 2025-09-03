package com.udacity.webcrawler.main;

import com.google.inject.Guice;
import com.udacity.webcrawler.WebCrawler;
import com.udacity.webcrawler.WebCrawlerModule;
import com.udacity.webcrawler.json.ConfigurationLoader;
import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.json.CrawlResultWriter;
import com.udacity.webcrawler.json.CrawlerConfiguration;
import com.udacity.webcrawler.profiler.Profiler;
import com.udacity.webcrawler.profiler.ProfilerModule;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Objects;

public final class WebCrawlerMain
{

    private final CrawlerConfiguration config;

    private WebCrawlerMain(CrawlerConfiguration config)
    {
        this.config = Objects.requireNonNull(config);
    }

    @Inject
    private WebCrawler crawler;

    @Inject
    private Profiler profiler;

    private void run() throws Exception
    {
        Guice.createInjector(new WebCrawlerModule(config), new ProfilerModule()).injectMembers(this);

        CrawlResult result = crawler.crawl(config.getStartPages());
        CrawlResultWriter resultWriter = new CrawlResultWriter(result);

        // Write the crawl results to a JSON file (or System.out if the file name is empty)
        String resultPath = config.getResultPath();
        if (resultPath != null && !resultPath.isEmpty())
        {
            Path resultFilePath = Path.of(resultPath);
            if (resultFilePath.getParent() != null)
            {
                Files.createDirectories(resultFilePath.getParent());
            }
            resultWriter.write(resultFilePath);
        } else
        {
            try (Writer writer = new OutputStreamWriter(System.out))
            {
                resultWriter.write(writer);
                writer.flush(); // ensures output appears immediately
            }
        }

        // Write the profile data to a text file (or System.out if the file name is empty)
        String profilePath = config.getProfileOutputPath();
        if (profilePath != null && !profilePath.isEmpty())
        {
            Path profileFilePath = Path.of(profilePath);
            if (profileFilePath.getParent() != null)
            {
                Files.createDirectories(profileFilePath.getParent());
            }
            try (BufferedWriter writer = Files.newBufferedWriter(profileFilePath)) {
                profiler.writeData(writer);
            }
        } else

        {
            try (Writer writer = new OutputStreamWriter(System.out))
            {
                profiler.writeData(writer);
                writer.flush();
            }
        }
    }

    public static void main(String[] args) throws Exception
    {
        if (args.length != 1)
        {
            System.out.println("Usage: WebCrawlerMain [configuration-file-path]");
            return;
        }

        CrawlerConfiguration config = new ConfigurationLoader(Path.of(args[0])).load();
        new WebCrawlerMain(config).run();
    }
}
