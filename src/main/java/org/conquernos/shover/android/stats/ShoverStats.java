package org.conquernos.shover.android.stats;


import java.util.concurrent.atomic.AtomicLong;

public class ShoverStats {

    private final AtomicLong numberOfMessages = new AtomicLong(0);
    private final AtomicLong numberOfCompletedMessages = new AtomicLong(0);
    private final AtomicLong numberOfFailedMessages = new AtomicLong(0);


    public long getNumberOfMessages() {
        return numberOfMessages.get();
    }

    public long getNumberOfCompletedMessages() {
        return numberOfCompletedMessages.get();
    }

    public long getNumberOfFailedMessages() {
        return numberOfFailedMessages.get();
    }

    public long addNumberOfMessages() {
        return addNumberOfMessages(1);
    }

    public long addNumberOfMessages(long number) {
        return numberOfMessages.addAndGet(number);
    }

    public long addNumberOfCompletedMessages() {
        return addNumberOfCompletedMessages(1);
    }

    public long addNumberOfCompletedMessages(long number) {
        return numberOfCompletedMessages.addAndGet(number);
    }

    public long addNumberOfFailedMessages() {
        return addNumberOfFailedMessages(1);
    }

    public long addNumberOfFailedMessages(long number) {
        return numberOfFailedMessages.addAndGet(number);
    }

    @Override
    public String toString() {
        return "ShoverStats { " +
            "numberOfMessages=" + numberOfMessages +
            ", numberOfCompletedMessages=" + numberOfCompletedMessages +
            ", numberOfFailedMessages=" + numberOfFailedMessages +
            " }";
    }

}
