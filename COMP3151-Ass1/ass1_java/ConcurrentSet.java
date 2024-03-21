WeChat: cstutorcs
QQ: 749389476
Email: tutorcs@163.com
package ass1_java;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A concurrent array data structure that supports concurrent searches, insertions, deletes, and sorted printing.
 * 
 * It is 
 */
public class ConcurrentSet {

  private int N;
  private List<Integer> arr;
  private Semaphore capacity;
  private List<ReadWriteLock> locks;
  private List<Semaphore> insertion_mutex;

  public ConcurrentSet(int N) {
    this.N = N;
    arr = new ArrayList<>(N);
    capacity = new Semaphore(N, true);
    locks = new ArrayList<>(N);
    insertion_mutex = new ArrayList<>(N);

    for (int i = 0; i < N; i++) {
      arr.add(-1);
      locks.add(new ReentrantReadWriteLock(true));
      insertion_mutex.add(new Semaphore(1, true));
    }
  }

  private void shiftValue(int from, int to) {

    assert(0 <= from && from < N);
    assert(0 <= to && to < N);

    Lock fromLock = locks.get(from).writeLock();
    Lock toLock = locks.get(to).writeLock();

    while (true) {
      fromLock.lock();
      if (toLock.tryLock()) break;
      fromLock.unlock();
    }

    arr.set(to, arr.get(from));
    arr.set(from, -1);

    fromLock.unlock();
    toLock.unlock();
  }

  private void shiftLeft(int index) {
    shiftValue(index, index - 1);
  }

  private void shiftRight(int index) {
    shiftValue(index, index + 1);
  }

  private void insertIndex(int index, int value) {

    Lock insertionLock = locks.get(index).writeLock();

    insertionLock.lock();
    arr.set(index, value);
    insertionLock.unlock();
  }

  private boolean deleteIndex(int index) {

    Lock deletionLock = locks.get(index).writeLock();

    deletionLock.lock();

    boolean successfulDeletion = arr.get(index) != -1;
    arr.set(index, -1);

    deletionLock.unlock();

    return successfulDeletion;
  }

  private int readIndex(int index) {

    locks.get(index).readLock().lock();

    int value = arr.get(index);

    locks.get(index).readLock().unlock();

    return value;
  }

  /**
   * Implements binary search on the array to search for a number 
   * @param x - the number to search for
   * @return true if the number is found, false otherwise
   */
  public boolean member(int x) {

    // Perform the initial setup for the binary search

    int L = 0;
    int R = N - 1;
    int M;

    Lock LLock = locks.get(L).readLock();
    Lock RLock = locks.get(R).readLock();
    Lock MLock;

    int MValue;

    LLock.lock();
    RLock.lock();

    if (arr.get(L) == x || arr.get(R) == x) {
      LLock.unlock();
      RLock.unlock();
      return true;
    }

    while (true) {

      // System.out.println("Searching for " + x + " L: " + L + ", R: " + R);

      // lock the middle element
      int MGuessLeft = (int)Math.floor((L + R) / 2);
      int MGuessRight = MGuessLeft + 1;

      locks.get(MGuessLeft).readLock().lock();
      locks.get(MGuessRight).readLock().lock();

      // search both directions until the number is found or the search space is exhausted
      while (true) {

        assert(L < R);

        boolean endLeft = MGuessLeft == L;
        boolean endRight = MGuessRight == R;

        if (endLeft && endRight) {

          locks.get(MGuessLeft).readLock().unlock();
          locks.get(MGuessRight).readLock().unlock();
          LLock.unlock();
          RLock.unlock();
          return false;
        }
        
        // Move the pointer left
        if (!endLeft) {
          if (arr.get(MGuessLeft) != -1) {
            M = MGuessLeft;
            break;
          }

          locks.get(MGuessLeft - 1).readLock().lock();
          locks.get(MGuessLeft).readLock().unlock();

          MGuessLeft--;
          
        }

        if (!endRight) {
          if (arr.get(MGuessRight) != -1) {
            M = MGuessRight;
            break;
          }

          locks.get(MGuessRight + 1).readLock().lock();
          locks.get(MGuessRight).readLock().unlock();
          MGuessRight++;
        }
      }

      // We have identified a non-empty element at position M
      assert(M == MGuessLeft || M == MGuessRight);
      assert(L < M);
      assert(M < R);

      MLock = locks.get(M).readLock();
      MValue = arr.get(M);

      MLock.lock();
      locks.get(MGuessLeft).readLock().unlock();
      locks.get(MGuessRight).readLock().unlock();

      // We have identified a non-empty element at position M

      assert(MValue != -1);

      if (MValue == x) {
        // The number is found
        MLock.unlock();
        LLock.unlock();
        RLock.unlock();
        return true;
      } else if (MValue < x) {
        // The number must be further to the right
        LLock.unlock();

        LLock = MLock;
        L = M;
      } else {
        // The number must be further to the left
        RLock.unlock();

        RLock = MLock;
        R = M;
      }
    }
  }

