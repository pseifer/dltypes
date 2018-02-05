**General note**: The success and failure cases only check whether
compilation succeeds or an compilation error happens. As such, this
test suite is suitable to quickly test whether code changes violate
these cases in an obvious way. To fully understand if everything
works as expected, some manual analysis of the CHECKED or DEBUG
output is required.

# Simple assignment
Test cases that cover trivial val assignment cases,
as well as type inference for those.
## Success
#### Declared (same) type
```scala
val wine1: `:Wine` = iri"PeterMccoyChardonnay"
val wine2: `:Wine` = wine1
```

#### Declared supertype
```scala
val wine: `:WhiteWine` = iri"PeterMccoyChardonnay"
```

#### Inference for vals
```scala
val wine: `:WhiteWine` = iri"PeterMccoyChardonnay"
val wine1 = wine
```

#### Inference for vars
```scala
var wine: `:WhiteWine` = iri"PeterMccoyChardonnay"
var wine1 = wine
```

#### Assignment to vars
```scala
var wine: `:WhiteWine` = iri"PeterMccoyChardonnay"
wine = iri"BancroftChardonnay"
```

#### Inference from IRI
```scala
val wine1 = iri"PeterMccoyChardonnay"
```


## Failure
#### Declared subtype
```scala
val wine1: `:Wine` = iri"PeterMccoyChardonnay"
val wine2: `:RedWine` = wine1
```

#### Declared supertype
```scala
val wine: `:RedWine` = iri"PeterMccoyChardonnay"
```

#### Assignment to vars
```scala
var wine: `:WhiteWine` = iri"PeterMccoyChardonnay"
wine = iri"PageMillWineryCabernetSauvignon"
```





# Single type parameter
Covers classes with a single type parameter `T`, using `scala.collection.immutable.List` as
example. In addition to member functions returning `T` (for example `head`), also covers
type inference for constructors of type `T*`.
## Success
#### Empty T* constructor
```scala
val wines: List[`:RedWine`] = List()
```

#### Member function of that return type
```scala
val wine1 = iri"PeterMccoyChardonnay"
val  wines: List[`:WhiteWine`] = List(wine1)
val whead: `:Wine` = wines.head
```

#### One element T* constructor
```scala
val wine1 = iri"PeterMccoyChardonnay"
val wines: List[`:WhiteWine`] = List(wine1)
```

#### One element T* constructor (not list)
**ISSUE:** This infers DLType, which causes compilation error (for now).
```scala
case class Thing[A](a: A*)
val wine1: `:WhiteWine` = iri"PeterMccoyChardonnay"
val awine = Thing(wine1, wine1)
val awine2: Thing[`:Wine`] = awine
```

#### Two element T* constructor with annotated type
Note, that this would not work in cases where :WhiteWine was not
explicitly given. In such a case, the types of the arguments
to List() have to be exactly the same (for now).
```scala
val wine1 = iri"PeterMccoyChardonnay"
val wine2 = iri"BancroftChardonnay"
val wines = List[`:WhiteWine`](wine1, wine2)
val other: List[`:Wine`] = wines
```

#### Two element T* constructor same element type
As explained in the previous test case, this works:
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

#### Functions returning values of classes with type parameters
```scala
val wine1 = iri"PeterMccoyChardonnay"
val wines1: List[`:WhiteWine`] = List.fill(3)(wine1)
```

#### Example with non List type
```scala
case class Thing[A](a: A, a2: A)
val wine1: `:WhiteWine` = iri"PeterMccoyChardonnay"
val wine2: `:WhiteWine` = iri"BancroftChardonnay"
val thing = Thing(wine1, wine1)
val thing2: Thing[`:Wine`] = thing
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

#### Two element T* constructor with annotated type
```scala
val wine1 = iri"PeterMccoyChardonnay"
val wine2 = iri"BancroftChardonnay"
val wines = List[`:RedWine`](wine1, wine2)
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

#### Functions returning values of classes with type parameters
```scala
val wine1 = iri"PeterMccoyChardonnay"
val wines1: List[`:RedWine`] = List.fill(3)(wine1)
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





# Multiple type parameter
## Success
#### Different types
This should issue a "using argument matching heuristic"
warning as well.
```scala
case class Thing[A, B](a: A, a2: B)
val wine1: `:WhiteWine` = iri"PeterMccoyChardonnay"
val wine2: `:RedWine` = iri"PageMillWineryCabernetSauvignon"
val thing = Thing(wine1, wine2)
val thing2: Thing[`:Wine`, `:Wine`] = thing
```

## Failure
#### Different types
This should issue a "using argument matching heuristic"
warning as well.
```scala
case class Thing[A, B](a: A, a2: B)
val wine1: `:WhiteWine` = iri"PeterMccoyChardonnay"
val wine2: `:RedWine` = iri"PageMillWineryCabernetSauvignon"
val thing = Thing(wine1, wine2)
val thing2: Thing[`:RedWine`, `:Wine`] = thing
```
