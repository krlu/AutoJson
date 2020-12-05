import java.util.HashMap;
import java.util.Map;

public class TestClass extends TestSuperClass implements TestInterface{
    public int a = 0;
    public int b = 0;
    public Map<Integer, Integer> c = new HashMap<>();

    public TestClass(int a, int b){
//        this.c.add(1);
//        this.c.add(2);
        this.a = a;
        this.b = b;
    }

    public int getA(){
        return a;
    }
}
