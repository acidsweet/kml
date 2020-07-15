package com.jin.kml

import io.jin.kml.annotations.Attribute
import io.jin.kml.annotations.Leaf
import org.w3c.dom.Element
import sun.rmi.runtime.Log
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.jvmErasure

class Kml {
    /**
     * object -> xml
     */
    fun toXml(obj: Any?, name: String? = null): String {

        if (obj == null) {
            return ""
        }

        // is Leaf
        if (isNativeObject(obj)) {
            if (name != null && name.isNotBlank()) {
                return String.format("%s%s%s", openTag(name), obj.toString(), closeTag(name))
            }
            return obj.toString()
        }

        val sb = StringBuilder()
        var tagName = name
        if (tagName == null || tagName.isBlank()) {
            tagName = getName(obj)
        }
        val properties = obj.javaClass.kotlin.declaredMemberProperties
        val attributes = ArrayList<Pair<String, String>>()
        val elements = ArrayList<KProperty1<Any, *>>()
        properties.map {
            System.out.println("fields: ${it.name} , ${it.returnType} ,[ ${it.annotations.size} ]")
            if (it.findAnnotation<Attribute>() != null) {
                System.out.println("attribute: ${it.name}")
                when {
                    isNativeObject(it.returnType) -> {
                        val attributeName = getName(it)
                        val attributeValue = it.get(obj).toString()
                        attributes.add(Pair(attributeName, attributeValue))
                        System.out.println("--> attribute: $attributeName , $attributeValue")
                    }
                    isArray(it.returnType) -> {
                        throw RuntimeException("xml attribute can't be an array")
                    }
                    else -> {
                        throw RuntimeException("xml attribute must be a native object")
                    }
                }
            } else {
                elements.add(it)
            }
        }

        System.out.println("elements = ${elements.size}")
        //openTag
        sb.append(openTag(tagName, attributes))

        elements.map {
            //            it.isAccessible = true
            System.out.println("elements : ${it.name} , ${it.annotations}")
            when {
                it.findAnnotation<Leaf>() != null -> {
                    System.out.println("leaf : ${it.name} , ${it.annotations}")
                    sb.append(toXml(it.get(obj)))
                }
                else -> {
                    System.out.println("elements : ${it.name} , ${it.annotations}")
                    val elementName = getName(it)
                    val element = it.get(obj)
                    when {
                        isArray(element) -> {
                            System.out.println(elements.javaClass)
                            val children = element as List<*>
                            for (_c in children) {
                                System.out.println(_c.toString())
                                sb.append(toXml(_c, elementName))
                            }
                        }
                        else -> {
                            System.out.println("elements 3: ${it.name} , ${it.returnType} ,${it.annotations}")
                            sb.append(toXml(element, elementName))
                        }
                    }
                }
            }
        }
        sb.append(closeTag(tagName))
        System.out.println("<-- : $sb")
        return sb.toString()
    }

    fun <T> fromXml(xml: String, clazz: KClass<*>): T? {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val strBuilder = StringBuilder()
        strBuilder.append(xml)
        val byteStream = ByteArrayInputStream(strBuilder.toString().toByteArray())
        val doc = builder.parse(byteStream)
        doc.documentElement.normalize()
        return fromXmlW3CElement(doc.documentElement, clazz)
    }

