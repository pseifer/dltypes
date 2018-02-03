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
