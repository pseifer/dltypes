# Val
## Success
#### Declared val same type
```scala
val wine1: `:Wine` = iri"PeterMccoyChardonnay"
val wine2: `:Wine` = wine1
```

#### Declared var same type
```scala
var wine1: `:Wine` = iri"PeterMccoyChardonnay"
var wine2: `:Wine` = wine1
```

#### Declared val supertype
```scala
val wine: `:WhiteWine` = iri"PeterMccoyChardonnay"
```

#### Declared var supertype
```scala
var wine: `:WhiteWine` = iri"PeterMccoyChardonnay"
```

#### Inference val
```scala
val wine: `:WhiteWine` = iri"PeterMccoyChardonnay"
val wine1 = wine
val wine2: `:WhiteWine` = wine1
```

#### Inference var
```scala
var wine: `:WhiteWine` = iri"PeterMccoyChardonnay"
var wine1 = wine
var wine2: `:WhiteWine` = wine1
```

#### Inference val from IRI
```scala
val wine1 = iri"PeterMccoyChardonnay"
val wine2: `{:PeterMccoyChardonnay}` = wine1
```

#### Inference var from IRI
```scala
var wine1 = iri"PeterMccoyChardonnay"
var wine2: `{:PeterMccoyChardonnay}` = wine1
```

## Failure
#### Declared val same type 1
```scala
val wine1: `:RedWine` = iri"PeterMccoyChardonnay"
val wine2: `:RedWine` = wine1
```

#### Declared val same type 2
```scala
val wine1: `:Wine` = iri"PeterMccoyChardonnay"
val wine2: `:RedWine` = wine1
```

#### Declared var same type 1
```scala
var wine1: `:RedWine` = iri"PeterMccoyChardonnay"
var wine2: `:RedWine` = wine1
```

#### Declared var same type 2
```scala
var wine1: `:Wine` = iri"PeterMccoyChardonnay"
var wine2: `:RedWine` = wine1
```

#### Declared val supertype
```scala
val wine: `:RedWine` = iri"PeterMccoyChardonnay"
```

#### Declared var supertype
```scala
var wine: `:RedWine` = iri"PeterMccoyChardonnay"
```

#### Inference val
```scala
val wine: `:WhiteWine` = iri"PeterMccoyChardonnay"
val wine1 = wine
val wine2: `:RedWine` = wine1
```

#### Inference var
```scala
var wine: `:WhiteWine` = iri"PeterMccoyChardonnay"
var wine1 = wine
var wine2: `:RedWine` = wine1
```

#### Inference val from IRI
```scala
val wine1 = iri"PeterMccoyChardonnay"
val wine2: `{:PageMillWineryCabernetSauvignon}` = wine1
```

#### Inference var from IRI
```scala
var wine1 = iri"PeterMccoyChardonnay"
var wine2: `{:PageMillWineryCabernetSauvignon}` = wine1
```


# Def
## Success
#### Wine id
```scala
def m(x: `:Wine`): `:Wine` = x
```

#### Wine up cast
```scala
def m(x: `:RedWine`): `:Wine` = x
```

#### Method 2 args (same - r1)
```scala
def m(x1: `:Wine`, x2: `:Wine`): `:Wine` = x1
```

#### Method 2 args (same - r2)
```scala
def m(x1: `:Wine`, x2: `:Wine`): `:Wine` = x2
```

#### Method 2 args (dif - r1)
```scala
def m(x1: `:RedWine`, x2: `:WhiteWine`): `:Wine` = x1
```

#### Method 2 args (dif - r2)
```scala
def m(x1: `:RedWine`, x2: `:WhiteWine`): `:Wine` = x2
```

#### Method 2 args (dif2 - r1)
```scala
def m(x1: `:RedWine`, x2: `:WhiteWine`): `:RedWine` = x1
```

#### Method 2 args (dif2 - r2)
```scala
def m(x1: `:RedWine`, x2: `:WhiteWine`): `:WhiteWine` = x2
```

## Failure
#### Wine id
```scala
def m(x: `:Wine`): `:RedWine` = x
```

#### Wine up cast
```scala
def m(x: `:RedWine`): `:WhiteWine` = x
```

#### Method 2 args (same - r1)
```scala
def m(x1: `:Wine`, x2: `:Wine`): `:RedWine` = x1
```

#### Method 2 args (same - r2)
```scala
def m(x1: `:Wine`, x2: `:Wine`): `:RedWine` = x2
```

#### Method 2 args (dif - r1)
```scala
def m(x1: `:RedWine`, x2: `:WhiteWine`): `:WhiteWine` = x1
```

#### Method 2 args (dif - r2)
```scala
def m(x1: `:RedWine`, x2: `:WhiteWine`): `:RedWine` = x2
```

