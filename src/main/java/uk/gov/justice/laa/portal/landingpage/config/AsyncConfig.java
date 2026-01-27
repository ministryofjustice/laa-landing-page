package uk.gov.justice.laa.portal.landingpage.config;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for async execution with isolated thread pool for PDA sync operations.
 * This ensures that long-running sync operations don't impact the main application.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Dedicated thread pool for PDA sync operations.
     * Configured with:
     * - Single core thread to prevent resource exhaustion
     * - Queue to handle concurrent requests
     * - Caller runs policy to provide backpressure
     * - Timeout for idle threads
     */
    @Bean(name = "pdaSyncExecutor")
    public Executor pdaSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Only allow 1 sync operation at a time
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);

        // Queue up to 2 additional requests
        executor.setQueueCapacity(2);

        // Thread naming for easy identification in logs
        executor.setThreadNamePrefix("pda-sync-");

        // Timeout for idle threads
        executor.setKeepAliveSeconds(60);

        // Rejection policy: caller runs (provides backpressure)
        executor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                log.warn("PDA sync task rejected - too many concurrent sync operations");
                throw new RuntimeException("Too many concurrent sync operations. Please try again later.");
            }
        });

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }
}
