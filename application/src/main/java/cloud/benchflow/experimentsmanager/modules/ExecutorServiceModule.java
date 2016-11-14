package cloud.benchflow.experimentsmanager.modules;

import cloud.benchflow.experimentsmanager.configurations.ExperimentsManagerConfiguration;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;

import javax.annotation.Nonnull;
import javax.inject.Named;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *         <p>
 *         Created on 07/03/16.
 */
public class ExecutorServiceModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    @Named("runBenchmarkExecutorService")
    public ExecutorService providesRunExperimentExecutorService(ExperimentsManagerConfiguration c, Environment env) {
        int cpus = Runtime.getRuntime().availableProcessors();

        return env.lifecycle().executorService("run-benchmark-%d")
                .minThreads(5 * cpus) //TODO: make base min value configurable
                .maxThreads(15 * cpus) //TODO: make base max value configurable
                .keepAliveTime(Duration.seconds(60))
                .workQueue(new SynchronousQueue<>())
                .threadFactory(new DaemonThreadFactory())
                .rejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy())
                .build();
    }

    @Provides
    @Singleton
    @Named("submitRunExecutorService")
    public ExecutorService providesSubmitRunExecutorService(ExperimentsManagerConfiguration c, Environment env) {
        int cpus = Runtime.getRuntime().availableProcessors();

        return env.lifecycle().executorService("submit-run-%d")
                .minThreads(5 * cpus) //TODO: make base min value configurable
                .maxThreads(25 * cpus) //TODO: make base max value configurable
                .keepAliveTime(Duration.seconds(60))
                .workQueue(new SynchronousQueue<>())
                .threadFactory(new DaemonThreadFactory())
                .rejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy())
                .build();
    }

    /**
     * See http://dev.bizo.com/2014/06/cached-thread-pool-considered-harmlful.html
     */
    public static class DaemonThreadFactory implements ThreadFactory {

        private AtomicInteger count = new AtomicInteger();

        @Override
        public Thread newThread(@Nonnull Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("experiments-manager-" + count.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }

}