#### Method 2 args (dif2 - r1)
```scala
def m(x1: `:RedWine`, x2: `:WhiteWine`): `:WhiteWine` = x1
```

#### Method 2 args (dif2 - r2)
```scala
def m(x1: `:RedWine`, x2: `:WhiteWine`): `:RedWine` = x2
```


# TypeDef
## Success
#### Alias
```scala
type WhiteWine = `:WhiteWine`
val x: WhiteWine = iri"PeterMccoyChardonnay"
```

#### Double Alias
```scala
type WhiteWine = `:WhiteWine`
type WhiteWineAlias = WhiteWine
val x: WhiteWineAlias = iri"PeterMccoyChardonnay"
```

## Failure
#### Alias
```scala
type Wine = `:RedWine`
val x: Wine = iri"PeterMccoyChardonnay"
```

#### Double Alias
```scala
type RedWine = `:RedWine`
type RedWineAlias = RedWine
val x: RedWineAlias = iri"PeterMccoyChardonnay"
```


# Function
## Success
#### Simple function
```scala
val f: `:Wine` => `:Wine` = (x: `:Wine`) => x
val x: `:Wine` = f(iri"PeterMccoyChardonnay")
```

## Failure
#### Simple function
```scala
val f: `:RedWine` => `:RedWine` = x: `:RedWine` => x
val x: `:Wine` = f(iri"PeterMccoyChardonnay")
```


# Block
## Success
#### Block 1 statement
```scala
val wine1: `:WhiteWine` = iri"PeterMccoyChardonnay"
val wine2: `:Wine` = { wine1 }
```

#### Block 2 statement
```scala
val wine1: `:WhiteWine` = iri"PeterMccoyChardonnay"
val wine2: `:Wine` = { val x: Int = 42; wine1 }
```

## Failure
#### Block 1 statement
```scala
val wine1: `:WhiteWine` = iri"PeterMccoyChardonnay"
val wine2: `:RedWine` = { wine1 }
```

#### Block 2 statement
```scala
val wine1: `:WhiteWine` = iri"PeterMccoyChardonnay"
val wine2: `:RedWine` = { val x: Int = 42; wine1 }
```


# Assign
## Success
#### Simple Assignment
```scala
var wine: `:WhiteWine` = iri"PeterMccoyChardonnay"
wine = iri"BancroftChardonnay"
```
## Failure
#### Simple Assignment
```scala
var wine: `:WhiteWine` = iri"PeterMccoyChardonnay"
wine = iri"PageMillWineryCabernetSauvignon"
```


# If
## Success
#### Two branch if
```scala
val wine: `:RedWine | :WhiteWine` =
  if (1 < 2) iri"PeterMccoyChardonnay"
  else iri"PageMillWineryCabernetSauvignon"
```

#### Three branch if (1)
```scala
val wine: `:RedWine | :WhiteWine` =
  if (1 < 2) iri"PeterMccoyChardonnay"
  else if (2 > 3) iri"PageMillWineryCabernetSauvignon"
  else iri"PeterMccoyChardonnay"
```

#### Three branch if (2)
```scala
val wine: `:RedWine | :WhiteWine` =
  if (1 < 2) iri"PageMillWineryCabernetSauvignon"
  else if (2 > 3) iri"PeterMccoyChardonnay"
  else iri"PeterMccoyChardonnay"
```

#### Three branch if (3)
```scala
val wine: `:RedWine | :WhiteWine` =
  if (1 < 2) iri"PeterMccoyChardonnay"
  else if (2 > 3) iri"PeterMccoyChardonnay"
  else iri"PageMillWineryCabernetSauvignon"
```

#### Full three branch if
```scala
val wine: `:RedWine | :WhiteWine | :RoseWine` =
  if (1 < 2) iri"RoseDAnjou"
  else if (2 > 3) iri"PeterMccoyChardonnay"
  else iri"PageMillWineryCabernetSauvignon"
```


## Failure
#### Two branch if 1
```scala
var wine: `:RedWine` =
  if (1 < 2) iri"PageMillWineryCabernetSauvignon"
  else iri"PeterMccoyChardonnay"
```

#### Two branch if 2
```scala
val wine: `:RedWine` =
  if (1 < 2) iri"PeterMccoyChardonnay"
  else iri"PageMillWineryCabernetSauvignon"
```

#### Three branch if (1) 1
```scala
val wine: `:RedWine` =
  if (1 < 2) iri"PeterMccoyChardonnay"
  else if (2 > 3) iri"PageMillWineryCabernetSauvignon"
  else iri"PeterMccoyChardonnay"
```

