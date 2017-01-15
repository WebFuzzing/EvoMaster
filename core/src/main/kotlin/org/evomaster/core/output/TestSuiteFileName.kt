package org.evomaster.core.output


class TestSuiteFileName(
        /**
         * This can be in the form
         * foo.bar.X
         */
        val name: String) {


    fun getPackage() : String{
        if(! name.contains('.')){
            return ""
        }

        return name.substring(0, name.lastIndexOf('.'))
    }

    fun hasPackage() = ! getPackage().isBlank()


    fun getClassName(): String{
        if(! hasPackage()){
            return name
        }

        return name.substring(name.lastIndexOf('.'), name.length)
    }


    fun getAsPath(format: OutputFormat) : String{

        return name.replace('.', '/') + when{
            format.isJava() -> ".java"
            format.isKotlin() -> ".kt"
            else -> ".txt"
        }
    }
}