package autojson.core.example;

public abstract class Worker implements Person{
    public String name;
    public String id;
    public int age;
    public Worker(String name, String id, int age){
        this.name = name;
        this.id = id;
        this.age = age;
    }
}