#### Three branch if (2) 1
```scala
val wine: `:RedWine` =
  if (1 < 2) iri"PageMillWineryCabernetSauvignon"
  else if (2 > 3) iri"PeterMccoyChardonnay"
  else iri"PeterMccoyChardonnay"
```

#### Three branch if (3) 1
```scala
val wine `:RedWine` =
  if (1 < 2) iri"PeterMccoyChardonnay"
  else if (2 > 3) iri"PeterMccoyChardonnay"
  else iri"PageMillWineryCabernetSauvignon"
```

#### Three branch if (1) 2
```scala
val wine: `:WhiteWine` =
  if (1 < 2) iri"PeterMccoyChardonnay"
  else if (2 > 3) iri"PageMillWineryCabernetSauvignon"
  else iri"PeterMccoyChardonnay"
```

#### Three branch if (2) 2
```scala
val wine: `:WhiteWine` =
  if (1 < 2) iri"PageMillWineryCabernetSauvignon"
  else if (2 > 3) iri"PeterMccoyChardonnay"
  else iri"PeterMccoyChardonnay"
```

#### Three branch if (3) 2
```scala
val wine `:WhiteWine` =
  if (1 < 2) iri"PeterMccoyChardonnay"
  else if (2 > 3) iri"PeterMccoyChardonnay"
  else iri"PageMillWineryCabernetSauvignon"
```

#### Full three branch if 1
```scala
val wine: `:RedWine | :WhiteWine` =
  if (1 < 2) iri"RoseDAnjou"
  else if (2 > 3) iri"PeterMccoyChardonnay"
  else iri"PageMillWineryCabernetSauvignon"
```

#### Full three branch if 2
```scala
val wine: `:RedWine | :RoseWine` =
  if (1 < 2) iri"RoseDAnjou"
  else if (2 > 3) iri"PeterMccoyChardonnay"
  else iri"PageMillWineryCabernetSauvignon"
```

#### Full three branch if 3
```scala
val wine: `:WhiteWine | :RoseWine` =
  if (1 < 2) iri"RoseDAnjou"
  else if (2 > 3) iri"PeterMccoyChardonnay"
  else iri"PageMillWineryCabernetSauvignon"
```


# Match and Case
## Success
#### Simple match
```scala
val x: `#t` = iri"PeterMccoyChardonnay"
val w: `:RedWine | :WhiteWine` = x match {
  case x: `:RedWine` => x
  case y: `:WhiteWine` => y
}
```

#### Instance
```scala
val x: Any = iri"PeterMccoyChardonnay"
if (x.isInstanceOf[`:RedWine | :WhiteWine`])
  println("always" + 19)
```

## Failure
#### Simple match 1
```scala
val x: `#t` = iri"PeterMccoyChardonnay"
val w: `:RedWine` = x match {
  case x: `:RedWine` => x
  case y: `:WhiteWine` => y
}
```

#### Simple match 2
```scala
val x: `#t` = iri"PeterMccoyChardonnay"
val w: `:WhiteWine` = x match {
  case x: `:RedWine` => x
  case y: `:WhiteWine` => y
}
```


# Return
## Success
#### Super simple return
```scala
def m(x: `:WhiteWine`): `:Wine` =
  return x
```

#### Simple return
```scala
def m(x: `:WhiteWine`): `:Wine` = {
  val y: `:Wine` = x
  return y
}
```

#### Forked return
```scala
def m(x: `:WhiteWine`): `:Wine` = {
  val y: `:Wine` = x
  if (1 < 2) return y
  x
}
```

## Failure
#### Super simple return
```scala
def m(x: `:WhiteWine`): `:RedWine` =
  return x
```

#### Simple return
```scala
def m(x: `:RedWine`): `:RedWine` = {
  val y: `:Wine` = x
  return y
}
```

#### Forked return 1
```scala
def m(x: `:RedWine`): `:RedWine` = {
  val y: `:Wine` = x
  if (1 < 2) return y
  x
}
```

#### Forked return 2
```scala
def m(x: `:RedWine`): `:RedWine` = {
  val y: `:Wine` = x
  if (1 < 2) return x
  y
}
```


# Try
## Success
#### One branch
```scala
val ww = iri"PeterMccoyChardonnay"
val rw = iri"PageMillWineryCabernetSauvignon"

val x: `:RedWine | :WhiteWine` = {
  try {
    ww
  }
  catch {
    case e: Exception => rw
  }
}
```

#### Two branches
```scala
val ww = iri"PeterMccoyChardonnay"
val rw = iri"PageMillWineryCabernetSauvignon"
val ro = iri"RoseDAnjou"

