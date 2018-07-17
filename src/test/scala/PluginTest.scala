import org.scalatest._


class PluginTest extends FreeSpec {
  val tests = new TestTools(1, debug = false)
  import tests._

  // Run all tests in "Tests.md".
  parse("Tests.md")
    //.filterCategory("Type Parameters")
    .onlyFor(Success, { (name, test) =>
      name in success(testCase(test))
    })
    .onlyFor(Warning, { (name, test) =>
      name in warning(testCase(test))
    })
    .onlyFor(Failure, { (name, test) =>
      name in failure(testCase(test))
    })

  /*
  "dont get it"  in {
    success(testCase("sldkjfsf",
    """
      |val x: `:Wine` = iri"PeterMccoyChardonnay"
    """.stripMargin))

  }

  "whaaaaaat" in {
    success(testCase("plsgodno",
    """
      |class Thing[A](a: A*)
      |val wine1: `:WhiteWine & (#E :hasColor.#t)` = iri"PeterMccoyChardonnay"
      |val awine = new Thing(wine1)
      |val awine2: Thing[`:Wine`] = awine
    """.stripMargin))
  }

  "test: foreach" in {
    success(testCase("Test0",
      """
        |val rw = iri"PageMillWineryCabernetSauvignon"
        |val ww = iri"PeterMccoyChardonnay"
        |
        |val lst = sparql"SELECT ?m WHERE { $rw :hasMaker ?m }"
        |
        |val t = lst.foreach { row =>
        |  println(row)
        |}
      """.stripMargin))
  }

  // MISSION: MULTIPLE PARAMETER LISTS (BEGIN)

  "mpl: just types" in {
    success(testCase("Test0",
      """
        |val rw = iri"PageMillWineryCabernetSauvignon"
        |val ww = iri"PeterMccoyChardonnay"
        |
        |def method(r: `:RedWine`)(w: `:WhiteWine`): List[`:Wine`] = List(r,w)
        |
        |val t: List[`#t`] = method(rw)(ww)
      """.stripMargin))
  }

  "mpl: inferred return type" in {
    success(testCase("Test0",
      """
        |val rw = iri"PageMillWineryCabernetSauvignon"
        |val ww = iri"PeterMccoyChardonnay"
        |
        |def method(r: `:RedWine`)(w: `:WhiteWine`) = List(r,w)
        |
        |val t: List[`#t`] = method(rw)(ww)
      """.stripMargin))
  }

  "mpl: inferred return type (3 lists)" in {
    success(testCase("Test0",
      """
        |val rw = iri"PageMillWineryCabernetSauvignon"
        |val ww = iri"PeterMccoyChardonnay"
        |
        |def method(r: `:RedWine`)(w1: `:WhiteWine`)(w2: `:WhiteWine`) = List(r,w1,w2)
        |
        |val t: List[`#t`] = method(rw)(ww)(ww)
      """.stripMargin))
  }

  "mpl: type arguments (one, two lists)" in {
    success(testCase("Test0",
      """
        |val rw = iri"PageMillWineryCabernetSauvignon"
        |val ww = iri"PeterMccoyChardonnay"
        |
        |def method[T](t1: T)(t2: T): List[T] = List(t1, t2)
        |
        |val t: List[`#t`] = method(rw)(ww)
      """.stripMargin))
  }

  "mpl: type arguments (two, two lists)" in {
    success(testCase("Test0",
      """
        |val rw = iri"PageMillWineryCabernetSauvignon"
        |val ww = iri"PeterMccoyChardonnay"
        |
        |def method[A,B](a: A)(b: B)(a2: A): Tuple2[A,B] = (a,b)
        |
        |val t: Tuple2[`#t`,`#t`] = method(rw)(ww)(rw)
      """.stripMargin))
  }

  // TODO: BROKEN?
  "mpl: application" in {
    success(testCase("Test0",
      """
        |val ww = iri"PeterMccoyChardonnay"
        |
        |def doApply(f: `:Wine` => `:Wine`, x: `:WhiteWine`): `:Wine` = f(x)
        |def f(x: `:WhiteWine`): `:WhiteWine` = x
        |
        |//val fn: `:Wine` => `:Wine` = f _
        |
        |val t: `#t` = doApply(f, ww)
      """.stripMargin))
  }

  // TODO: BROKEN?
  "mpl: mapping" in {
    success(testCase("Test0",
      """
        |val rw = iri"PageMillWineryCabernetSauvignon"
        |val ww = iri"PeterMccoyChardonnay"
        |
        |val lst = List(rw, ww)
        |
        |def id(x: `:WhiteWine`): Unit = Unit
        |
        |val t = lst.foreach(id)
      """.stripMargin))
  }

  "mpl: ORDINARY TYPES - type arguments (two, two lists)" in {
    success(testCase("Test0",
      """
        |val rw = 19
        |val ww = "ninteen"
        |
        |def method[A,B](a: A)(b: B): Tuple2[A,B] = (a,b)
        |
        |val t: Tuple2[Any, Any] = method(rw)(ww)
      """.stripMargin))
  }

  // MISSION: MULTIPLE PARAMETER LISTS (END)







  // MISSION: TYPES FROM QUERIES (BEGIN)

  "query test" in {
    success(testCase("Test0",
      """
        |val w: `:Wine` = iri"PeterMccoyChardonnay"
        |val x: List[`<xsd:integer>`] = sparql"SELECT ?i WHERE { $w :hasVintageYear ?y. ?y :yearValue ?i }"
      """.stripMargin))
  }

  // MISSION: TYPES FROM QUERIES (END)






  // MISSION: SELECT - PHASE 2 (BEGIN)

  /*object: Main.this.lst method: head
  is method? true
  is param/skolem? true
  tree - Main.this.lst.head
    :  tpe - de.uni_koblenz.dltypes.runtime.DLType
  tree symbol - method head
    :  tpe - => A
  q - Main.this.lst
    :  tpe - List
  q symbol - value lst
    :  tpe - => List[de.uni_koblenz.dltypes.runtime.DLType @de.uni_koblenz.dltypes.runtime.dl("(<:WhiteWine>⊔<:WhiteWine>)")]

  >>>>> Other case in SELECT!
  ----- Main.this.lst . head
     :: de.uni_koblenz.dltypes.runtime.DLType
  ----- [Select] Main.this.lst head ----------------------------------------------------------------------------------*/

/*>>> --------------------------------
>>>>> paramTypes List(A)
>>>>> args List(de.uni_koblenz.dltypes.runtime.DLType @de.uni_koblenz.dltypes.runtime.dl("(<:WhiteWine>⊔<:WhiteWine>)"))
>>>>> formal A
>>>>> returnTpe de.uni_koblenz.dltypes.runtime.DLType
>>>>> ------------------------------*/
  "ms: method (0)" in {
    success(testCase("test",
    """
      |val ww: `:WhiteWine` = iri"PeterMccoyChardonnay"
      |val lst = List(ww, ww)
      |val h: `#t` = lst.head
    """.stripMargin))
  }

