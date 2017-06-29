package org.clapper.peoplegen

class PeopleGeneratorSpec extends BaseSpec {
  val BaseParams = Params(
    femalePercent = 50,
    malePercent   = 50,
    totalPeople   = 100
  )

  "generatePeople" should "make the requested number of people" in {
    val gen = new PeopleGenerator(params = BaseParams,
                                  msg    = EmptyMessageHandler)

    val t = gen.generatePeople
    t shouldBe 'success
    val people = t.get
    people.size shouldBe BaseParams.totalPeople
  }

  it should "make an equal number of M and F records by default" in {
    val gen = new PeopleGenerator(params = BaseParams,
                                  msg    = EmptyMessageHandler)

    val t = gen.generatePeople
    t shouldBe 'success
    val people = t.get
    val (females, males) = people partition { _.gender == Gender.Female }
    females.size shouldBe males.size
  }

  it should "make all F records if requested" in {
    val gen = new PeopleGenerator(
      params = BaseParams.copy(femalePercent = 100, malePercent = 0),
      msg    = EmptyMessageHandler
    )

    val t = gen.generatePeople
    t shouldBe 'success
    val people = t.get
    val (females, males) = people partition { _.gender == Gender.Female }
    females.size shouldBe BaseParams.totalPeople
    males.size shouldBe 0
  }

  it should "make all M records if requested" in {
    val gen = new PeopleGenerator(
      params = BaseParams.copy(malePercent = 100, femalePercent = 0),
      msg    = EmptyMessageHandler
    )

    val t = gen.generatePeople
    t shouldBe 'success
    val people = t.get
    val (females, males) = people partition { _.gender == Gender.Female }
    males.size shouldBe BaseParams.totalPeople
    females.size shouldBe 0
  }

  it should "make an uneven distribution of females to males if requested" in {

    for (malePercent <- Seq(10, 30, 90, 66)) {
      val params = BaseParams.copy(malePercent   = malePercent,
                                   femalePercent = 100 - malePercent,
                                   totalPeople   = 1000)
      val gen = new PeopleGenerator(params = params, msg = EmptyMessageHandler)

      val t = gen.generatePeople
      t shouldBe 'success
      val people = t.get
      val (females, males) = people partition { _.gender == Gender.Female }

      val expectedMales = (params.totalPeople * params.malePercent) / 100
      males.size shouldBe expectedMales

      val expectedFemales = (params.totalPeople * params.femalePercent) / 100
      females.size shouldBe expectedFemales
    }
  }
}