val x: `:RedWine | :WhiteWine | :RoseWine` = {
  try {
    rw
  }
  catch {
    case e: RuntimeException => ww
    case e: Exception => ro
  }
}
```

## Failure
#### One branch 1
```scala
val ww = iri"PeterMccoyChardonnay"
val rw = iri"PageMillWineryCabernetSauvignon"

val x: `:RedWine` = {
  try {
    ww
  }
  catch {
    case e: Exception => rw
  }
}
```

#### One branch 2
```scala
val ww = iri"PeterMccoyChardonnay"
val rw = iri"PageMillWineryCabernetSauvignon"

val x: `:WhiteWine` = {
  try {
    ww
  }
  catch {
    case e: Exception => rw
  }
}
```

#### Two branches 1
```scala
val ww = iri"PeterMccoyChardonnay"
val rw = iri"PageMillWineryCabernetSauvignon"
val ro = iri"RoseDAnjou"

val x: `:WhiteWine | :RoseWine` = {
  try {
    rw
  }
  catch {
    case e: RuntimeException => ww
    case e: Exception => ro
  }
}
```

#### Two branches 2
```scala
val ww = iri"PeterMccoyChardonnay"
val rw = iri"PageMillWineryCabernetSauvignon"
val ro = iri"RoseDAnjou"

val x: `:RedWine | :RoseWine` = {
  try {
    rw
  }
  catch {
    case e: RuntimeException => ww
    case e: Exception => ro
  }
}
```

#### Two branches 3
```scala
val ww = iri"PeterMccoyChardonnay"
val rw = iri"PageMillWineryCabernetSauvignon"
val ro = iri"RoseDAnjou"

