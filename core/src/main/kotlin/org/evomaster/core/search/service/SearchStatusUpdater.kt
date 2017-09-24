package org.evomaster.core.search.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.LoggingUtil
import javax.annotation.PostConstruct


class SearchStatusUpdater : SearchListener{

    @Inject
    private lateinit var time: SearchTimeController

    @Inject
    private lateinit var config: EMConfig

    private var passed = -1

    @PostConstruct
    fun postConstruct(){
        if(config.showProgress) {
            time.addListener(this)
        }
    }

    override fun newActionEvaluated() {
        val current = (time.percentageUsedBudget() * 100).toInt()

        if(passed < -1){
            println()
        }

        if(current != passed){
            passed = current

            /*
                Using: ANSI/VT100 Terminal Control Escape Sequences
                http://www.termsys.demon.co.uk/vtansi.htm
             */
            print("\u001b[1A") // move up by 1 line
            print("\u001b[2K") // erase line
            println("* Consumed search budget: $passed%")
        }
    }
}