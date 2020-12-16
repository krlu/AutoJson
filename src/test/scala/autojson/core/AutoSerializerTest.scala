package autojson.core

import java.util

import autojson.core.AutoSerializer.toJson
import example1._
import example2.{Captain, Pilot, Scientist, Spaceship}
import example3.{Assemblyman, City, Councilor, Mayor}
import example4.{Bar, Foo}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters._


class AutoSerializerTest extends AnyFlatSpec with Matchers{

  val bl: Person = new BrickLayer("Brandie", "asdf", 100)
  val in: Person = new Inspector("Ivan", "qwer", 100)
  val en: Person = new Engineer("Eugenia", "uiop", 100)
  val b1r1 = new Room()
  val b1r2 = new Room()
  val build1 = new Building("b1", Set(b1r1, b1r2).asJava)
  val b2r1 = new Room()
  val b2r2 = new Room()
  val build2 = new Building("b2", Set(b2r1, b2r2).asJava)
  val buildings: util.List[Building] = List(build1, build2).asJava
  val workers: util.Set[Person] = Set(bl, in, en).asJava
  val cs = new ConstructionSite(workers, buildings)
  "AutoSerializer" should "serialize and deserialize a java object while ignoring parameter-less methods" in {
    val csJsonString = AutoSerializer.toJson(cs)
    val gtString =
      "{\"workers\":[" +
        "{\"name\":\"Brandie\",\"id\":\"asdf\",\"age\":100,\"className\":\"example1.BrickLayer\"}," +
        "{\"name\":\"Ivan\",\"id\":\"qwer\",\"age\":100,\"className\":\"example1.Inspector\"}," +
        "{\"name\":\"Eugenia\",\"id\":\"uiop\",\"age\":100,\"className\":\"example1.Engineer\"}]," +
      "\"buildings\":[" +
        "{\"id\":\"b1\",\"rooms\":[{\"className\":\"example1.Room\"}," +
        "{\"className\":\"example1.Room\"}],\"className\":\"example1.Building\"}," +
        "{\"id\":\"b2\",\"rooms\":[{\"className\":\"example1.Room\"}," +
        "{\"className\":\"example1.Room\"}],\"className\":\"example1.Building\"}]," +
      "\"className\":\"example1.ConstructionSite\"}"
    csJsonString shouldEqual gtString
    val csFromJson: ConstructionSite = AutoDeserializer.toObject(csJsonString, classOf[ConstructionSite])
    csFromJson.workers.asScala.toList.foreach{ w =>
      assert(List("BrickLayer", "Inspector", "Engineer").contains(w.getClass.getSimpleName))
    }
    cs.workers.asScala.toList.foreach{ w =>
      assert(List("BrickLayer", "Inspector", "Engineer").contains(w.getClass.getSimpleName))
    }
    compareConstructionSites(csFromJson, cs)
  }
  
  "AutoSerializer" should "serialize and deserialize a scala object" in {
    val crewMembers = Set(new Captain("luffy", 100), new Scientist("nami", 200), new Pilot("Usop", 300))
    val spaceShip = new Spaceship(3.14, crewMembers)
    val spaceShipJsonString = AutoSerializer.toJson(spaceShip)
    val spaceShipFromJson: Spaceship = AutoDeserializer.toObject(spaceShipJsonString, classOf[Spaceship])
    spaceShip.weight shouldEqual spaceShipFromJson.weight
    spaceShip.crewMembers.map(crew => (crew.name, crew.age)) shouldEqual spaceShipFromJson.crewMembers.map(crew => (crew.name, crew.age))
  }

  "AutoSerializer" should "serialize and deserialize a scala object with an underlying java field in different package" in {
    val crewMembers = Set(new Mayor("Spike", 100), new Councilor("Faye", 200), new Assemblyman("Edward", 300))
    val city = new City("GrandLine", 1999, crewMembers, cs)
    val cityJsonString = AutoSerializer.toJson(city, prettyPrint = true)
    val cityFromJson: City = AutoDeserializer.toObject(cityJsonString, classOf[City])
    city.name shouldEqual cityFromJson.name
    city.foundingYear shouldEqual cityFromJson.foundingYear
    compareConstructionSites(city.constructionSite, cs)
  }

