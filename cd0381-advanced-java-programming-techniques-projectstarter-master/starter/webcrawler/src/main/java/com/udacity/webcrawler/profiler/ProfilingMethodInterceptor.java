package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler
{

  private final Clock clock;
  private final Object delegate;
  private final ProfilingState state;

  ProfilingMethodInterceptor(Clock clock, Object delegate, ProfilingState state)
  {
    this.clock = Objects.requireNonNull(clock);
    this.delegate = Objects.requireNonNull(delegate);
    this.state = Objects.requireNonNull(state);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // Check if method is annotated for profiling
    if (!method.isAnnotationPresent(Profiled.class))
    {
      try
      {
        return method.invoke(delegate, args);
      } catch (InvocationTargetException e) {
        throw e.getTargetException();
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    long start = clock.millis();
    try
    {
      return method.invoke(delegate, args);
    } catch (InvocationTargetException e) {
      throw e.getTargetException();
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } finally {
      long elapsedMillis = clock.millis() - start;
      Duration duration = Duration.ofMillis(elapsedMillis);
      state.record(delegate.getClass(), method, duration);
    }
  }
}
