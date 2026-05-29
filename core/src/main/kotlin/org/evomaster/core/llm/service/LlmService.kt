package org.evomaster.core.llm.service

import dev.langchain4j.model.chat.StreamingChatModel
import org.evomaster.core.llm.LlmSupport
import javax.annotation.PostConstruct
import javax.inject.Inject

class LlmService {

    @Inject
    private lateinit var model: StreamingChatModel

    @PostConstruct
    private fun initService() {

//        model = LlmSupport.createModel(
//
//        )
    }

}