  /*object: Main.this.lst method: drop
  is method? true
  is param/skolem? true
  tree - Main.this.lst.drop
    :  tpe - (n: Int)List[de.uni_koblenz.dltypes.runtime.DLType]
  tree symbol - method drop
    :  tpe - (n: Int)List[A]
  q - Main.this.lst
    :  tpe - List
  q symbol - value lst
    :  tpe - => List[de.uni_koblenz.dltypes.runtime.DLType @de.uni_koblenz.dltypes.runtime.dl("(<:WhiteWine>⊔<:WhiteWine>)")]

  >>>>> METHOD ( List(value n) )-> List[de.uni_koblenz.dltypes.runtime.DLType]
  ----- Main.this.lst . drop
     :: (n: Int)List[de.uni_koblenz.dltypes.runtime.DLType]
  ----- [Select] Main.this.lst drop ----------------------------------------------------------------------------------*/

  /*

   */
  "ms: method (1)" in {
    success(testCase("test",
      """
        |val ww: `:WhiteWine` = iri"PeterMccoyChardonnay"
        |val lst = List(ww, ww)
        |val lst2: List[`:Wine`] = lst.drop(1)
      """.stripMargin))
  }

  /* object: Main.this.lst method: apply
  is method? true
  is param/skolem? true
  tree - Main.this.lst.apply
    :  tpe - (n: Int)de.uni_koblenz.dltypes.runtime.DLType
  tree symbol - method apply
    :  tpe - (n: Int)A
  q - Main.this.lst
    :  tpe - List
  q symbol - value lst
    :  tpe - => List[de.uni_koblenz.dltypes.runtime.DLType @de.uni_koblenz.dltypes.runtime.dl("(<:WhiteWine>⊔<:WhiteWine>)")]

  >>>>> METHOD ( List(value n) )-> de.uni_koblenz.dltypes.runtime.DLType
  ----- Main.this.lst . apply
     :: (n: Int)de.uni_koblenz.dltypes.runtime.DLType
  ----- [Select] Main.this.lst apply ---------------------------------------------------------------------------------*/
  "ms: apply (1)" in {
    success(testCase("test",
      """
        |val ww: `:WhiteWine` = iri"PeterMccoyChardonnay"
        |val lst = List(ww, ww)
        |val lst2: `:Wine` = lst(1)
      """.stripMargin))
  }

