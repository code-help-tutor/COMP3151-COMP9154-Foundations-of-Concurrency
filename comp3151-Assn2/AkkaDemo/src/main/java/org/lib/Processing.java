WeChat: cstutorcs
QQ: 749389476
Email: tutorcs@163.com
package org.lib;

public class Processing {
    public static String consuming(String consumer, String data, long timeToProcess) {
        try {
            Thread.sleep(timeToProcess);
        } catch (InterruptedException e) {
            System.err.printf("%s: Sleep is interrupted\n", consumer);
        }
        return data;
    }

    public static String producing(String producer, long seed, long timeToProcess) {
        try {
            Thread.sleep(timeToProcess);
        } catch (InterruptedException e) {
            System.err.printf("%s: Sleep is interrupted\n", producer);
        }
        return producer + ' ' + seed;
    }
}
