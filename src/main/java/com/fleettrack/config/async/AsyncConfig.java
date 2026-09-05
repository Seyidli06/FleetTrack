package com.fleettrack.config.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration(proxyBeanMethods = false)
@EnableAsync
public class AsyncConfig {

    public static final String MAINTENANCE_REMINDER_EXECUTOR =
            "maintenanceReminderExecutor";

    @Bean(name = MAINTENANCE_REMINDER_EXECUTOR)
    public Executor maintenanceReminderExecutor() {

        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);

        executor.setThreadNamePrefix(
                "maintenance-reminder-"
        );

        executor.setWaitForTasksToCompleteOnShutdown(
                true
        );

        executor.setAwaitTerminationSeconds(
                30
        );

        /*
         * Queue dolarsa task itmir.
         *
         * Scheduler thread task-i özü icra edir və
         * sistemə təbii backpressure verir.
         */
        executor.setRejectedExecutionHandler(
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        executor.initialize();

        return executor;
    }
}