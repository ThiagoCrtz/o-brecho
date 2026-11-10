package br.org.ang.brecho.application.port;

public interface LoginThrottle {
  boolean allow(String key, int limit);
}
