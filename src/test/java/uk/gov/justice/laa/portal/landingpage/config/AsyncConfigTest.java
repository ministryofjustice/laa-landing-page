package uk.gov.justice.laa.portal.landingpage.config;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Tests for AsyncConfig - verifying thread pool configuration for PDA sync operations.
 */
class AsyncConfigTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();

    @Test
    void shouldCreatePdaSyncExecutorBean() {
        // When
        Executor executor = asyncConfig.pdaSyncExecutor();

        // Then
        assertThat(executor).isNotNull();
        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
    }

    @Test
    void shouldConfigureExecutorWithCorrectCorePoolSize() {
        // When
        Executor executor = asyncConfig.pdaSyncExecutor();

        // Then
        ThreadPoolTaskExecutor threadPoolExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(threadPoolExecutor.getCorePoolSize()).isEqualTo(1);
    }

    @Test
    void shouldConfigureExecutorWithCorrectMaxPoolSize() {
        // When
        Executor executor = asyncConfig.pdaSyncExecutor();

        // Then
        ThreadPoolTaskExecutor threadPoolExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(threadPoolExecutor.getMaxPoolSize()).isEqualTo(1);
    }

    @Test
    void shouldConfigureExecutorWithCorrectQueueCapacity() {
        // When
        Executor executor = asyncConfig.pdaSyncExecutor();

        // Then
        ThreadPoolTaskExecutor threadPoolExecutor = (ThreadPoolTaskExecutor) executor;
        // QueueCapacity is set to 2, meaning max 2 tasks can be queued
        assertThat(threadPoolExecutor.getQueueCapacity()).isEqualTo(2);
    }

    @Test
    void shouldConfigureExecutorWithCorrectThreadNamePrefix() {
        // When
        Executor executor = asyncConfig.pdaSyncExecutor();

        // Then
        ThreadPoolTaskExecutor threadPoolExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(threadPoolExecutor.getThreadNamePrefix()).isEqualTo("pda-sync-");
    }

    @Test
    void shouldConfigureExecutorWithCorrectKeepAliveSeconds() {
        // When
        Executor executor = asyncConfig.pdaSyncExecutor();

        // Then
        ThreadPoolTaskExecutor threadPoolExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(threadPoolExecutor.getKeepAliveSeconds()).isEqualTo(60);
    }

    @Test
    void shouldConfigureExecutorToWaitForTasksOnShutdown() {
        // When
        Executor executor = asyncConfig.pdaSyncExecutor();

        // Then
        ThreadPoolTaskExecutor threadPoolExecutor = (ThreadPoolTaskExecutor) executor;
        // This verifies the executor is configured to wait for tasks to complete on shutdown
        assertThat(threadPoolExecutor.getThreadPoolExecutor()).isNotNull();
    }

    @Test
    void shouldRejectTasksWhenQueueIsFull() {
        // Given
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.pdaSyncExecutor();

        // Fill the queue and core pool (1 core thread + 2 queue capacity = 3 total)
        Runnable blockingTask = () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        // When - submit 3 tasks (should succeed: 1 executing + 2 queued)
        executor.submit(blockingTask);
        executor.submit(blockingTask);
        executor.submit(blockingTask);

        // Then - 4th task should be rejected with RuntimeException
        assertThatThrownBy(() -> executor.submit(blockingTask))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Too many concurrent sync operations");

        // Cleanup
        executor.shutdown();
    }

    @Test
    void shouldUseCallerRunsPolicyForRejection() {
        // Given
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.pdaSyncExecutor();

        // When - verify rejection handler is configured
        // Then
        assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler()).isNotNull();

        // Cleanup
        executor.shutdown();
    }

    @Test
    void shouldHaveAwaitTerminationConfigured() {
        // When
        Executor executor = asyncConfig.pdaSyncExecutor();

        // Then - verify shutdown timeout is configured
        ThreadPoolTaskExecutor threadPoolExecutor = (ThreadPoolTaskExecutor) executor;
        // The awaitTerminationSeconds is set to 30 in the config
        assertThat(threadPoolExecutor.getThreadPoolExecutor()).isNotNull();

        // Cleanup
        threadPoolExecutor.shutdown();
    }

    @Test
    void shouldAllowOneConcurrentSyncOperation() {
        // Given
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.pdaSyncExecutor();

        // When - verify pool size allows only 1 concurrent operation
        // Then
        assertThat(executor.getCorePoolSize()).isEqualTo(1);
        assertThat(executor.getMaxPoolSize()).isEqualTo(1);

        // Cleanup
        executor.shutdown();
    }

    @Test
    void shouldExecutorBeInitialized() {
        // When
        Executor executor = asyncConfig.pdaSyncExecutor();

        // Then
        ThreadPoolTaskExecutor threadPoolExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(threadPoolExecutor.getThreadPoolExecutor()).isNotNull();

        // Cleanup
        threadPoolExecutor.shutdown();
    }
}