    private fun <T> fromXmlW3CElement(element: org.w3c.dom.Element?, clazz: KClass<*>): T? {
        System.out.println("fromXmlW3CElement: ${element?.tagName} ${clazz.qualifiedName}")
        if (clazz == null || isNativeObject(clazz) || element == null) {
            return null
        }
        for(i in 0 until element.attributes.length) {
            log("fromXmlW3CElement","attributes $i = ${element.attributes.item(i)}")
        }
        for(i in 0 until element.childNodes.length) {
            log("fromXmlW3CElement","childNodes $i = ${element.childNodes.item(i)}")
        }
        val obj = clazz.java.newInstance() as T
        val fields = clazz.java.declaredFields
        clazz.declaredMemberProperties.forEach { it ->
            System.out.println("declaredMemberProperties12: ${it.name} , ${it.returnType.arguments.map { it.type }}")
            if (it is KMutableProperty1<out Any, Any?>) {
                when {
                    it.findAnnotation<Attribute>() != null -> {
                        when {
                            isNativeObject(it.returnType) -> {
                                val attributeName = getName(it)
                                val attributeValue = string2Primite(element.getAttribute(attributeName), it.returnType)
                                System.out.println("--> attribute: $attributeName , $attributeValue")
                                if (attributeValue != null) {
                                    System.out.println("--> attribute 111")
                                    it.setUnsafed(obj, attributeValue)
                                }
                            }
                            isArray(it.returnType) -> {
                                throw RuntimeException("xml attribute can't be an array")
                            }
                            else -> {
                                throw RuntimeException("xml attribute must be a native object")
                            }
                        }
                    }
                    it.findAnnotation<Leaf>() != null -> {
                        log("leaf",element.toString())
                        val leafValue = element.textContent
                        System.out.println("--> attribute 222")
                        it.setUnsafed(obj, leafValue)
                    }
                    else -> {
                        val elementName = getName(it)
                        val propertyName = it.name
                        val elementValue = element.getElementsByTagName(elementName)
                        System.out.println("element $elementName ${it.returnType} : ${it.returnType.arguments.map { it.type }}")
                        when {
                            isArray(it.returnType) -> it.returnType.arguments?.get(0)?.run {
                                when (this.type) {
                                    Char::class.createType() -> {
                                        val array = arrayOfNulls<Char?>(elementValue.length)
                                        for (i in 0..elementValue.length) {
                                            array[i] = elementValue.item(i).nodeValue.toCharArray().first()
                                        }
                                        it.setUnsafed(obj, array)
                                    }
                                    Int::class.createType() -> {
                                        val array = arrayOfNulls<Int>(elementValue.length)
                                        for (i in 0..elementValue.length) {
                                            array[i] = elementValue.item(i).nodeValue.toInt()
                                        }
                                        it.setUnsafed(obj, array)
                                    }
                                    Short::class.createType() -> {
                                        val array = arrayOfNulls<Short>(elementValue.length)
                                        for (i in 0..elementValue.length) {
                                            array[i] = elementValue.item(i).nodeValue.toShort()
                                        }
                                        it.setUnsafed(obj, array)
                                    }
                                    Long::class.createType() -> {
                                        val array = arrayOfNulls<Long>(elementValue.length)
                                        for (i in 0..elementValue.length) {
                                            array[i] = elementValue.item(i).nodeValue.toLong()
                                        }
                                        it.setUnsafed(obj, array)
                                    }
                                    Float::class.createType() -> {
                                        val array = arrayOfNulls<Float>(elementValue.length)
                                        for (i in 0..elementValue.length) {
                                            array[i] = elementValue.item(i).nodeValue.toFloat()
                                        }
                                        it.setUnsafed(obj, array)
                                    }
                                    Double::class.createType() -> {
                                        val array = arrayOfNulls<Double>(elementValue.length)
                                        for (i in 0..elementValue.length) {
                                            array[i] = elementValue.item(i).nodeValue.toDouble()
                                        }
                                        it.setUnsafed(obj, array)
                                    }
                                    Boolean::class.createType() -> {
                                        val array = arrayOfNulls<Boolean>(elementValue.length)
                                        for (i in 0..elementValue.length) {
                                            array[i] = elementValue.item(i).nodeValue.toBoolean()
                                        }
                                        it.setUnsafed(obj, array)
                                    }
                                    String::class.createType() -> {
                                        val array = arrayOfNulls<String>(elementValue.length)
                                        for (i in 0..elementValue.length) {
                                            array[i] = elementValue.item(i).nodeValue
                                        }
                                        it.setUnsafed(obj, array)
                                    }
                                    else -> {
                                        System.out.println("非原生类型")
                                        // 非原生类型
                                        val array = arrayOfNulls<Any>(elementValue.length)
                                        for (i in 0 until  elementValue.length) {
                                            var _e: Element? = null
                                            if (elementValue.item(i) is Element) {
                                                _e = elementValue.item(i) as Element
                                            }
                                            array[i] = fromXmlW3CElement(_e, this.type!!.jvmErasure)
                                        }
                                        var list = ArrayList<Any>()
                                        array.forEach {
                                            it?.let { list.add(it) }
                                        }
//                                        it.setUnsafed(obj, array)
                                        fields.find {
                                            log("find","${it.name} == $propertyName")
                                            it.name == propertyName
                                        }?.also {
                                            it.isAccessible = true
                                            log("also","${it.type} ${array::class.java}")
                                            it.set(obj,list)
                                        }
                                    }
                                }
                            }
                            isNativeObject(it.returnType) -> it.setUnsafed(obj, string2Primite(elementValue.item(0).textContent,it.returnType))
                            else -> it.setUnsafed(obj, fromXmlW3CElement(elementValue.item(0) as Element, it.returnType.jvmErasure))
                        }
                    }
                }
            } else {
                throw RuntimeException("property ${it.name} has no access to setter")
            }
        }
        return obj
    }
}