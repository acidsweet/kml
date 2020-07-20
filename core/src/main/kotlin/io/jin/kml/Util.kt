package com.jin.kml

import io.jin.kml.annotations.Attribute
import io.jin.kml.annotations.Element
import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.*

private const val LT = "<"
private const val GT = ">"
private const val LT_END = "</"

fun getNameForKProperty(property: KProperty<*>): String {
    System.out.println("getNameForClass : $property , ${property.annotations}")
    // Attribute
    val attribute = property.findAnnotation<Attribute>()
    if (attribute != null && attribute.name.isNotBlank()) {
        return attribute.name
    }
    // Element
    val element = property.findAnnotation<Element>()
    if (element != null && element.name.isNotBlank()) {
        return element.name
    }
    return property.name!!
}

fun getNameForClass(clazz: Class<*>): String {
    System.out.println("getNameForClass : $clazz , ${clazz.annotations}")
    // Element
    val element = clazz.getAnnotation(Element::class.java)
    if (element != null && element.name.isNotBlank()) {
        return element.name
    }
    return clazz.simpleName!!
}
fun getNameForField(field: Field): String {
    System.out.println("getNameForField : $field , ${field.annotations}")
    // Attribute
    val attribute = field.getAnnotation(Attribute::class.java)
    if (attribute != null && attribute.name.isNotBlank()) {
        return attribute.name
    }
    // Element
    val element = field.getAnnotation(Element::class.java)
    if (element != null && element.name.isNotBlank()) {
        return element.name
    }
    return field.name!!
}
fun openTag(tagName: String, attributes: List<Pair<String, String>>? = null): String {
    val sb = StringBuilder()
    sb.append(LT).append(tagName)
    attributes?.map {
        sb.append(" ").append(it.first).append("=\"").append(it.second).append("\"")
    }
    return sb.append(GT).toString()
}

fun closeTag(tagName: String): String {
    val sb = StringBuilder()
    return sb.append(LT_END).append(tagName).append(GT).toString()
}

fun getName(obj: Any): String {
    System.out.println("getName : $obj")
    return when (obj) {
        is Field -> getNameForField(obj)
        is Class<*> -> getNameForClass(obj)
        is KProperty<*> -> getNameForKProperty(obj)
        else -> obj.javaClass.simpleName
    }
}

fun isArray(obj: Any?): Boolean {
    System.out.println("isArray obj = $obj")
    if(obj == null) {
        return false
    }
    val res =  when(obj) {
        is KType -> {
            List::class == obj.classifier
        }
        is KClass<*> -> {
            obj::createType
            obj.isSubclassOf(List::class)
        }
        else -> {
            obj is Array<*> || obj is List<*>
        }
    }
    log("isArray","$obj isArray = $res")
    return res
}

fun string2Primitive(string: String?,type: KType): Any? {
    return when (type) {
        Char::class.createType() -> string?.toCharArray()?.first()
        Int::class.createType() -> string?.toInt()
        Short::class.createType() -> string?.toShort()
        Long::class.createType() -> string?.toLong()
        Float::class.createType() -> string?.toFloat()
        Double::class.createType() -> string?.toDouble()
        Boolean::class.createType() -> string?.toBoolean()
        else -> string
    }
}

fun <T,R> KMutableProperty1<T,R>.setUnsafed(receiver: Any?, value: R) {
    log("setUnsafed","${receiver.toString()} : ${value.toString()} ${this.returnType.classifier}")
    return set(receiver as T, value)
}
fun <T,R> KMutableProperty1<T,R>.getUnsafed(receiver: Any?): R {
    log("getUnsafed",receiver.toString())
    return get(receiver as T)
}

/**
 * 通过KType判断是不是原生类型
 */
fun isPrimitiveByKType(type:KType): Boolean {
    return when {
        Char::class.createType(nullable = true).isSupertypeOf(type) -> true
        Int::class.createType(nullable = true).isSupertypeOf(type) -> true
        Short::class.createType(nullable = true).isSupertypeOf(type) -> true
        Long::class.createType(nullable = true).isSupertypeOf(type) -> true
        Float::class.createType(nullable = true).isSupertypeOf(type) -> true
        Double::class.createType(nullable = true).isSupertypeOf(type) -> true
        Boolean::class.createType(nullable = true).isSupertypeOf(type) -> true
        else -> String::class.createType(nullable = true).isSupertypeOf(type)
    }
}

private var isDebug = false

fun enableDebug(debug :Boolean = true) {
    isDebug = debug
}

fun log(tag: String? = "Kml",msg: String) {
    if(isDebug) {
        System.out.println("$tag : $msg")
    }
}
