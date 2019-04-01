package org.conquernos.shover.android.thread.pool;


import org.conquernos.shover.android.thread.InterruptibleThreadFactory;
import org.conquernos.shover.android.thread.runnable.InterruptibleRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class InterruptibleScheduledThreadPool extends ScheduledThreadPoolExecutor {

    private List<InterruptibleRunner<?>> runners = new ArrayList<>();
    private List<ScheduledFuture<?>> futures = new ArrayList<>();

    public InterruptibleScheduledThreadPool(String poolName, int poolSize) {
        super(poolSize, new InterruptibleThreadFactory(poolName));
    }

    public void execute(InterruptibleRunner<?> runner, long initDelay, long period) {
        runners.add(runner);
        futures.add(scheduleAtFixedRate(runner, initDelay, period, TimeUnit.MILLISECONDS));
    }

    public void interruptAll() {
        for (InterruptibleRunner<?> runner : runners) {
            runner.interrupt();
        }
    }

    public void joinForRunners() throws ExecutionException, InterruptedException {
        for (ScheduledFuture<?> future : futures) {
            future.get();
        }
    }

}
