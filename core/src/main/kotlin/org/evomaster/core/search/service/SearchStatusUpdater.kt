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

    private var passed = -1

    private var extra = ""

    private val utf8 = Charset.forName("UTF-8")

    private val p = String(byteArrayOf(0xF0.toByte(), 0x9F.toByte(), 0x92.toByte(), 0xA9.toByte()), utf8)

    private val u = String(byteArrayOf(0xF0.toByte(), 0x9F.toByte(), 0xA6.toByte(), 0x84.toByte()), utf8)

    private val r = String(byteArrayOf(0xF0.toByte(), 0x9F.toByte(), 0x8C.toByte(), 0x88.toByte()), utf8)

    @PostConstruct
    fun postConstruct(){
        /*
            Make sure that, when we print, we are using UTF-8 and not the default encoding
         */
        System.setOut(PrintStream(System.out, true, "UTF-8"))

        if(config.showProgress) {
            time.addListener(this)
        }
    }

    override fun newActionEvaluated() {
        val current = (time.percentageUsedBudget() * 100).toInt()

        if(passed < -1){
            println()
            if(config.e_u1f984){
                println()
            }
        }

        if(current != passed){
            passed = current

            if(config.e_u1f984){
                upLineAndErase()
            }

            upLineAndErase()
            println("* Consumed search budget: $passed%")

            if(config.e_u1f984){
                updateExtra()
                println(extra)
            }
        }
    }

    private fun updateExtra(){
        if(extra.isBlank()){
            extra = u
        } else {
            extra = p + r  + extra
        }
    }

    private fun upLineAndErase(){
        /*
              Using: ANSI/VT100 Terminal Control Escape Sequences
              http://www.termsys.demon.co.uk/vtansi.htm
         */
        print("\u001b[1A") // move up by 1 line
        print("\u001b[2K") // erase line
    }
}