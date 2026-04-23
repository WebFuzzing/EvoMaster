package com.foo.rest.examples.spring.openapi.v3.assertions

open class AssertionDto(
        var a : Int? = 42,
        var b : String? = null,
        var c : Array<Int>? = arrayOf(1000, 2000, 3000),
        var d : SomewhatInnerObject? = SomewhatInnerObject(),
        var i : Boolean? = true,
        var l : Boolean? = false
)

open class SomewhatInnerObject(
        var e: Int? = 66,
        var f: String? = "bar",
        var g: ReallyInnerObject? = ReallyInnerObject()
)

open class ReallyInnerObject(
        var h: Array<String>? = arrayOf("xvalue", "yvalue")
)

open class SimpleObject(
        var x: Int
)