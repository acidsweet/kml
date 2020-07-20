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
        log("toXml","obj = $obj , name = $name")

        if (obj == null) {
            return ""
        }

        // is Leaf
        if (isPrimitiveByKType(obj::class.createType())) {
            return when {
                name?.isNotBlank() == true -> String.format("%s%s%s", openTag(name), obj.toString(), closeTag(name))
                else -> obj.toString()
            }
        }

        // get tagName
        val sb = StringBuilder()
        var tagName = name
        if (tagName == null || tagName.isBlank()) {
            tagName = getName(obj)
        }

        val properties = obj.javaClass.kotlin.declaredMemberProperties
        val attributes = ArrayList<Pair<String, String>>()
        val elements = ArrayList<KProperty1<Any, *>>()
        // get attributes
        properties.forEach {
            log("toXml","properties: ${it.name} , ${it.returnType} ,[ ${it.annotations.size} ]")
            if (it.findAnnotation<Attribute>() != null) {
                log("toXml","attribute: ${it.name}")
                when {
                    isPrimitiveByKType(it.returnType) -> {
                        val attributeName = getName(it)
                        val attributeValue = it.get(obj).toString()
                        attributes.add(Pair(attributeName, attributeValue))
                        log("toXml","--> attribute: $attributeName , $attributeValue")
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

        log("toXml","elements.size = ${elements.size}")

        //openTag
        sb.append(openTag(tagName, attributes))

        elements.forEach {
            log("toXml","elements : ${it.name} , ${it.annotations}")
            when {
                it.findAnnotation<Leaf>() != null -> {
                    log("toXml","leaf : ${it.name} , ${it.annotations}")
                    sb.append(toXml(it.get(obj)))
                }
                else -> {
                    log("toXml","elements : ${it.name} , ${it.annotations}")
                    val elementName = getName(it)
                    val element = it.get(obj)
                    when {
                        isArray(it.returnType) -> {
                            log("toXml","${elements.javaClass}")
                            for (it in (element as List<*>)) {
                                it?.also {
                                    with(sb) { append(toXml(it,elementName)) }
                                }
                            }
                        }
                        else -> {
                            sb.append(toXml(element, elementName))
                        }
                    }
                }
            }
        }
        sb.append(closeTag(tagName))
        log("toXml","$sb")
        return sb.toString()
    }

    /**
     * xml -> object
     */
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

    private fun <T> fromXmlW3CElement(element: Element?, clazz: KClass<*>): T? {
        log("->fromXml","${element?.tagName} ${clazz.qualifiedName}")

        if (clazz == null || isPrimitiveByKType(clazz.createType()) || element == null) {
            return null
        }
//        for(i in 0 until element.attributes.length) {
//            log("fromXml","attributes $i = ${element.attributes.item(i)}")
//        }
//        for(i in 0 until element.childNodes.length) {
//            log("fromXml","childNodes $i = ${element.childNodes.item(i)}")
//        }

        val obj = clazz.java.newInstance() as T
        val fields = clazz.java.declaredFields
        clazz.declaredMemberProperties.forEach { it ->
            log("fromXml", "${it.name} , ${it.returnType.arguments.map { it.type }}")
            if (it is KMutableProperty1<out Any, Any?>) {
                when {
                    it.findAnnotation<Attribute>() != null -> {
                        when {
                            isPrimitiveByKType(it.returnType) -> {
                                val attributeName = getName(it)
                                val attributeValue = string2Primitive(element.getAttribute(attributeName), it.returnType)
                                log("fromXml","$attributeName , $attributeValue")
                                if (attributeValue != null) {
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
                        it.setUnsafed(obj, leafValue)
                    }
                    else -> {
                        log("element",element.toString())
                        val elementName = getName(it)
                        val propertyName = it.name
                        val elementValue = element.getElementsByTagName(elementName)
                        log("fromXml","element: $elementName ${it.returnType} ${elementValue.length}: ${it.returnType.arguments.map { it.type }}")
                        if(elementValue.length > 0) {
                            when {
                                isArray(it.returnType) -> it.returnType.arguments?.get(0)?.run {
                                    when (this.type) {
                                        Char::class.createType() -> {
                                            val array = arrayOfNulls<Char?>(elementValue.length)
                                            for (i in 0 until elementValue.length) {
                                                array[i] = elementValue.item(i).nodeValue.toCharArray().first()
                                            }
                                            it.setUnsafed(obj, array)
                                        }
                                        Int::class.createType() -> {
                                            val array = arrayOfNulls<Int>(elementValue.length)
                                            for (i in 0 until elementValue.length) {
                                                array[i] = elementValue.item(i).nodeValue.toInt()
                                            }
                                            it.setUnsafed(obj, array)
                                        }
                                        Short::class.createType() -> {
                                            val array = arrayOfNulls<Short>(elementValue.length)
                                            for (i in 0 until elementValue.length) {
                                                array[i] = elementValue.item(i).nodeValue.toShort()
                                            }
                                            it.setUnsafed(obj, array)
                                        }
                                        Long::class.createType() -> {
                                            val array = arrayOfNulls<Long>(elementValue.length)
                                            for (i in 0 until elementValue.length) {
                                                array[i] = elementValue.item(i).nodeValue.toLong()
                                            }
                                            it.setUnsafed(obj, array)
                                        }
                                        Float::class.createType() -> {
                                            val array = arrayOfNulls<Float>(elementValue.length)
                                            for (i in 0 until elementValue.length) {
                                                array[i] = elementValue.item(i).nodeValue.toFloat()
                                            }
                                            it.setUnsafed(obj, array)
                                        }
                                        Double::class.createType() -> {
                                            val array = arrayOfNulls<Double>(elementValue.length)
                                            for (i in 0 until elementValue.length) {
                                                array[i] = elementValue.item(i).nodeValue.toDouble()
                                            }
                                            it.setUnsafed(obj, array)
                                        }
                                        Boolean::class.createType() -> {
                                            val array = arrayOfNulls<Boolean>(elementValue.length)
                                            for (i in 0 until elementValue.length) {
                                                array[i] = elementValue.item(i).nodeValue.toBoolean()
                                            }
                                            it.setUnsafed(obj, array)
                                        }
                                        String::class.createType() -> {
                                            val array = arrayOfNulls<String>(elementValue.length)
                                            for (i in 0 until elementValue.length) {
                                                array[i] = elementValue.item(i).nodeValue
                                            }
                                            it.setUnsafed(obj, array)
                                        }
                                        else -> {
                                            log("fromXml","isArray not Primitive")
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
                                            array.forEach { that ->
                                                that?.let { list.add(that) }
                                            }
                                            // 由于kotlin反射的问题，使用java反射规避
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
                                isPrimitiveByKType(it.returnType) -> {
                                    log("jin","${elementValue.length} , ${elementValue.item(0)}")
                                    it.setUnsafed(obj, string2Primitive(elementValue.item(0).textContent,it.returnType))
                                }
                                elementValue.item(0) != null -> {
                                    it.setUnsafed(obj, fromXmlW3CElement(elementValue.item(0) as Element, it.returnType.jvmErasure))
                                }
                            }
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