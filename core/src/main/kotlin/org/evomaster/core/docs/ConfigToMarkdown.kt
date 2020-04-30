package org.evomaster.core.docs

import org.evomaster.core.EMConfig
import java.io.File
import java.nio.charset.Charset
import kotlin.reflect.KMutableProperty
import kotlin.reflect.jvm.javaType

/**
 * Class used to generate Markdown documentation for [EMConfig]
 */
object ConfigToMarkdown {

    private val defaultInstance = EMConfig()

    @JvmStatic
    fun main(args: Array<String>) {
        saveToDocs(toMarkdown())
    }

    fun toMarkdown(): String {

        val buffer = StringBuilder()

        addHeader(buffer)

        addOptions(buffer)

        return buffer.toString()
    }


    fun saveToDocs(markdownText: String) {
        val file = File("docs/options.md")
        file.delete()
        file.createNewFile()
        file.writeText(markdownText, Charset.forName("utf-8"))
    }


    private fun addHeader(buffer: StringBuilder) {

        val header = """
            # Command-Line Options
            
            _EvoMaster_ has several options that can be configured. 
            Those can be set on the command-line when _EvoMaster_ is started.
            
            There are 3 types of options:
            
            * __Important__: those are the main options that most users will need to set
                            and know about.
            
            * __Internal__: these are low-level tuning options, which most users do not need
                            to modify. These were mainly introduced when experimenting with 
                            different configurations to maximize the performance of _EvoMaster_.
                            
            * __Experimental__: these are work-in-progress options, for features still under development
                                and testing.        
                     

             The list of available options can also be displayed by using `--help`, e.g.:

             `java -jar evomaster.jar --help`
              
              Options might also have *constraints*, e.g., a numeric value within a defined range,
              or a string being an URL.
              In some cases, strings might only be chosen within a specific set of possible values (i.e., an Enum).
              If any constraint is not satisfied, _EvoMaster_ will fail with an error message.
              
              When used, all options need to be prefixed with a `--`, e.g., `--maxTime`.
              
        """.trimIndent()

        buffer.append(header)
    }


    private fun addOptions(buffer: StringBuilder) {

        val all = EMConfig.getConfigurationProperties()

        val important = all.filter { it.annotations.any { a -> a is EMConfig.Important } }
        val experimental = all.filter { it.annotations.any { a -> a is EMConfig.Experimental } }
        val internal = all.filter { it.annotations.none { a -> a is EMConfig.Experimental || a is EMConfig.Important } }

        assert(all.size == important.size + experimental.size + internal.size)

        addImportant(buffer, important)
        addInternal(buffer, internal)
        addExperimental(buffer, experimental)
    }

    private fun addInternal(buffer: StringBuilder, internal: List<KMutableProperty<*>>) {

        val sorted = internal.sortedBy { it.name }

        buffer.append("\n## Internal Command-Line Options\n\n")
        printOptionList(buffer, sorted)
    }

    private fun addExperimental(buffer: StringBuilder, experimental: List<KMutableProperty<*>>) {

        val sorted = experimental.sortedBy { it.name }

        buffer.append("\n## Experimental Command-Line Options\n\n")
        printOptionList(buffer, sorted)
    }


    private fun addImportant(buffer: StringBuilder, important: List<KMutableProperty<*>>) {

        val sorted = important.sortedBy {
            it.annotations.find { a -> a is EMConfig.Important }!!
                    .let { a ->
                        a as EMConfig.Important
                        a.priority
                    }
        }

        buffer.append("\n## Important Command-Line Options\n\n")
        printOptionList(buffer, sorted)
    }

    private fun printOptionList(buffer: StringBuilder, list: List<KMutableProperty<*>>){

        /*
            Markdown-style tables have all column widths messed up, see:

            https://stackoverflow.com/questions/36121672/set-table-column-width-via-markdown

            however, if we use HTML, then we need to change all the formattings, eg ** and `` :-(
         */

        val awfulHackButWhatElseCanWeDo = "" //"<img width=2500/>"

        buffer.append("|Options$awfulHackButWhatElseCanWeDo|Description|\n")
        buffer.append("|---|---|\n")

//        buffer.append("<table><thead><tr><th>Options</th><th>Description</th></tr></thead><tbody>")

        for(opt in list){
            printOption(buffer, opt)
        }

//        buffer.append("</tbody></table>")
    }

    private fun printOption(buffer: StringBuilder, opt: KMutableProperty<*>) {

        var default = opt.call(defaultInstance).toString()
        if(default.isBlank()){
            default = "\"\""
        }
        val type = (opt.returnType.javaType as Class<*>)
        val typeName = if(type.isEnum){
            "Enum"
        } else {
            type.simpleName.capitalize()
        }

        val description = EMConfig.getDescription(opt)

//        buffer.append("<tr>")

//        buffer.append("|<nobr>`--${opt.name}` &lt;$typeName&gt;</nobr>| ")
        buffer.append("|`${opt.name}`| ")

//        buffer.append("<td><nobr>`--${opt.name}` &lt;$typeName&gt;</nobr></td>")

//        buffer.append("<td>")

        buffer.append("__${typeName}__. ")

        buffer.append(description.text.trim())
        if(!description.text.trim().endsWith(".")){
            buffer.append(".")
        }
        if(description.constraints.isNotBlank()){
            buffer.append(" *Constraints*: `${description.constraints}`.")
        }
        if(description.enumValues.isNotBlank()){
            buffer.append(" *Valid values*: `${description.enumValues}`.")
        }
        buffer.append(" *Default value*: `$default`.")

//        buffer.append("</td></tr>")
        buffer.append("|\n")
    }



}