  /*>>>>>
  object: Main.this.lst method: lift
  is method? true
  is param/skolem? true
  tree - Main.this.lst.lift
    :  tpe - Int => Option[de.uni_koblenz.dltypes.runtime.DLType]
  tree symbol - method lift
    :  tpe - => A => Option[B]
  q - Main.this.lst
    :  tpe - List
  q symbol - value lst
    :  tpe - => List[de.uni_koblenz.dltypes.runtime.DLType @de.uni_koblenz.dltypes.runtime.dl("(<:WhiteWine>⊔<:WhiteWine>)")]

  >>>>> Other case in SELECT!
  ----- Main.this.lst . lift
     :: Int => Option[de.uni_koblenz.dltypes.runtime.DLType]
  ----- [Select] Main.this.lst lift -----------------------------------------------------------------------------------*/
  /*object: Main.this.lst.lift method: apply
  is method? true
  is param/skolem? true
  tree - Main.this.lst.lift.apply
    :  tpe - (v1: Int)Option[de.uni_koblenz.dltypes.runtime.DLType]
  tree symbol - method apply
    :  tpe - (v1: T1)R
  q - Main.this.lst.lift
    :  tpe - Int => Option[de.uni_koblenz.dltypes.runtime.DLType]
  q symbol - method lift
    :  tpe - => A => Option[B]

  >>>>> METHOD ( List(value v1) )-> Option[de.uni_koblenz.dltypes.runtime.DLType]
  ----- Main.this.lst.lift . apply
     :: (v1: Int)Option[de.uni_koblenz.dltypes.runtime.DLType]
  ----- [Select] Main.this.lst.lift apply ----------------------------------------------------------------------------*/
  "ms: function & apply (1)" in {
    success(testCase("test",
      """
        |val ww: `:WhiteWine` = iri"PeterMccoyChardonnay"
        |val lst = List(ww, ww)
        |val lst2: Option[`:Wine`] = lst.lift(0) // note: this is lst.lift.apply()
      """.stripMargin))
  }

  /*object: Main.this.lst method: drop
  is method? true
  is param/skolem? true
  tree - Main.this.lst.drop
    :  tpe - (n: Int)List[Int]
  tree symbol - method drop
    :  tpe - (n: Int)List[A]
  q - Main.this.lst
    :  tpe - List
  q symbol - value lst
    :  tpe - => List[Int]

  >>>>> METHOD ( List(value n) )-> List[Int]
  ----- Main.this.lst . drop
     :: (n: Int)List[Int]
  ----- [Select] Main.this.lst drop ----------------------------------------------------------------------------------*/
  "ms: compare INT" in {
    success(testCase("test",
      """
        |val ww: `:WhiteWine` = iri"PeterMccoyChardonnay"
        |val lst = List(1,2)
        |val lst2 = lst.drop(1)
      """.stripMargin))
  }

  // MISSION: SELECT - PHASE 2 (END)


  "function type test" in {
    success(testCase("test",
      """
        |val ww = iri"PeterMccoyChardonnay"
        |val f1 = (x: `:WhiteWine`) => x
        |val t1: `:Wine` = f1(ww)
        |
        |def f2(x: `:WhiteWine`) = x
        |val t2: `:Wine` = f2(ww)
      """.stripMargin))
  }


  "OPTIONAL" in {
    success(testCase("test",
      """
        |val ww1: List[(Option[`#t`], `:Wine`)] =
        |  sparql"SELECT ?b ?a WHERE { ?a a :RedWine OPTIONAL { ?a :hasMaker ?b } }"
      """.stripMargin))
  }

  "apply call" in {
    success(testCase("test",
      """
        |val rw = iri"PageMillWineryCabernetSauvignon"
        |
        |//class Thing[A](val a: A) {
        |//  def apply(i: Int): A = a
        |//  def get: A = a
        |//  def callo(b: A): List[A] = List(b, a)
        |//}
        |
        |class Thong(val a: `:RedWine`) {
        |  def apply(i: Int): `:RedWine` = a
        |}
        |
        |val c0 = new Thong(rw)
        |val g1: `:Wine` = c0(42)
        |
        |//val c = new Thing(rw)
        |//val g1: `:Wine` = c(42)
        |//val g2: `:Wine` = c.get
        |//val gs: List[`:Wine`] = c.callo(rw)
      """.stripMargin))
  }

