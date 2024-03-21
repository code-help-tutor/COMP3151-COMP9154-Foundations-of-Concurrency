WeChat: cstutorcs
QQ: 749389476
Email: tutorcs@163.com
package ass1_java;

import ass1_java.ConcurrentSet;
import ass1_java.ConcurrentSetOperator;

public class SortedPrinter extends ConcurrentSetOperator {

  public SortedPrinter(ConcurrentSet array) {
    super(array);
  }

  @Override
  public void run() {
    System.out.println("Printing Sorted Array");
    array.printSorted();
  }
  
}
