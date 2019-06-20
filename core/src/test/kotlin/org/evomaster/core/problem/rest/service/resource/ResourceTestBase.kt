package org.evomaster.core.problem.rest.service.resource

import com.google.inject.Module
import com.netflix.governator.lifecycle.LifecycleManager
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.client.java.controller.api.dto.database.operations.DatabaseCommandDto
import org.evomaster.client.java.controller.api.dto.database.operations.QueryResultDto
import org.evomaster.client.java.controller.db.SqlScriptRunner
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.database.DatabaseExecutor
import org.evomaster.core.database.extract.h2.ExtractTestBaseH2
import org.evomaster.core.problem.rest.service.ResourceDepManageService
import org.evomaster.core.problem.rest.service.ResourceManageService
import org.evomaster.core.problem.rest.service.ResourceSampleMethodController
import org.evomaster.core.problem.rest.service.resource.model.SimpleResourceModule
import org.evomaster.core.problem.rest.service.resource.model.SimpleResourceSampler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class ResourceTestBase : ExtractTestBaseH2() {

    protected lateinit var config: EMConfig
    protected lateinit var sampler: SimpleResourceSampler
    protected lateinit var rm: ResourceManageService
    protected lateinit var dm: ResourceDepManageService
    protected lateinit var ssc : ResourceSampleMethodController
    private lateinit var lifecycleManager : LifecycleManager

    @BeforeEach
    fun init() {
        val injector = LifecycleInjector.builder()
                .withModules(* arrayOf<Module>(SimpleResourceModule(), BaseModule()))
                .build().createInjector()

        lifecycleManager = injector.getInstance(LifecycleManager::class.java)
        lifecycleManager.start()

        sampler = injector.getInstance(SimpleResourceSampler::class.java)
        config = injector.getInstance(EMConfig::class.java)
        rm = injector.getInstance(ResourceManageService::class.java)
        dm = injector.getInstance(ResourceDepManageService::class.java)
        ssc = injector.getInstance(ResourceSampleMethodController::class.java)
    }

    @AfterEach
    fun close(){
        lifecycleManager.close()
    }

    abstract fun getSwaggerLocation(): String

    fun getDatabaseExecutor() : DatabaseExecutor = DirectDatabaseExecutor()

    private class DirectDatabaseExecutor : DatabaseExecutor {

        override fun executeDatabaseInsertionsAndGetIdMapping(dto: DatabaseCommandDto): Map<Long, Long>? {
            return null
        }

        override fun executeDatabaseCommandAndGetQueryResults(dto: DatabaseCommandDto): QueryResultDto? {
            return SqlScriptRunner.execCommand(connection, dto.command).toDto()
        }

        override fun executeDatabaseCommand(dto: DatabaseCommandDto): Boolean {
            return false
        }
    }
}

