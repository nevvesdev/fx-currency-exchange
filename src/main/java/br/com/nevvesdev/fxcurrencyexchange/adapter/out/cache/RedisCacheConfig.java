package br.com.nevvesdev.fxcurrencyexchange.adapter.out.cache;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FxCacheProperties.class)
class RedisCacheConfig {
}