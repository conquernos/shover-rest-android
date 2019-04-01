package org.conquernos.shover.android.examples;


import org.conquernos.shover.android.Shover;
import org.conquernos.shover.android.ShoverMessage;
import org.conquernos.shover.android.exceptions.ShoverException;

import java.util.Arrays;
import java.util.Date;

public class App {

    private static final Shover shover = Shover.getInstance();

    private static final class Log extends ShoverMessage {
        public String log;

        public Log(String log) {
            this.log = log;
        }
    }

    public static void main(String[] args) throws ShoverException, InterruptedException {
        for (int i = 1; i < 132; i++) {
//            shover.send(new Log("android-test-"+i));
            Object[] values = new Object[]{"haimjoon", "android-test-" + i, "opened"};
            System.out.printf("[%s] put - %s\n", new Date(System.currentTimeMillis()), Arrays.toString(values));

            shover.send(values);
            Thread.sleep((long) (1000 * Math.sqrt(i)));
        }
    }
}
