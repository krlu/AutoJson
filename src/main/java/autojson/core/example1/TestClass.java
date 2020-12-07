package autojson.core.example1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestClass extends TestSuperClass implements TestInterface {
    public int a = 0;
    public int b = 0;
    public Map<Integer, String> c = new HashMap<>();
    public List<Integer> d = new ArrayList<>();
    public TestClass2 e = new TestClass2();

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