  "method call" in {
    success(testCase("test",
      """
        |val rw: `:RedWine` = iri"PageMillWineryCabernetSauvignon"
        |
        |class Thing[A](val a: A) {
        |  def get(i: Int): A = a
        |}
        |
        |val c = new Thing(rw)
        |val g: `:Wine` = c.get(1000)
      """.stripMargin))
  }

  "meeee" in {
    success(testCase("test",
    """
      |val rw: `:RedWine` = iri"PageMillWineryCabernetSauvignon"
      |val ww: `:WhiteWine` = iri"PeterMccoyChardonnay"
      |
      |def m[A <: `:Wine`](a1: A, b: Int, a2: A): A = a2
      |
      |val x: `:Wine` = m(rw, 42, ww)
    """.stripMargin))
  }

  "m" in {
    success(testCase("test",
      """
        |val rw: `:RedWine` = iri"PageMillWineryCabernetSauvignon"
        |val mc = iri"PageMillWineryCabernetSauvignon"
        |val ww: `:WhiteWine` = iri"PeterMccoyChardonnay"
        |
        |def m[A, B](a1: A, a2: A, b: B): List[A] =
        |  List(a1, a2)
        |
        |val t = m(rw, mc, ww)
        |
        |val x: `:Wine` = t.head
        |
        |val x11: Option[`:Wine`] = t.lift(0)
        |
      """.stripMargin
    ))
  }

  "a" in {
    success(testCase("test",
      """
        |val rw: `:RedWine` = iri"PageMillWineryCabernetSauvignon"
        |val mc = iri"PageMillWineryCabernetSauvignon"
        |val ww: `:WhiteWine` = iri"PeterMccoyChardonnay"
        |
        |class Twice[A <: `:Wine`, B >: `:Wine`](a1: A, a2: A, b: B) {
        |  def getA1: A = a1
        |  def getA2: A = a2
        |  def getB: B = b
        |}
        |
        |val t = new Twice(rw, mc, ww)
        |val x11: `:Wine` = t.getA1
        |val x12: `:Wine` = t.getA2
        |val x2: `:Wine` = t.getB
      """.stripMargin
    ))
  }

  "s" in {
    success(testCase("test",
      """
        |val rw: `:RedWine` = iri"PageMillWineryCabernetSauvignon"
        |val s: String = rw.string
      """.stripMargin
    ))
  }

  "abox (temp)" in {
    success(testCase("test",
      """
        |val rw = iri"PageMillWineryCabernetSauvignon" : `:WhiteWine`
        |val w: `:Wine` = rw
      """.stripMargin
    ))
  }

  "try" in {
    success(testCase("test",
      """
        |val rw = iri"PageMillWineryCabernetSauvignon"
        |val ww = iri"PeterMccoyChardonnay"
        |val w: `:Wine` = iri"PeterMccoyChardonnay"
        |
        |def f(): `:Wine` = {
        |  try {
        |    throw new Exception("1")
        |    rw
        |  }
        |  catch {
        |    case e: Exception => ww
        |  }
        |  finally {
        |    return rw
        |  }
        |}
        |
        |val e: `:Wine` = f()
      """.stripMargin
    ))
  }

  "inference match/case" in {
    success(testCase("test",
      """
        |val p = 1 < 2
        |val rw = iri"PageMillWineryCabernetSauvignon"
        |val ww = iri"PeterMccoyChardonnay"
        |val w: `:Wine` = iri"PeterMccoyChardonnay"
        |val e: Seq[`:WhiteWine`] = p match {
        |  case true => List(rw)
        |  case false => Seq(ww)
        |}
      """.stripMargin))
  }

  "multi param lists" in {
    success(testCase("test",
    """
      |val ww: `:WhiteWine` = iri"PeterMccoyChardonnay"
      |val rw: `:RedWine` = iri"PageMillWineryCabernetSauvignon"
      |
      |def f[A,B](a: A)(b1: B)(b2: B): List[B] = List(b1, b2)
      |
      |val l = f(ww)(rw)(rw)
    """.stripMargin))
  }

  "annotation list" in {
    success(testCase("test",
    """
      |val ww: `:WhiteWine` = iri"PeterMccoyChardonnay"
      |val rw: `:RedWine` = iri"PageMillWineryCabernetSauvignon"
      |
      |val lst = List(ww, rw)
      |
      |val h: `#t` = lst.head
      |
      |//def f(x: `:Wine`): `:Wine` = x
      |//val i = lst.map(f)
    """.stripMargin))
  }