val x: `:RedWine | :WhiteWine` = {
  try {
    rw
  }
  catch {
    case e: RuntimeException => ww
    case e: Exception => ro
  }
}
```


# Typed
## Success
#### Annotated type
```scala
val w: `:Wine` =  iri"PageMillWineryCabernetSauvignon" : `:RedWine`
```

## Failure
#### Annotated type 1
```scala
val w: `:Wine` =  iri"PageMillWineryCabernetSauvignon" : `:WhiteWine`
```

#### Annotated type 2
```scala
val w: `:RoseWine` =  iri"PageMillWineryCabernetSauvignon" : `:WhiteWine`
```


# New
## Success
#### Constructor
```scala
class Thing(x: `:WhiteWine`) { }
val ww = iri"PeterMccoyChardonnay"
val t: Thing = new Thing(ww)
```

#### Constructor val
```scala
class Thing(val x: `:WhiteWine`) { }
val ww = iri"PeterMccoyChardonnay"
val t: Thing = new Thing(ww)
```

#### Constructor 2 arguments
```scala
class Thing(x: `:WhiteWine`, y: `:RedWine`) { }
val ww = iri"PeterMccoyChardonnay"
val rw = iri"PageMillWineryCabernetSauvignon"
val t: Thing = new Thing(ww, rw)
```

#### Constructor with type parameter
```scala
class Thing[+W](x: W, y: W) { }
val ww = iri"PeterMccoyChardonnay"
val rw = iri"PageMillWineryCabernetSauvignon"
val t: Thing[`:Wine`] = new Thing(ww, rw)
```

## Failure
#### Constructor
```scala
class Thing(x: `:RedWine`) { }
val ww = iri"PeterMccoyChardonnay"
val t: Thing = new Thing(ww)
```

#### Constructor val
```scala
class Thing(val x: `:RedWine`) { }
val ww = iri"PeterMccoyChardonnay"
val t: Thing = new Thing(ww)
```

#### Constructor 2 arguments
```scala
class Thing(x: `:RedWine`, y: `:WhiteWine`) { }
val ww = iri"PeterMccoyChardonnay"
val rw = iri"PageMillWineryCabernetSauvignon"
val t: Thing = new Thing(ww, rw)
```

#### Constructor with type parameter 1
```scala
class Thing[+W](x: W, y: W) { }
val ww = iri"PeterMccoyChardonnay"
val rw = iri"PageMillWineryCabernetSauvignon"
val t: Thing[`:RedWine`] = new Thing(ww, rw)
```

#### Constructor with type parameter 2
```scala
class Thing[W](x: W, y: W) { }
val ww = iri"PeterMccoyChardonnay"
val rw = iri"PageMillWineryCabernetSauvignon"
val t: Thing[`:Wine`] = new Thing(ww, rw)
```


# Apply
## Success
#### Call wine id
```scala
def m(x: `:Wine`): `:Wine` = x
val x: `:Wine` = m(iri"PeterMccoyChardonnay")
```

#### Method 2 args (1)
```scala
def m(x1: `:Wine`, x2: `:Wine`): `:Wine` = x1
val x: `:Wine` = m(iri"PeterMccoyChardonnay", iri"PeterMccoyChardonnay")
```

#### Method 2 args (2)
```scala
def m(x1: `:RedWine`, x2: `:WhiteWine`): `:RedWine` = x1
val x: `:RedWine` = m(iri"PageMillWineryCabernetSauvignon", iri"PeterMccoyChardonnay")
```

#### Method 2 arg lists
```scala
def m(x1: `:RedWine`)(x2: `:WhiteWine`): `:RedWine` = x1
val x: `:RedWine` = m(iri"PageMillWineryCabernetSauvignon")(iri"PeterMccoyChardonnay")
```

## Failure
#### Call wine id 1
```scala
def m(x: `:Wine`): `:Wine` = x
val x: `:RedWine` = m(iri"PeterMccoyChardonnay")
```

#### Call wine id 2
```scala
def m(x: `:RedWine`): `:RedWine` = x
val x: `:RedWine` = m(iri"PeterMccoyChardonnay")
```

#### Method 2 args (1) 1
```scala
def m(x1: `:Wine`, x2: `:Wine`): `:Wine` = x1
val x: `:RedWine` = m(iri"PeterMccoyChardonnay", iri"PeterMccoyChardonnay")
```

#### Method 2 args (1) 2
```scala
def m(x1: `:RedWine`, x2: `:RedWine`): `:RedWine` = x1
val x: `:Wine` = m(iri"PeterMccoyChardonnay", iri"PeterMccoyChardonnay")
```

#### Method 2 args (2)
```scala
def m(x1: `:WhiteWine`, x2: `:RedWine`): `:WhiteWine` = x1
val x: `:WhiteWine` = m(iri"PageMillWineryCabernetSauvignon", iri"PeterMccoyChardonnay")
```

#### Method 2 arg lists
```scala
def m(x1: `:WhiteWine`)(x2: `:RedWine`): `:WhiteWine` = x1
val x: `:WhiteWine` = m(iri"PageMillWineryCabernetSauvignon")(iri"PeterMccoyChardonnay")
```


# Queries
## Success
#### With args (1)
```scala
val w: `:Wine` = iri"PeterMccoyChardonnay"
val x1: List[`:Winery`] = sparql"SELECT ?y WHERE { $w :hasMaker ?y }"
```

#### With args (2)
```scala
val w: `:Winery` = iri"PeterMccoy"
val x: List[`∃:hasMaker.:Winery`] = sparql"SELECT ?y WHERE { ?y :hasMaker $w }"
```

#### With args (3)
```scala
val w: List[`:Winery`] = List(iri"PeterMccoy")
val x: List[`∃:hasMaker.:Winery`] = sparql"SELECT ?y WHERE { ?y :hasMaker ${w.head} }"
```

#### With Scala args
```scala
val w: `:Wine` = iri"PeterMccoyChardonnay"
val i: Int = 1998
val x = sparql"SELECT ?y WHERE { $w :hasVintageYear ?y. ?y :yearValue $i }"
val y: `:VintageYear` = x.head
```

#### With Scala args expr
```scala
val w: `:Wine` = iri"PeterMccoyChardonnay"
val i: Int = 98
val x = sparql"SELECT ?y WHERE { $w :hasVintageYear ?y. ?y :yearValue ${1900 + i} }"
val y: `:VintageYear` = x.head
```

#### Strict query with arg
```scala
val p = iri"PeterMccoyChardonnay"
val x: List[`:Winery`] = strictsparql"SELECT ?y WHERE { $p :hasMaker ?y }"
```

## Failure
#### With args (1) 1
```scala
val w: `:Wine` = iri"PeterMccoyChardonnay"
val x1: List[`:Wine`] = sparql"SELECT ?y WHERE { $w :hasMaker ?y }"
```

#### With args (1) 2
```scala
val w: `:Wine` = iri"PeterMccoyChardonnay"
val x1: List[`:Winery`] = sparql"SELECT ?y WHERE { $w :locatedIn ?y }"
```

#### With args (2)
```scala
val w: `:Winery` = iri"PeterMccoy"
val x: List[`:Wine`] = sparql"SELECT ?y WHERE { ?y :hasMaker $w }"
```

#### With args (3)
```scala
val w: List[`:Winery`] = List(iri"PeterMccoy")
val x: List[`∃:hasMaker.:Winery`] = sparql"SELECT ?y WHERE { ?y :locatedIn ${w.head} }"
```

#### With Scala args
```scala
val w: `:Wine` = iri"PeterMccoyChardonnay"
val i: String = "1998"
val x = sparql"SELECT ?y WHERE { $w :hasVintageYear ?y. ?y :yearValue $i }"
val y: `:VintageYear` = x.head
```

#### With Scala args expr
```scala
val w: `:Wine` = iri"PeterMccoyChardonnay"
val i: Int = 1998
val x = sparql"SELECT ?y WHERE { $w :hasVintageYear ?y. ?y :yearValue ${i.toString} }"
val y: `:VintageYear` = x.head
```

#### Strict query with arg 1
```scala
val p = iri"PeterMccoyChardonnay"
val x: List[`:Winery`] = strictsparql"SELECT ?y WHERE { $p :locatedIn ?y }"
```

#### Strict query with arg 2
```scala
val p = iri"PeterMccoy"
val x: List[`:Winery`] = strictsparql"SELECT ?y WHERE { $p :hasMaker ?y }"
```


# Role Projection
## Success
#### Simple projection (1)
```scala
val p = iri"PeterMccoyChardonnay"
val x: List[`:Winery`] = p.`:hasMaker`
```

#### Simple projection (2)
```scala
val p: `:Wine` = iri"PeterMccoyChardonnay"
val x: List[`:Winery`] = p.`:hasMaker`
```

## Failure
#### Simple projection (1) 1
```scala
val p = iri"PeterMccoy"
val x: List[`:Winery`] = p.`:hasMaker`
```

#### Simple projection (1) 2
```scala
val p = iri"PeterMccoyChardonnay"
val x: List[`:Winery`] = p.`:locatedIn`
```

#### Simple projection (2) 1
```scala
val p: `:Winery` = iri"PeterMccoy"
val x: List[`:Winery`] = p.`:hasMaker`
```

#### Simple projection (2) 2
```scala
val p: `:Wine` = iri"PeterMccoyChardonnay"
val x: List[`:Winery`] = p.`:locatedIn`
```


# Type Parameters
## Success
#### Empty T* constructor
```scala
val wines: List[`:RedWine`] = List()
```

#### One element T* constructor
```scala
val wine1 = iri"PeterMccoyChardonnay"
val wines: List[`:WhiteWine`] = List(wine1)
```

#### One element T* constructor (annotated type)
```scala
val wine1 = iri"PeterMccoyChardonnay"
val wines: List[`:WhiteWine`] = List[`:WhiteWine`](wine1)
```

#### One element T* constructor (inv) (not list)
```scala
class Thing[A](a: A*)
val wine1: `:Wine` = iri"PeterMccoyChardonnay"
val awine = new Thing(wine1, wine1)
val awine2: Thing[`:Wine`] = awine
```

#### One element T* constructor (cov) (not list)
```scala
class Thing[+A](a: A*)
val wine1: `:WhiteWine` = iri"PeterMccoyChardonnay"
val awine = new Thing(wine1, wine1)
val awine2: Thing[`:Wine`] = awine
```

#### One element T* constructor (con) (not list)
```scala
class Thing[-A](a: A*)
val wine1: `:Wine` = iri"PeterMccoyChardonnay"
val awine = new Thing(wine1, wine1)
val awine2: Thing[`:WhiteWine`] = awine
```

#### Member function of that return type
```scala
val wine1 = iri"PeterMccoyChardonnay"
val  wines: List[`:WhiteWine`] = List(wine1)
val whead: `:Wine` = wines.head
```

#### Two element T* constructor with annotated type
```scala
val wine1 = iri"PeterMccoyChardonnay"
val wine2 = iri"BancroftChardonnay"
val wines = List[`:WhiteWine`](wine1, wine2)
val other: List[`:Wine`] = wines
```

#### Two element T* constructor same element type
```scala
val wines = List(iri"PeterMccoyChardonnay", iri"PeterMccoyChardonnay")
val other: List[`:Wine`] = wines
```

#### Assignment to supertype
```scala
val wine1 = iri"PeterMccoyChardonnay"
val wines1: List[`:WhiteWine`] = List(wine1)
val wines2: List[`:Wine`] = wines1
```

#### Example with non List type
```scala
case class Thing[+A](a: A, a2: A)
val wine1: `:WhiteWine` = iri"PeterMccoyChardonnay"
val wine2: `:WhiteWine` = iri"BancroftChardonnay"
val thing = Thing(wine1, wine1)
val thing2: Thing[`:Wine`] = thing
```

#### Different types
```scala
case class Thing[+A, +B](a: A, a2: B)
val wine1: `:WhiteWine` = iri"PeterMccoyChardonnay"
val wine2: `:RedWine` = iri"PageMillWineryCabernetSauvignon"
val thing = Thing(wine1, wine2)
val thing2: Thing[`:Wine`, `:Wine`] = thing
```

#### Different types, not the same class
```scala
class Thang[+A,+B]
class Thing[+A,+B](a: A, a2: B) extends Thang[A,B]
val wine1: `:WhiteWine` = iri"PeterMccoyChardonnay"
val wine2: `:RedWine` = iri"PageMillWineryCabernetSauvignon"
val thing = new Thing(wine1, wine2)
val thang: Thang[`:Wine`, `:Wine`] = thing
```

## Failure
#### Different types
```scala
case class Thing[A, B](a: A, a2: B)
val wine1: `:WhiteWine` = iri"PeterMccoyChardonnay"
val wine2: `:RedWine` = iri"PageMillWineryCabernetSauvignon"
val thing = Thing(wine1, wine2)
val thing2: Thing[`:RedWine`, `:Wine`] = thing
```


## Failure
#### Member function of that return type
```scala
val wine1 = iri"PeterMccoyChardonnay"
val  wines: List[`:WhiteWine`] = List(wine1)
val whead: `:RedWine` = wines.head
```

#### One element T* constructor
```scala
val wine1 = iri"PeterMccoyChardonnay"
val wines: List[`:RedWine`] = List(wine1)
```

#### One element T* constructor (annotated type)
```scala
val wine1 = iri"PeterMccoyChardonnay"
val wines: List[`:RedWine`] = List[`:RedWine`](wine1)
```

#### Two element T* constructor with annotated type
```scala
val wine1 = iri"PeterMccoyChardonnay"
val wine2 = iri"BancroftChardonnay"
val wines: List[`:RedWine`] = List[`:RedWine`](wine1, wine2)
val other: List[`:Wine`] = wines
```

#### Two element T* constructor same element type
```scala
val wines = List(iri"PeterMccoyChardonnay", iri"PeterMccoyChardonnay")
val other: List[`:RedWine`] = wines
```

#### Assignment to subtype
```scala
val wine1 = iri"PeterMccoyChardonnay"
val wines1: List[`:Wine`] = List(wine1)
val wines2: List[`:WhiteWine`] = wines1
```

#### Example with non List type I
```scala
case class Thing[A](a: A, a2: A)
val wine1: `:WhiteWine` = iri"PeterMccoyChardonnay"
val wine2: `:WhiteWine` = iri"BancroftChardonnay"
val thing = Thing(wine1, wine1)
val thing2: Thing[`:RedWine`] = thing
```

#### Example with non List type II
```scala
case class Thing[A](a: A, a2: A)
val wine1: `:WhiteWine` = iri"PeterMccoyChardonnay"
val wine2: `:RedWine` = iri"PageMillWineryCabernetSauvignon"
val thing = Thing[`:WhiteWine`](wine1, wine2)
val thing2: Thing[`:Wine`] = thing
```


# Defined and Satisfiable
## Success
#### Concept is defined
```scala
def m(x: `:Wine`): `#t` = x
```

#### Expression is satisfiable
```scala
def m(x: `:RedWine | :WhiteWine`): `#t` = x
```

## Warning
#### Concept is not defined
```scala
def m(x: `:What`): `#t` = x
```

#### Expression is not satisfiable
```scala
def m(x: `:RedWine & :WhiteWine`): `#t` = x
```


# Two parameter lists
## Success
#### T1
```scala
val rw = iri"PageMillWineryCabernetSauvignon"
val ww = iri"PeterMccoyChardonnay"
def method[A,B](a: A)(b: B)(a2: A): Tuple2[A,B] = (a,b)
val t: Tuple2[`:RedWine`,`:WhiteWine`] = method(rw)(ww)(rw)
```

#### T2
```scala
val rw = iri"PageMillWineryCabernetSauvignon"
val ww = iri"PeterMccoyChardonnay"
def method(r: `:RedWine`)(w: `:WhiteWine`): List[`:Wine`] = List(r,w)
val t: List[`:Wine`] = method(rw)(ww)
```

#### T3
```scala
val rw = iri"PageMillWineryCabernetSauvignon"
def method(r: `:RedWine`): List[`:Wine`] = List(r)
val t: List[`:Wine`] = method(rw)
```

## Failure
#### T1
```scala
val rw = iri"PageMillWineryCabernetSauvignon"
val ww = iri"PeterMccoyChardonnay"
def method[A,B](a: A)(b: B)(a2: A): Tuple2[A,B] = (a,b)
val t: Tuple2[`:WhiteWine`,`:RedWine`] = method(rw)(ww)(rw)
```

#### T2
```scala
val rw = iri"PageMillWineryCabernetSauvignon"
val ww = iri"PeterMccoyChardonnay"
def method(r: `:WhiteWine`)(w: `:RedWine`): List[`:Wine`] = List(r,w)
val t: List[`#t`] = method(rw)(ww)
```

#### T3
```scala
val rw = iri"PageMillWineryCabernetSauvignon"
def method(r: `:WhiteWine`): List[`:Wine`] = List(r)
val t: List[`#t`] = method(rw)
```

# Combined Cases
## Success
#### Method with block if body
```scala
def m(x1: `:WhiteWine`, x2: `:RedWine`): `:RedWine | :WhiteWine` = {
  val x: `:RedWine` = x2
  if (1 < 2) x1 else x
}
val x: `:RedWine | :WhiteWine` =
  m(iri"PeterMccoyChardonnay", iri"PageMillWineryCabernetSauvignon")
