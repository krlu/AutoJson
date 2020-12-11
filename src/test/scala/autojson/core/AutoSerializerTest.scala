package autojson.core

import java.util

import autojson.core.example._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters._


class AutoSerializerTest extends AnyFlatSpec with Matchers{
  "AutoSerializer" should "serialize and deserialize a construction site object" in {
    val bl: Person = new BrickLayer("Brandie", "asdf", 100)
    val in: Person = new Inspector("Ivan", "qwer", 100)
    val en: Person = new Engineer("Eugenia", "uiop", 100)
    val b1r1 = new Room()
    val b1r2 = new Room()
    val build1 = new Building("b1", Set(b1r1, b1r2).asJava)
    val b2r1 = new Room()
    val b2r2 = new Room()
    val build2 = new Building("b2", Set(b2r1, b2r2).asJava)
    val buildings = Set(build1, build2).asJava
    val workers = Set(bl, in, en).asJava
    val cs = new ConstructionSite(workers, buildings)
    val csJsonString = AutoSerializer.toJson(cs)
    val gtString = "{\"workers\":[{\"name\":\"Brandie\",\"id\":\"asdf\",\"age\":100,\"className\":\"BrickLayer\"},{\"name\":\"Ivan\",\"id\":\"qwer\",\"age\":100,\"className\":\"Inspector\"},{\"name\":\"Eugenia\",\"id\":\"uiop\",\"age\":100,\"className\":\"Engineer\"}],\"buildings\":[{\"id\":\"b1\",\"rooms\":[{\"className\":\"Room\"},{\"className\":\"Room\"}],\"className\":\"Building\"},{\"id\":\"b2\",\"rooms\":[{\"className\":\"Room\"},{\"className\":\"Room\"}],\"className\":\"Building\"}],\"className\":\"ConstructionSite\"}"
    csJsonString shouldEqual gtString
    val map = AutoSerializer.toMap(cs)
    val csFromJson = AutoSerializer.mapToObject(map, classOf[ConstructionSite], packageName = "autojson.core.example")
    csFromJson.workers.asScala.toList.foreach{ w =>
      assert(List("BrickLayer", "Inspector", "Engineer").contains(w.getClass.getSimpleName))
    }
    cs.workers.asScala.toList.foreach{ w =>
      assert(List("BrickLayer", "Inspector", "Engineer").contains(w.getClass.getSimpleName))
    }
    csFromJson.buildings.asScala.map(_.id) shouldEqual cs.buildings.asScala.map(_.id)
    csFromJson.workers.asScala.map(_.asInstanceOf[Worker]).map(w => (w.age, w.id, w.name, w.getClass.getSimpleName)) shouldEqual cs.workers.asScala.map(_.asInstanceOf[Worker]).map(w => (w.age, w.id, w.name, w.getClass.getSimpleName))
  }
}
