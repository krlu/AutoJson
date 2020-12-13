package example3

import example1.ConstructionSite

class City(val name: String, val foundingYear: Int, val members: Set[GovernmentMember], val constructionSite: ConstructionSite)

trait GovernmentMember{
  val name: String
  val timeInOffice: Int
}

class Mayor(override val name: String, override val timeInOffice: Int) extends GovernmentMember
class Councilor(override val name: String, override val timeInOffice: Int) extends GovernmentMember
class Assemblyman(override val name: String, override val timeInOffice: Int) extends GovernmentMember