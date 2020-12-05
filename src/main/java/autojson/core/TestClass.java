package autojson.core;

import java.util.HashMap;
import java.util.Map;

public class TestClass extends TestSuperClass implements TestInterface {
    public int a = 0;
    public int b = 0;
    public Map<Integer, String> c = new HashMap<>();

    public TestClass(int a, int b){
        this.c.put(1, "1");
        this.c.put(2, "2");
        this.a = a;
        this.b = b;
    }

    public int getA(){
        return a;
    }
}
