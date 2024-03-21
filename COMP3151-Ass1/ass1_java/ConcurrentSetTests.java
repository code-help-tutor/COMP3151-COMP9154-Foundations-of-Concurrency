WeChat: cstutorcs
QQ: 749389476
Email: tutorcs@163.com
package ass1_java;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ConcurrentSetTests {
  @Test
  public void TestInsert() {

    int N = 10;

    ConcurrentSet array = new ConcurrentSet(N);
    Thread[] threads = new Thread[N];
    for (int i = 0; i < N; i++) {
      threads[i] = new Thread(new Inserter(array, i));
    }
    for (int i = 0; i < N; i++) {
      threads[i].start();
    }
    for (int i = 0; i < N; i++) {
      try {
        threads[i].join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    array.printSorted();
  }

  @Test
  public void TestMember() {
    int N = 10;

    ConcurrentSet array = new ConcurrentSet(N);

    Thread[] threads = new Thread[N];

    for (int i = 0; i < N; i += 2) {
      threads[i] = new Thread(new Inserter(array, i));
    }
    for (int i = 0; i < N; i += 2) {
      threads[i].start();
    }
    for (int i = 0; i < N; i += 2) {
      try {
        threads[i].join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    Thread sortedPrinter = new Thread(new SortedPrinter(array));
    sortedPrinter.start();
    try {
      sortedPrinter.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    for (int i = 0; i < N; i++) {
      threads[i] = new Thread(new Reader(array, i));
    }

    for (int i = 0; i < N; i++) {
      threads[i].start();
    }

    for (int i = 0; i < N; i++) {
      try {
        threads[i].join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  @Test
  public void TestInsertAndMember() {
    int N = 1000;

    ConcurrentSet array = new ConcurrentSet(N);

    List<Thread> inserterThreads = new ArrayList<Thread>(N);
    List<Thread> memberThreads = new ArrayList<Thread>(N);

    for (int i = 0; i < N; i++) {
      inserterThreads.add(new Thread(new Inserter(array, i)));
    }

    for (int i = 0; i < N; i++) {
      memberThreads.add(new Thread(new Reader(array, i)));
    }

    // interlace the threads so they run concurrently

    // array.printSorted();

    for (int i = 0; i < N; i++) {
      inserterThreads.get(i).start();
      // try {
      //   inserterThreads.get(i).join();
      // } catch (InterruptedException e) { 
      //   e.printStackTrace();
      // }
        memberThreads.get(i).start();
        // try {
        //   memberThreads.get(i).join();
        // } catch (InterruptedException e) { 
        //   e.printStackTrace();
        // }

    }

    for (int i = 0; i < N; i++) {
      try {
        inserterThreads.get(i).join();
        memberThreads.get(i).join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    array.printSorted();

    assert(array.printSorted().size() == N);

  }
    
}