  "annotation apply" in {
    success(testCase("test",
      """
        |val ww: `:WhiteWine` = iri"PeterMccoyChardonnay"
        |val rw: `:RedWine` = iri"PageMillWineryCabernetSauvignon"
        |
        |//def m1[A](a: A, as: List[A]): A = { val x = a; x }
        |//def m2[A](a: A, b: `:Wine`): A = { val x = a; x }
        |//def m3(a: `:Wine`, b: `:Wine`): `:Wine` = { val x = a; x }
        |//def m4[A, B](a: A, b: B): B = b
        |
        |//val lst1: List[`:Wine`] = List(ww, rw)
        |
        |class Thing[A](a: A)
        |
        |val ai = new Thing(ww)
        |
        |//val a1 = m1(ww, lst1)
        |//
        |//val a2 = m2(ww, rw)
        |//
        |//val a3 = m3(ww, rw)
        |//
        |//val a4 = m4(ww, 19)
        |
      """.stripMargin))
  }

  "annotation 3" in {
    success(testCase("test",
    """
      |val ww1: List[`<:Wine>`] = sparql"SELECT ?woot WHERE { ?woot a :RedWine }"
      |val ww2 = ww1
      |val ww3 = ww2
    """.stripMargin))
  }

  "annotation 4" in {
    success(testCase("test",
      """
        |val ww1: `:WhiteWine` = iri"PeterMccoyChardonnay"
        |val ww2 = { val x = 1; ww1 }
        |//val ww2 = ww1
        |val ww3 = ww2
      """.stripMargin
    ))
  }

  "annotation 2" in {
    success(testCase("test",
      """
        |val ww: `:WhiteWine` = iri"PeterMccoyChardonnay"
        |val rw: `:RedWine` = iri"PageMillWineryCabernetSauvignon"
        |//val wine1: DLType = ww
        |//val wine2: DLType = wine1
        |
        |val lst1: List[`:Wine`] = List(ww, rw)
        |
        |val h: `#t` = lst1.head
        |
        |def m[A](a: A, as: List[A]): A = a
        |
        |val lst2 = m(ww, lst1)
        |
        |//val lst2 = List(iri"PeterMccoyChardonnay", iri"PageMillWineryCabernetSauvignon")
        |
        |//val wws: List[List[`:WhiteWine`]] = List( List(iri"PeterMccoyChardonnay") )
        |//val ws: Seq[List[DLType]] = wws
        |//val ws = wws
        |
        |//val w = ws
        |
        |//def f(x: `:Wine`): List[`:Wine`] = List(x)
        |
        |//val y = f(wine)
      """.stripMargin))
  }

  "annotation inference" in {
    success(testCase("test",
      """
        |val whitewine: `:WhiteWine` = iri"PeterMccoyChardonnay"
        |val wine: DLType = whitewine
        |
        |//val wws: List[List[`:WhiteWine`]] = List( List(iri"PeterMccoyChardonnay") )
        |//val ws: Seq[List[DLType]] = wws
        |//val ws = wws
        |
        |//val w = ws
        |
        |def f(x: `:Wine`): List[`:Wine`] = List(x)
        |
        |val y = f(wine)
      """.stripMargin))
  }

  "exp.r querio" in {
    success(testCase("test",
      """
        |val t: List[(`:Professor`, `:Course`)] =
        |  sparql"SELECT ?x ?y WHERE { ?x a :Professor. ?y a :Course. ?x :teacherOf ?y. }"
      """.stripMargin))
  }

  "new" in {
    success(testCase("test",
      """
        |//class A[T <: `:Wine`](t: T)
        |
        |//val rw: `:RedWine` = iri"PageMillWineryCabernetSauvignon"
        |//val c = new A(rw)
        |
        |//class B extends A
        |//class C extends B
        |//
        |//def f[T >: C <: A](t: T): T = t
        |
        |//val t = f(new B)
        |
        |//val rw: `:RedWine` = iri"PageMillWineryCabernetSauvignon"
        |//def f[W >: `{:PageMillWineryCabernetSauvignon}`](w: W): W = w
        |//val w: `#t` = f(rw)
      """.stripMargin))
  }

  "temp (val / if)" in {
    success(testCase("test",
      """
        |val ww = iri"PeterMccoyChardonnay"
        |def f[W <: `:Wine`](w: W) = w.`:hasMaker`
        |val e: List[`:Winery`] = f(ww)
      """.stripMargin))
  }

  "inference (val / if) in LIST" in {
    success(testCase("test",
      """
        |val p = 1 < 2
        |val rw = iri"PageMillWineryCabernetSauvignon"
        |val ww = iri"PeterMccoyChardonnay"
        |val w: `:Wine` = iri"PeterMccoyChardonnay"
        |val e: Seq[`#t`] =
        |    if (p) { List(rw) }
        |    else if (!p) { Seq(ww) }
        |    else { List(w) }
      """.stripMargin))
  }