  /**
   * Inserts a number into the array
   * @param x - the number to insert
   * @return true if the number is inserted, false otherwise
   */
  public boolean insert(int x) {

    capacity.acquireUninterruptibly();

    int L = -1;
    int R = -1;

    while (true) {
      // get R + 1

      int nextValue;

      if (R == N - 1) {
        assert(L != -1);
        nextValue = Integer.MAX_VALUE; // forces left insertion
      } else {
        insertion_mutex.get(R + 1).acquireUninterruptibly();
        nextValue = arr.get(R + 1);
      }

      if (R != -1 && R != L) insertion_mutex.get(R).release();

      if (nextValue == -1) {

        // A new empty slot is found, release the mutexes and update it to the new space
        if (L != -1) insertion_mutex.get(L).release();
        
        R = R + 1;
        L = R;

      } else if (nextValue < x) {

        // The insertion point must further to the right
        R = R + 1;

      } else if (nextValue == x) {

        // The number is already in the array, no insertion occurs
        capacity.release();

        // release the mutexes
        insertion_mutex.get(R + 1).release();
        if (L != -1) insertion_mutex.get(L).release();

        return false;

      } else if (nextValue > x) {
        
        // The insertion point is found, insert the number between R and R + 1

        if (L != -1) {
          
          // We have an empty slot reserved at L
          // shift all the numbers between L + 1 and R to the left in ascending order
          for (int i = L + 1; i <= R; i++) shiftLeft(i);

          // insert the number at R
          insertIndex(R, x);

          // release the mutexes
          if (R != N - 1) insertion_mutex.get(R + 1).release();
          insertion_mutex.get(L).release();

          return true;

        } else {

          // We don't have an empty slot reserved, search to the right for an empty slot
          int k = 2;

          while (true) {

            assert(R + k < N);
            insertion_mutex.get(R + k).acquireUninterruptibly();
            if (k != 2) insertion_mutex.get(R + k - 1).release();
            
            if (readIndex(R + k) == -1) {

              // An empty slot is found at R + k
              // shift all the numbers between R + 1 and R + k - 1 to the right in descending order
              for (int i = R + k - 1; i >= R + 1; i--) shiftRight(i);

              // insert the number at R + 1
              insertIndex(R + 1, x);

              // release the mutexes

              insertion_mutex.get(R + k).release();
              insertion_mutex.get(R + 1).release();

              return true;
            }
            k++;
          }
        }
      }
    }
  }

  /**
   * Deletes a number from the array
   * @param x - the number to delete
   * @return true if the number is deleted, false otherwise
   */
  public boolean delete(int x) {
    return false;
  }

  /**
   * Prints the array in sorted order
   * @return true
   */
  public List<Integer> printSorted() {

    List<Integer> sorted = new ArrayList<Integer>();

    for (int i = 0; i < N; i++) {

      // acquire the next lock, then release the previous lock
      locks.get(i).readLock().lock();
      if (i != 0) locks.get(i - 1).readLock().unlock();

      // read the next value and add it to the sorted list
      int nextValue = arr.get(i);

      if (nextValue != -1) {
        sorted.add(nextValue);
      }

    }

    // release the last lock
    locks.get(N - 1).readLock().unlock();

    System.out.println(sorted);

    return sorted;

  }
}
