package test


@io.jin.kml.annotations.NoArg
@io.jin.kml.annotations.Element(name = "book")
data class Book(@io.jin.kml.annotations.Attribute var category: String, @io.jin.kml.annotations.Element var title: Title, @io.jin.kml.annotations.Element var author: String, @io.jin.kml.annotations.Element var year: Int, @io.jin.kml.annotations.Element var price: Double)

@io.jin.kml.annotations.NoArg
data class Title(@io.jin.kml.annotations.Attribute var lang: String, @io.jin.kml.annotations.Leaf var text: String)