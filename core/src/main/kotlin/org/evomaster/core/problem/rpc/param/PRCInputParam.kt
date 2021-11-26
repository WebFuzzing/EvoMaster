package org.evomaster.core.problem.rpc.param

import org.evomaster.core.problem.httpws.service.param.Param
import org.evomaster.core.search.gene.Gene

/**
 * params in requests
 */
class PRCInputParam (name: String, gene: Gene): Param(name, gene)