  "inference (val / if)" in {
    success(testCase("test",
      """
        |val p = 1 < 2
        |val rw = iri"PageMillWineryCabernetSauvignon"
        |val ww = iri"PeterMccoyChardonnay"
        |val w: `:Wine` = iri"PeterMccoyChardonnay"
        |val e =
        |    if (p) { rw }
        |    else if (!p) { ww }
        |    else { w }
        |val e2: `:Wine` = e
      """.stripMargin))
  }

  "inference (def / if) - no parens" in {
    success(testCase("test",
      """
        |def fn = {
        |  val p = 1 < 2
        |  val rw = iri"PageMillWineryCabernetSauvignon"
        |  val ww = iri"PeterMccoyChardonnay"
        |  val w: `:Wine` = iri"PeterMccoyChardonnay"
        |  if (p) { rw }
        |  else if (!p) { ww }
        |  else { w }
        |}
        |val e2: `:Wine` = fn
      """.stripMargin))
  }

  "inference (def / if) - with parens" in {
    success(testCase("test",
      """
        |def fn() = {
        |  val p = 1 < 2
        |  val rw = iri"PageMillWineryCabernetSauvignon"
        |  val ww = iri"PeterMccoyChardonnay"
        |  val w: `:Wine` = iri"PeterMccoyChardonnay"
        |  if (p) { rw }
        |  else if (!p) { ww }
        |  else { w }
        |}
        |val e2: `:Wine` = fn()
      """.stripMargin))
  }

  "inference (def / if) - with args" in {
    success(testCase("test",
      """
        |val wine = iri"PageMillWineryCabernetSauvignon"
        |def fn(x: `:Wine`) = {
        |  val p = 1 < 2
        |  val rw = iri"PageMillWineryCabernetSauvignon"
        |  val ww = iri"PeterMccoyChardonnay"
        |  val w: `:Wine` = iri"PeterMccoyChardonnay"
        |  if (p) { rw }
        |  else if (!p) { ww }
        |  else { w }
        |}
        |val e2: `:Wine` = fn(wine)
      """.stripMargin))
  }

  "inference (def / if) - poly" in {
    success(testCase("test",
      """
        |val wine = iri"PageMillWineryCabernetSauvignon"
        |def fn[T](x: T) = {
        |  val p = 1 < 2
        |  val rw = iri"PageMillWineryCabernetSauvignon"
        |  val ww = iri"PeterMccoyChardonnay"
        |  val w: `:Wine` = iri"PeterMccoyChardonnay"
        |  if (p) { rw }
        |  else if (!p) { ww }
        |  else { w }
        |}
        |val e2: `:Wine` = fn(wine)
      """.stripMargin))
  }

  "inference (def / if) - poly (but no args)" in {
    success(testCase("test",
      """
        |def fn[T]() = {
        |  val p = 1 < 2
        |  val rw = iri"PageMillWineryCabernetSauvignon"
        |  val ww = iri"PeterMccoyChardonnay"
        |  val w: `:Wine` = iri"PeterMccoyChardonnay"
        |  if (p) { rw }
        |  else if (!p) { ww }
        |  else { w }
        |}
        |val e2: `:Wine` = fn()
      """.stripMargin))
  }

  "inference (def / if) - recursive" in {
    success(testCase("test",
      """
        |def fn(): `:Wine` = {
        |  val p = 1 < 2
        |  val rw = iri"PageMillWineryCabernetSauvignon"
        |  val ww = iri"PeterMccoyChardonnay"
        |  val w: `:Wine` = iri"PeterMccoyChardonnay"
        |  if (p) { rw }
        |  else if (!p) { ww }
        |  else { fn() }
        |}
        |val e2: `:Wine` = fn()
      """.stripMargin))
  }

  "random" in {
    success(testCase("test",
    """
      |class Thing[+A](a: A)
      |val i: `:WhiteWine` = iri"PeterMccoyChardonnay"
      |//val is: Thing[Thing[`:Wine`]] = new Thing(new Thing(i))
      |val is: List[(Thing[`:Wine`], Thing[`:Wine`])] = List((new Thing(i), new Thing(i)))
      |val ir: List[(Thing[`#t`], Thing[`#t`])] = is
    """.stripMargin))
  }

  "random II" in {
    success(testCase("test",
      """
        |class Thing[A, B](a: A, b: B)
        |class Thang[A, B](a: A, b: B) extends Thing[B, A](b, a)
        |
        |val i1: `:Wine` = iri"PeterMccoyChardonnay"
        |val i2: `:WhiteWine` = iri"PeterMccoyChardonnay"
        |
        |val t: Thing[`:RedWine`, `:WhiteWine`] = new Thang(i1, i2)
      """.stripMargin))
  }


