package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class ParallelWebCrawler implements WebCrawler {
  private final Clock clock;
  private final Duration timeout;
  private final int popularWordCount;
  private final ForkJoinPool pool;
  private final List<Pattern> ignoredUrls;
  private final List<Pattern> ignoredWords;
  private final int maxDepth;
  private final PageParserFactory parserFactory;

  @Inject
  ParallelWebCrawler(
          Clock clock,
          @Timeout Duration timeout,
          @PopularWordCount int popularWordCount,
          @TargetParallelism int threadCount,
          PageParserFactory parserFactory,
          @IgnoredUrls List<Pattern> ignoredUrls,
          @com.udacity.webcrawler.IgnoredWords List<Pattern> ignoredWords,
          @MaxDepth int maxDepth) {
    this.clock = clock;
    this.timeout = timeout;
    this.popularWordCount = popularWordCount;
    this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
    this.parserFactory = parserFactory;
    this.ignoredUrls = ignoredUrls;
    this.ignoredWords = ignoredWords;
    this.maxDepth = maxDepth;
  }

  @Override
  public CrawlResult crawl(List<String> startingUrls) {
    if (startingUrls == null || startingUrls.isEmpty()) {
      return new CrawlResult.Builder()
              .setUrlsVisited(0)
              .setWordCounts(Map.of())
              .build();
    }
    Instant deadline = clock.instant().plus(timeout);
    ConcurrentMap<String, Integer> wordCounts = new ConcurrentHashMap<>();
    Set<String> visitedUrls = new ConcurrentSkipListSet<>();

    List<String> filteredStartingUrls = startingUrls.stream()
            .filter(url -> !isUrlIgnored(url))
            .collect(Collectors.toList());

    CrawlTask rootTask = new CrawlTask(filteredStartingUrls, deadline, maxDepth,
            wordCounts, visitedUrls, parserFactory,
            ignoredUrls, ignoredWords);
    pool.invoke(rootTask);

    Map<String, Integer> popularWords = WordCounts.sort(wordCounts, popularWordCount);

    return new CrawlResult.Builder()
            .setWordCounts(popularWords)
            .setUrlsVisited(visitedUrls.size())
            .build();
  }

  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }

  private boolean isUrlIgnored(String url) {
    return ignoredUrls.stream().anyMatch(pattern -> pattern.matcher(url).matches());
  }

  private boolean isWordIgnored(String word) {
    return ignoredWords.stream().anyMatch(pattern -> pattern.matcher(word).matches());
  }

  private static class CrawlTask extends RecursiveTask<Void> {
    private final List<String> urls;
    private final Instant deadline;
    private final int maxDepth;
    private final ConcurrentMap<String, Integer> wordCounts;
    private final Set<String> visitedUrls;
    private final PageParserFactory parserFactory;
    private final List<Pattern> ignoredUrls;
    private final List<Pattern> ignoredWords;

    CrawlTask(List<String> urls,
              Instant deadline,
              int maxDepth,
              ConcurrentMap<String, Integer> wordCounts,
              Set<String> visitedUrls,
              PageParserFactory parserFactory,
              List<Pattern> ignoredUrls,
              List<Pattern> ignoredWords) {
      this.urls = urls;
      this.deadline = deadline;
      this.maxDepth = maxDepth;
      this.wordCounts = wordCounts;
      this.visitedUrls = visitedUrls;
      this.parserFactory = parserFactory;
      this.ignoredUrls = ignoredUrls;
      this.ignoredWords = ignoredWords;
    }

    @Override
    protected Void compute() {
      if (urls.isEmpty() || maxDepth == 0 || Instant.now().isAfter(deadline)) {
        return null;
      }

      List<CrawlTask> subtasks = urls.stream()
              .filter(url -> ignoredUrls.stream().noneMatch(p -> p.matcher(url).matches()))
              .filter(url -> visitedUrls.add(url))
              .map(url -> {
                PageParser.Result result = parserFactory.get(url).parse();
                Map<String, Integer> filteredCounts = result.getWordCounts().entrySet().stream()
                        .filter(entry -> ignoredWords.stream().noneMatch(p -> p.matcher(entry.getKey()).matches()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                filteredCounts.forEach((word, count) -> wordCounts.merge(word, count, Integer::sum));
                List<String> filteredLinks = result.getLinks().stream()
                        .filter(l -> ignoredUrls.stream().noneMatch(p -> p.matcher(l).matches()))
                        .collect(Collectors.toList());

                return new CrawlTask(filteredLinks, deadline, maxDepth - 1,
                        wordCounts, visitedUrls, parserFactory,
                        ignoredUrls, ignoredWords);
              })
              .collect(Collectors.toList());

      invokeAll(subtasks);
      return null;
    }
  }
}
