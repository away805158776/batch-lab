package com.example.batchlab.config;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Batch 全局配置类。
 * 通过在此静态声明 JobRegistryBeanPostProcessor，
 * 避免了 Spring Batch 自动配置中非静态声明导致的早期注入警告（BeanPostProcessorChecker WARN）。
 */
@Configuration
public class BatchConfig {

    @Bean
    public static JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor(JobRegistry jobRegistry) {
        JobRegistryBeanPostProcessor postProcessor = new JobRegistryBeanPostProcessor();
        postProcessor.setJobRegistry(jobRegistry);
        return postProcessor;
    }
}
