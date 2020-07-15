package com.jin.kml

import io.jin.kml.annotations.Attribute
import io.jin.kml.annotations.Element
import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf

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
private const val LT = "<"
private const val GT = ">"
private const val LT_END = "</"

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

fun isNativeObject(obj: Any): Boolean {
    System.out.println("--> isNativeObject: $obj")
    if(isArray(obj)){
        return false
    }
    var type: KType = when(obj) {
        is KType -> obj
        is KClass<*> -> obj.createType()
        else -> obj.javaClass.kotlin.createType()
    }
    System.out.println("--> isNativeObject: type = $type ")
    return when (type) {
        Char::class.createType() -> true
        Int::class.createType() -> true
        Short::class.createType() -> true
        Long::class.createType() -> true
        Float::class.createType() -> true
        Double::class.createType() -> true
        Boolean::class.createType() -> true
        else -> String::class.createType() == type
    }
}
fun isArray(obj: Any?): Boolean {
    System.out.println("isArray obj = $obj")
    if(obj == null) {
        return false
    }
    return when(obj) {
        is KType -> {
            kotlin.collections.List::class == obj.classifier
        }
        is KClass<*> -> {
            obj.isSubclassOf(List::class)
        }
        else -> {
            obj is Array<*> || obj is List<*>
        }
    }
}

fun string2Primite(string: String?,type: KType): Any? {
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
//    return when {
//        isArray(this) -> {
//            set(receiver as T, value as List<Any>)
//        }
//        else -> set(receiver as T, value)
//
//    }
    return set(receiver as T, value)
}
fun <T,R> KMutableProperty1<T,R>.getUnsafed(receiver: Any?): R {
    log("getUnsafed",receiver.toString())
    return get(receiver as T)
}
fun log(tag: String? = "Kml",msg: String) {
    if(true) {
        System.out.println("$tag : $msg")
    }
}
