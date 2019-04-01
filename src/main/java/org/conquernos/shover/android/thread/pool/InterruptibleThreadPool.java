package org.conquernos.shover.android.thread.pool;


import org.conquernos.shover.android.thread.InterruptibleThreadFactory;
import org.conquernos.shover.android.thread.runnable.InterruptibleRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class InterruptibleThreadPool extends ThreadPoolExecutor {

    private List<InterruptibleRunner<?>> runners = new ArrayList<>();
    private List<Future<Object>> futures = new ArrayList<>();

    public InterruptibleThreadPool(String poolName, int poolSize) {
        super(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS
            , new LinkedBlockingQueue<>()
            , new InterruptibleThreadFactory(poolName));
    }

    public void execute(InterruptibleRunner<?> runner) {
        runners.add(runner);
        futures.add(submit(runner, null));
    }

    public void interruptAll() {
        for (InterruptibleRunner<?> runner : runners) {
            runner.interrupt();
        }
    }

    public void joinForRunners() {
        for (Future<Object> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

}
