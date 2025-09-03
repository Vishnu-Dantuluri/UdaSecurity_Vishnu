package com.udacity.webcrawler;

import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Qualifier annotation for ignored URL patterns.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface IgnoredUrls {}
