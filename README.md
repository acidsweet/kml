# Kml
Kml- a lightweight xml serializer/deserializer via kotlin

## What is kml

Kml provide two simple method to  serializer/deserializer xml:

```kotlin
/* object -> xml */ fun toXml(obj: Any?, name: String? = null): String
/* xml -> object */fun <T> fromXml(xml: String, clazz: KClass<*>): T?
```

![截屏2020-07-15 下午4.26.29](https://tva1.sinaimg.cn/large/007S8ZIlly1ggrqjoptnej30pq0703yv.jpg)

Kml has one main class,one tool file,four annotations

```shell
.
├── Kml.kt
├── Util.kt
└── annotations
    ├── Attribute.kt
    ├── Element.kt
    ├── Leaf.kt
    └── NoArg.kt

```

## How To Use

#### Dependency

```groovy
// kotlin reflect support
dependencies {
    compile "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version"
    compile "org.jetbrains.kotlin:kotlin-reflect:$kotlin_version"
}

// data class noarg allopen plugin
// root build.gradle
buildscript {
		...
    dependencies {
				...
        classpath "org.jetbrains.kotlin:kotlin-noarg:$kotlin_version"
        classpath "org.jetbrains.kotlin:kotlin-allopen:$kotlin_version"
    }
}
// app build.gradle
apply plugin: 'kotlin-noarg'
apply plugin: 'kotlin-allopen'
noArg {
    invokeInitializers = true
    annotation("io.jin.kml.annotations.NoArg")
}

allOpen {
    annotation("io.jin.kml.annotations.NoArg")
}
```

#### Define your data class

* @NoArg : help data class to create a no-arg constructer
* @Element : mark class or property as xml element
  * elementName = name.isNotBlank()?name:class.simpleName or property.name 
* @Attribute : mark property as xml attribute
  * attributeName = name.isNotBlank()?name:property.name
* @Leaf : mark property as xml textNode

via kotlin

```kotlin
@NoArg
@Element(name = "book")
data class Book(@Attribute var category: String, @Element var title: Title, @Element var author: String, @Element var year: Int, @Element var price: Double)

```

via java

```java
public class Title {
    @Attribute
    private String lang;
    @Leaf
    private String text;
}
```

#### ToXml

```kotlin
Kml().toXml(bookStore)
```

#### FromXml

```kotlin
Kml().fromXml<BookStore>(xml, BookStore::class)
```

More details in [Test.kt](https://github.com/acidsweet/kml/blob/master/core/src/test/kotlin/test/Test.kt)

## What's Next

* More robustness
* Coroutine support
* Huge file support

## License





