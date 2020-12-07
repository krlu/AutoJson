package autojson.core.example2;

public abstract class Worker {
    public String name;
    public String id;
    public int age;
    public Worker(String name, String id, int age){
        this.name = name;
        this.id = id;
        this.age = age;
    }
}
