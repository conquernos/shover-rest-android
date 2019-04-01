package org.conquernos.shover.android.kafka;


import com.google.gson.JsonObject;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RecordQueue {

    private BlockingQueue<JsonObject> queue = new LinkedBlockingQueue<>();
    private Lock lock = new ReentrantLock();


    public boolean lock() {
        return lock.tryLock();
    }

    public void unlock() {
        lock.unlock();
    }

    public void put(JsonObject record) throws InterruptedException {
        queue.put(record);
    }

    public JsonObject poll() {
        return queue.poll();
    }

    public JsonObject poll(long timeout, TimeUnit timeUnit) throws InterruptedException {
        return queue.poll(timeout, timeUnit);
    }

    public int size() {
        return queue.size();
    }

}
