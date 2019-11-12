package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import java.nio.charset.Charset
import javax.annotation.PostConstruct
import java.io.PrintStream




class SearchStatusUpdater : SearchListener{

    @Inject
    private lateinit var time: SearchTimeController

    @Inject
    private lateinit var config: EMConfig

    @Inject
    private lateinit var archive: Archive<*>


    private var passed = "-1"

    private var lastUpdateMS = 0L

    private var lastCoverageComputation = 0

    private var coverage = 0

    private var extra = ""

    private val utf8 = Charset.forName("UTF-8")

    private val p = String(byteArrayOf(0xF0.toByte(), 0x9F.toByte(), 0x92.toByte(), 0xA9.toByte()), utf8)

    private val u = String(byteArrayOf(0xF0.toByte(), 0x9F.toByte(), 0xA6.toByte(), 0x84.toByte()), utf8)

    private val r = String(byteArrayOf(0xF0.toByte(), 0x9F.toByte(), 0x8C.toByte(), 0x88.toByte()), utf8)

    private var first = true

    /*
       Make sure that, when we print, we are using UTF-8 and not the default encoding
     */
    private val out = PrintStream(System.out, true, "UTF-8")

    @PostConstruct
    fun postConstruct(){
        if(config.showProgress) {
            time.addListener(this)
        }
    }

    override fun newActionEvaluated() {

        val percentageInt = (time.percentageUsedBudget() * 100).toInt()
        val current = String.format("%.3f", time.percentageUsedBudget() * 100)

        if(first){
            println()
            if(config.e_u1f984){
                println()
            }
            first = false
        }

        val delta = System.currentTimeMillis() - lastUpdateMS

        //writing on console is I/O, which is expensive. So, can't do it too often
        if(current != passed && delta > 500){
            lastUpdateMS += delta
            passed = current

            if(percentageInt - lastCoverageComputation > 0){
                lastCoverageComputation = percentageInt
                //this is not too expensive, but still computation. so we do it only at each 1%
                coverage = archive.numberOfCoveredTargets()
            }

            if(config.e_u1f984){
                upLineAndErase()
            }

            val avgTimeAndSize = time.computeExecutedIndividualTimeStatistics()
            val avgTime = String.format("%.1f", avgTimeAndSize.first)
            val avgSize = String.format("%.1f",avgTimeAndSize.second)

            upLineAndErase()
            println("* Consumed search budget: $passed%;" +
                    " covered targets: $coverage;" +
                    " time per test: ${avgTime}ms ($avgSize actions)")

            if(config.e_u1f984){
                updateExtra()
                out.println(extra)
            }
        }
    }

    private fun updateExtra(){
        if(extra.isBlank() || extra.length > 22){
            //going more than a line makes thing very complicated... :(
            extra = u
        } else {
            extra = p + r  + extra
        }
    }

    /*
              Using: ANSI/VT100 Terminal Control Escape Sequences
              http://www.termsys.demon.co.uk/vtansi.htm

              Note: unfortunately, many terminals do not support saving/restoring the cursor :(
              ie, following does not work for example in GitBash:
               print("\u001b[u")
               print("\u001b[s")
     */

    private fun eraseLine(){
        print("\u001b[2K") // erase line
    }

    private fun moveUp(){
        print("\u001b[1A") // move up by 1 line
    }

    private fun upLineAndErase(){
        moveUp()
        eraseLine()
    }
}