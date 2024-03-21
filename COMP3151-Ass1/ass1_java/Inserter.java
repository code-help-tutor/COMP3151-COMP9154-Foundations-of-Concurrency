WeChat: cstutorcs
QQ: 749389476
Email: tutorcs@163.com
package ass1_java;

public class Inserter extends ConcurrentSetOperator {

  private Integer x;

  public Inserter(ConcurrentSet array, Integer x) {
    super(array);
    this.x = x;
  }

  @Override
  public void run() {
    System.out.println("Insertion: " + x);
    boolean result = array.insert(x);
    System.out.println("Insertion: " + x + (result ? " inserted" : " not inserted"));
  }
}
