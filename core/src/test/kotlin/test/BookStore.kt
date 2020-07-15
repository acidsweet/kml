package test

@io.jin.kml.annotations.NoArg
@io.jin.kml.annotations.Element(name = "bookstore")
data class BookStore(@io.jin.kml.annotations.Element(name = "book") var books: List<Book>)