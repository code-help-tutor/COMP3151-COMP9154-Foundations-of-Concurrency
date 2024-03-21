WeChat: cstutorcs
QQ: 749389476
Email: tutorcs@163.com
package ass1_java;

public class Reader extends ConcurrentSetOperator {

  private Integer x;

  public Reader(ConcurrentSet array, Integer x) {
    super(array);
    this.x = x;
  }

  @Override
  public void run() {
    System.out.println("Search: " + x);
    boolean result = array.member(x);
    System.out.println("Search: " + x + (result ? " found" : " not found"));
  }
  
}