  "random III" in {
    success(testCase("test",
      """
        |class Thing[+A](a: A)
        |val i: `:WhiteWine` = iri"PeterMccoyChardonnay"
        |val is: Thing[`:WhiteWine`] = new Thing(i)
        |val ir = is
      """.stripMargin))
  }

  "playspace -1" in {
    success(testCase("test",
      """
        |val f1 = (x: `:Wine`, y: `:Wine`) =>
        |  if (x.isInstanceOf[`:WhiteWine`])
        |    x.asInstanceOf[`:WhiteWine`]
        |  else
        |    iri"PeterMccoyChardonnay"
        |val f2: (`:RedWine`, `:RedWine`) => `:Wine` = f1
      """.stripMargin))
  }

  "playspace 0" in {
    success(testCase("test",
      """
        |val i = iri"PeterMccoyChardonnay"
        |val fn = (x: `:Wine`) => x
        |val ws = List(i, i, i)
        |ws.map(fn)
      """.stripMargin))
  }

  "playspace 1" in {
    success(testCase("test",
    """
      |val i = iri"PeterMccoyChardonnay"
      |val fn = (x: `:Wine`) => println(x)
      |val ws = List(i, i, i)
      |ws.foreach(fn)
    """.stripMargin))
  }

  "playspace 2" in {
    failure(testCase("test",
      """
        |val i = iri"PeterMccoyChardonnay"
        |def f[T <: `:RedWine`](x: T): Unit = println(x)
        |f(i) // TYPE BOUNDS ARE IGNORED
      """.stripMargin))
  }

  /*
  "Implicit" in {
    success(testCase("test",
    """
      |abstract class Monoid[A] {
      |  def add(x: A, y: A): A
      |  def unit: A
      |}
      |
      |object ImplicitTest {
      |  implicit val intMonoid: Monoid[Int] = new Monoid[Int] {
      |    def add(x: Int, y: Int): Int = x + y
      |    def unit: Int = 0
      |  }
      |
      |  def sum[A](xs: List[A])(implicit m: Monoid[A]): A =
      |    if (xs.isEmpty) m.unit
      |    else m.add(xs.head, sum(xs.tail))
      |
      |  def main(args: Array[String]): Unit = {
      |    println(sum(List(1,3,4)))
      |  }
      |}
    """.stripMargin))
  }
  */

  "Role projection" in {
    success(testCase("test",
    """
      |val p = iri"PeterMccoyChardonnay"
      |val x: List[`:Winery`] = p.`:hasMaker`
    """.stripMargin))
  }

  "Strict query" in {
    success(testCase("test",
    """
      |val p = iri"PeterMccoyChardonnay"
      |val x: List[`:Winery`] = strictsparql"SELECT ?y WHERE { $p :hasMaker ?y }"
    """.stripMargin))
  }

  "Equality (warn)" in {
    success(testCase("test",
      """
        |val rw: `:RedWine` = iri"PageMillWineryCabernetSauvignon"
        |val ww: `:WhiteWine` = iri"PeterMccoyChardonnay"
        |val p1 = rw == ww
      """.stripMargin))
  }

  "Equality (ok)" in {
    success(testCase("test",
    """
      |val rw: `:RedWine` = iri"PageMillWineryCabernetSauvignon"
      |val ww: `:Wine` = iri"PeterMccoyChardonnay"
      |val p1 = rw == ww
    """.stripMargin))
  }

  "Query with arguments I" in {
    success(testCase("test",
    """
        |val w: `:Wine` = iri"PeterMccoyChardonnay"
        |val x1: List[(`:Winery`, `:Winery`)] = sparql"SELECT ?y ?x WHERE { $w :hasMaker ?y }"
    """.stripMargin))
  }

  "Query with arguments II" in {
    failure(testCase("test",
      """
        |val w: `:Winery` = iri"PeterMccoy"
        |val x: List[`:Wine`] = sparql"SELECT ?y WHERE { ?y :hasMaker $w }"
    """.stripMargin))
  }

  "Query with arguments III" in {
    success(testCase("test",
      """
        |val w: `:Winery` = iri"PeterMccoy"
        |val x: List[`∃:hasMaker.:Winery`] = sparql"SELECT ?y WHERE { ?y :hasMaker $w }"
      """.stripMargin))
  }