```

#### Method with alias
```scala
type RedOrWhiteWine = `:RedWine | :WhiteWine`
def m(x: RedOrWhiteWine): RedOrWhiteWine = x
val x: `:Wine` = m(iri"PeterMccoyChardonnay")
```

#### Val if in list
```scala
val p = 1 < 2
val rw = iri"PageMillWineryCabernetSauvignon"
val ww = iri"PeterMccoyChardonnay"
val w: `:Wine` = iri"PeterMccoyChardonnay"
val e: Seq[`:Wine`] =
    if (p) { List(rw) }
    else if (!p) { Seq(ww) }
    else { List(w) }
```

#### Function with if body
```scala
val wine = iri"RoseDAnjou"
def fn(x: `:RoseWine`) = {
  val p = 1 < 2
  val rw = iri"PageMillWineryCabernetSauvignon"
  val ww = iri"PeterMccoyChardonnay"
  if (p) { rw }
  else if (!p) { ww }
  else { x }
}
val e2: `:WhiteWine | :RedWine | :RoseWine` = fn(wine)
```

## Failure
#### Method with block if body 1
```scala
def m(x1: `:WhiteWine`, x2: `:RedWine`): `:RedWine | :WhiteWine` = {
  val x: `:RedWine` = x2
  if (1 < 2) x1 else x
}
val x: `:RedWine` =
  m(iri"PeterMccoyChardonnay", iri"PageMillWineryCabernetSauvignon")
