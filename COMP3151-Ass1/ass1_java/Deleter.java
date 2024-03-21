WeChat: cstutorcs
QQ: 749389476
Email: tutorcs@163.com
package ass1_java;

public class Deleter extends ConcurrentSetOperator {

  private Integer x;

  public Deleter(ConcurrentSet array, Integer x) {
    super(array);
    this.x = x;
  }

  @Override
  public void run() {
    System.out.println("Deletion: " + x);
    boolean result = array.delete(x);
    System.out.println("Deletion: " + x + (result ? " found" : " not found"));
  }
}
