package io.jin.kml.annotations

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS,AnnotationTarget.FIELD,AnnotationTarget.PROPERTY)
annotation class Attribute(val name: String = "")