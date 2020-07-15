package test

import com.jin.kml.Kml
import com.jin.kml.log
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.createType

const val xml = "<bookstore>\n" +
        "<book category=\"COOKING\">\n" +
        "  <title lang=\"en\">Everyday Italian</title> \n" +
        "  <author>Giada De Laurentiis</author> \n" +
        "  <year>2005</year> \n" +
        "  <price>30.00</price> \n" +
        "</book>\n" +
        "<book category=\"CHILDREN\">\n" +
        "  <title lang=\"en\">Harry Potter</title> \n" +
        "  <author>J K. Rowling</author> \n" +
        "  <year>2005</year> \n" +
        "  <price>29.99</price> \n" +
        "</book>\n" +
        "<book category=\"WEB\">\n" +
        "  <title lang=\"en\">Learning XML</title> \n" +
        "  <author>Erik T. Ray</author> \n" +
        "  <year>2003</year> \n" +
        "  <price>39.95</price> \n" +
        "</book>\n" +
        "</bookstore>"
fun main(args: Array<String>) {

//    toXml
//    testToXml()

//    fromXml
    testFromXml()

}

fun testToXml() {
    //toXml
    val books = ArrayList<Book>()
    books.add(Book(category = "COOKING",
            title = Title("en", "Everyday Italian"),
            author = "Giada De Laurentiis",
            year = 2005,
            price = 30.00
    ))
    books.add(Book(category = "CHILDREN",
            title = Title("en", "Harry Potter"),
            author = "J K. Rowling",
            year = 2005,
            price = 29.99
    ))
    var bookStore = BookStore(books)
//    System.out.println(bookStore.javaClass.kotlin.createType())
    Kml().toXml(bookStore)
}
fun testFromXml() {
//    System.out.println(BookStore::class.createType())
    System.out.println(Kml().fromXml<BookStore>(xml, BookStore::class)?.books?.map { it.author })
//    log("testFromXml",Kml().fromXml<Book>("<book category=\"COOKING\">\n" +
//            "  <title lang=\"en\">Everyday Italian</title> \n" +
//            "  <author>Giada De Laurentiis</author> \n" +
//            "  <year>2005</year> \n" +
//            "  <price>30.00</price> \n" +
//            "</book>\n",Book::class).toString())
//    System.out.println(arrayOfNulls<String>(5)::class.constructors)

}
