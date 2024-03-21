WeChat: cstutorcs
QQ: 749389476
Email: tutorcs@163.com
package ass1_java;

public abstract class ConcurrentSetOperator implements Runnable {
  protected ConcurrentSet array;
  
  public ConcurrentSetOperator(ConcurrentSet array) {
    this.array = array;
  }
}