  "AutoSerializer" should "serialize and deserialize a class with a map structure and handle collections of objects" in {
    val foo = new Foo(Map("a" -> 1).asJava)
    val f2 = AutoDeserializer.toObject(toJson(foo), foo.getClass)
    f2.m shouldEqual foo.m
    val bar = new Bar(Map("a" -> 1))
    val b2 = AutoDeserializer.toObject(toJson(bar), bar.getClass)
    b2.m shouldEqual bar.m
    val jMap = new util.HashMap[Foo, Bar]()
    jMap.put(foo, bar)
    val sMap = Map(foo -> bar)
    val sList = List(foo, bar)
    val sSet = Set(foo, bar)
    val jList = new util.ArrayList[Object]()
    jList.add(foo)
    jList.add(bar)

    val jMapJson = AutoSerializer.toJson(jMap)
    val sMapJson = AutoSerializer.toJson(sMap)
    val sListJson = AutoSerializer.toJson(sList)
    val jListJson = AutoSerializer.toJson(jList)
    val sSetJson = AutoSerializer.toJson(sSet)

    jMapJson shouldEqual "{\"elements\":[{\"key\":{\"m\":[{\"key\":\"a\",\"value\":1}],\"className\":\"example4.Foo\"},\"value\":{\"m\":[{\"key\":\"a\",\"value\":1}],\"className\":\"example4.Bar\"}}],\"className\":\"java.util.HashMap\"}"
    sMapJson shouldEqual "{\"elements\":[{\"key\":{\"m\":[{\"key\":\"a\",\"value\":1}],\"className\":\"example4.Foo\"},\"value\":{\"m\":[{\"key\":\"a\",\"value\":1}],\"className\":\"example4.Bar\"}}],\"className\":\"scala.collection.immutable.Map1\"}"
    sListJson shouldEqual "{\"elements\":[{\"m\":[{\"key\":\"a\",\"value\":1}],\"className\":\"example4.Foo\"},{\"m\":[{\"key\":\"a\",\"value\":1}],\"className\":\"example4.Bar\"}],\"className\":\"scala.collection.immutable.List\"}"
    jListJson shouldEqual "{\"elements\":[{\"m\":[{\"key\":\"a\",\"value\":1}],\"className\":\"example4.Foo\"},{\"m\":[{\"key\":\"a\",\"value\":1}],\"className\":\"example4.Bar\"}],\"className\":\"java.util.ArrayList\"}"
    sSetJson shouldEqual "{\"elements\":[{\"m\":[{\"key\":\"a\",\"value\":1}],\"className\":\"example4.Foo\"},{\"m\":[{\"key\":\"a\",\"value\":1}],\"className\":\"example4.Bar\"}],\"className\":\"scala.collection.immutable.Set.Set2\"}"

    val restoredJMap = AutoDeserializer.toObject(jMapJson, classOf[Map[Foo, Bar]])
    restoredJMap.keySet.map(_.m) shouldEqual jMap.keySet().asScala.toSet.map{f : Foo => f.m}
    restoredJMap.values.toSet.map{b: Bar => b.m} shouldEqual jMap.values().asScala.toSet.map{b: Bar => b.m}

    val restoredSMap = AutoDeserializer.toObject(sMapJson, classOf[Map[Foo, Bar]])
    restoredSMap.keySet.map(_.m) shouldEqual sMap.keySet.map{ f : Foo => f.m}
    restoredSMap.values.toSet.map{b: Bar => b.m} shouldEqual sMap.values.toSet.map{b: Bar => b.m}

    val restoredJList: Seq[Object] = AutoDeserializer.toObject(jListJson, classOf[List[Object]])
    restoredJList.head.asInstanceOf[Foo].m shouldEqual jList.get(0).asInstanceOf[Foo].m
    restoredJList(1).asInstanceOf[Bar].m shouldEqual  jList.get(1).asInstanceOf[Bar].m

    val restoredSList: Seq[Object] = AutoDeserializer.toObject(sListJson, classOf[List[Object]])
    restoredSList.head.asInstanceOf[Foo].m shouldEqual sList.head.asInstanceOf[Foo].m
    restoredSList(1).asInstanceOf[Bar].m shouldEqual sList(1).asInstanceOf[Bar].m

    val restoredJSet = AutoDeserializer.toObject(sSetJson, classOf[Set[_]])
    restoredJSet.find(f =>f.isInstanceOf[Foo]).get.asInstanceOf[Foo].m == foo.m
    restoredJSet.find(b =>b.isInstanceOf[Bar]).get.asInstanceOf[Bar].m == bar.m
  }

  private def compareConstructionSites(cs1: ConstructionSite, cs2: ConstructionSite): Unit = {
    cs1.buildings.asScala.map(_.id) shouldEqual cs2.buildings.asScala.map(_.id)
    val result1 = cs1.workers.asScala.map(_.asInstanceOf[Worker]).map(w => (w.age, w.id, w.name, w.getClass.getSimpleName))
    val result2 = cs2.workers.asScala.map(_.asInstanceOf[Worker]).map(w => (w.age, w.id, w.name, w.getClass.getSimpleName))
    result1 shouldEqual result2
  }
}