  "Query with arguments - expression I" in {
    success(testCase("test",
      """
        |val w: List[`:Winery`] = List(iri"PeterMccoy")
        |val x: List[`∃:hasMaker.:Winery`] = sparql"SELECT ?y WHERE { ?y :hasMaker ${w.head} }"
      """.stripMargin))
  }

  "Query with Scala arguments (success)" in {
    success(testCase("Test0",
      """
        |val w: `:Wine` = iri"PeterMccoyChardonnay"
        |val i: Int = 1998
        |val x = sparql"SELECT ?y WHERE { $w :hasVintageYear ?y. ?y :yearValue $i }"
        |val y: `:VintageYear` = x.head
      """.stripMargin))
  }

  "Query with Scala arguments (failure)" in {
    failure(testCase("Test0",
      """
        |val w: `:Wine` = iri"PeterMccoyChardonnay"
        |val i: String = "1998"
        |val x = sparql"SELECT ?y WHERE { $w :hasVintageYear ?y. ?y :yearValue $i }"
      """.stripMargin))
  }

  "Query with Scala arguments - expression" in {
    success(testCase("Test0",
      """
        |val w: `:Wine` = iri"PeterMccoyChardonnay"
        |val i: Int = 98
        |val x = sparql"SELECT ?y WHERE { $w :hasVintageYear ?y. ?y :yearValue ${1900 + i} }"
        |val y: `:VintageYear` = x.head
      """.stripMargin))
  }

  "Explicit inference (for lists)" in {
    success(testCase("test",
      """
        |val rw: `:RedWine` = iri"PageMillWineryCabernetSauvignon"
        |val ww: `:WhiteWine` = iri"PeterMccoyChardonnay"
        |val ws = List[`???`](rw, ww)
        |val ws2: List[`:Wine`] = ws
      """.stripMargin))
  }

  "Runtime: Type Case" in {
    success(testCase("test",
      """
        |val a: Any = iri"PeterMccoyChardonnay"
        |val redAllowed = true
        |val m = a match {
        |  //case i: Int => println("integer!")
        |  case w: `:WhiteWine` =>
        |    val g: `:Wine` = w
        |    println("white wine")
        |    g
        |  case w: `:RedWine` if redAllowed =>
        |    // This works as well.
        |    //if (w.isInstanceOf[`:RedWine | :WhiteWine`])
        |    //  println("always " + 19)
        |    println("a red wine!")
        |    w
        |  //case _: `:Wine` => println("an undisclosed white wine!")
        |  //case _ => println("don't know that thing")
        |}
    """.stripMargin))
    // GENERATES
    // ---------
    //Main.this.a match {
    //  case (i @ (_: Int)) => scala.Predef.println("integer!")
    //  case (x$1 @ (_: de.uni_koblenz.dltypes.runtime.IRI)) if x$1.isSubsumed(de.uni_koblenz.dltypes.tools.Concept.apply(":WhiteWine")) => {
    //    val w: DLTypeDefs.:WhiteWine = x$1;
    //    val g: DLTypeDefs.:Wine = w;
    //    scala.Predef.println("white wine")
    //  }
    //  case (x$2 @ (_: de.uni_koblenz.dltypes.runtime.IRI)) if x$2.isSubsumed(de.uni_koblenz.dltypes.tools.Concept.apply(":RedWine")).&&(Main.this.redAllowed) => {
    //    val w: DLTypeDefs.:RedWine = x$2;
    //    scala.Predef.println("a red wine!")
    //  }
    //  case (x$3 @ (_: de.uni_koblenz.dltypes.runtime.IRI)) if x$3.isSubsumed(de.uni_koblenz.dltypes.tools.Concept.apply(":Wine")) => scala.Predef.println("an undisclosed white wine!")
    //  case _ => scala.Predef.println("don\'t know that thing")
    //}
    // ---------
  }

  "Runtime: instanceOf" in {
    success(testCase("test",
      """
      |val x: Any = iri"PeterMccoyChardonnay"
      |if (x.isInstanceOf[`:RedWine | :WhiteWine`])
      |  println("always " + 19)
    """.
        stripMargin))
    // GENERATES
    // ---------
    // if ({
    // val x$1: Any = Main.this.x
    // x$1.isInstanceOf[de.uni_koblenz.dltypes.runtime.IRI].&&(x$1.asInstanceOf[de.uni_koblenz.dltypes.runtime.IRI].isSubsumed(
    //   de.uni_koblenz.dltypes.tools.Union.apply(
    //     de.uni_koblenz.dltypes.tools.Concept.apply(":RedWine"),
    //     de.uni_koblenz.dltypes.tools.Concept.apply(":WhiteWine"))))
    // })
    //   scala.Predef.println("always".+(19))
    // ---------
  }
  */
}
