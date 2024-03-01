package org.evomaster.core.config

import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto

class ConfigsFromFile {

    var configs = mutableMapOf<String,String>()

    var auth = mutableListOf<AuthenticationDto>()
}