```

#### Method with block if body 2
```scala
def m(x1: `:WhiteWine`, x2: `:RedWine`): `:RedWine | :WhiteWine` = {
  val x: `:Wine` = x2
  if (1 < 2) x1 else x
}
val x: `:RedWine | :WhiteWine` =
  m(iri"PeterMccoyChardonnay", iri"PageMillWineryCabernetSauvignon")
```

#### Method with alias 1
```scala
type RedOrWhiteWine = `:RedWine | :WhiteWine`
def m(x: RedOrWhiteWine): RedOrWhiteWine = x
val x: `:WhiteWine` = m(iri"PeterMccoyChardonnay")
```

#### Method with alias 2
```scala
type RedOrWhiteWine = `:RedWine`
def m(x: RedOrWhiteWine): RedOrWhiteWine = x
val x: `:Wine` = m(iri"PeterMccoyChardonnay")
```

#### Val if in list
```scala
val p = 1 < 2
val rw = iri"PageMillWineryCabernetSauvignon"
val ww = iri"PeterMccoyChardonnay"
val w: `:Wine` = iri"PeterMccoyChardonnay"
val e: Seq[`:RedWine`] =
    if (p) { List(rw) }
    else if (!p) { Seq(ww) }
    else { List(w) }
```

#### Function with if body 1
```scala
val wine = iri"RoseDAnjou"
def fn(x: `:RoseWine`) = {
  val p = 1 < 2
  val rw = iri"PageMillWineryCabernetSauvignon"
  val ww = iri"PeterMccoyChardonnay"
  if (p) { rw }
  else if (!p) { ww }
  else { x }
}
val e2: `:WhiteWine | :RoseWine` = fn(wine)
```

#### Function with if body 2
```scala
val wine = iri"RoseDAnjou"
def fn(x: `:RoseWine`) = {
  val p = 1 < 2
  val rw = iri"PageMillWineryCabernetSauvignon"
  val ww = iri"PeterMccoyChardonnay"
  if (p) { rw }
  else if (!p) { ww }
  else { x }
}
val e2: `:WhiteWine | :RedWine` = fn(wine)
```

#### Function with if body 3
```scala
val wine = iri"RoseDAnjou"
def fn(x: `:RoseWine`) = {
  val p = 1 < 2
  val rw = iri"PageMillWineryCabernetSauvignon"
  val ww = iri"PeterMccoyChardonnay"
  if (p) { rw }
  else if (!p) { ww }
  else { x }
}
val e2: `:RoseWine | :RedWine` = fn(wine)
```
