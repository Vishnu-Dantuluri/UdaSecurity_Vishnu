package com.udacity.webcrawler.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.BufferedWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Utility class to write a {@link CrawlResult} to file.
 */
public final class CrawlResultWriter
{
  private final CrawlResult result;

  /**
   * Creates a new {@link CrawlResultWriter} that will write the given {@link CrawlResult}.
   */
  public CrawlResultWriter(CrawlResult result)
  {
    this.result = Objects.requireNonNull(result);
  }

  /**
   * Formats the {@link CrawlResult} as JSON and writes it to the given {@link Path}.
   *
   * <p>If a file already exists at the path, the existing file should not be deleted; new data
   * should be appended to it.
   *
   * @param path the file path where the crawl result data should be written.
   */
  public void write(Path path)
  {
    Objects.requireNonNull(path);
    // Use BufferedWriter in append mode to append data if file exists
    try (BufferedWriter bufferedWriter = Files.newBufferedWriter(path, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)) {
      write(bufferedWriter);
    } catch (Exception e)
    {
      throw new RuntimeException("Error writing CrawlResult to file: " + path, e);
    }
  }

  /**
   * Formats the {@link CrawlResult} as JSON and writes it to the given {@link Writer}.
   *
   * @param writer the destination where the crawl result data should be written.
   */
  public void write(Writer writer)
  {
    Objects.requireNonNull(writer);
    try
    {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET); // Prevents Jackson from closing the writer
      objectMapper.writeValue(writer, result);
    } catch (Exception e)
    {
      throw new RuntimeException("Error writing CrawlResult JSON", e);
    }
  }
}
