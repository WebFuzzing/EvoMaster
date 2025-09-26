package org.evomaster.core

import joptsimple.*
import org.evomaster.client.java.controller.api.ControllerConstants
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto
import org.evomaster.client.java.instrumentation.shared.ExternalServiceSharedUtils
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory
import org.evomaster.core.config.ConfigProblemException
import org.evomaster.core.config.ConfigUtil
import org.evomaster.core.config.ConfigsFromFile
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.naming.NamingStrategy
import org.evomaster.core.output.sorting.SortingStrategy
import org.evomaster.core.search.impact.impactinfocollection.GeneMutationSelectionMethod
import org.evomaster.core.search.service.IdMapper
import org.slf4j.LoggerFactory
import java.net.MalformedURLException
import java.net.URL
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.reflect.KMutableProperty
import kotlin.reflect.jvm.javaType

typealias PercentageAsProbability = EMConfig.Probability

/**
 * Class used to hold all the main configuration properties
 * of EvoMaster.
 *
 */
class EMConfig {

    /*
        Code here does use the JOptSimple library

        https://pholser.github.io/jopt-simple/
     */

    companion object {

        private val log = LoggerFactory.getLogger(EMConfig::class.java)

        private const val timeRegex = "(\\s*)((?=(\\S+))(\\d+h)?(\\d+m)?(\\d+s)?)(\\s*)"

        private const val headerRegex = "(.+:.+)|(^$)"

        private const val targetSeparator = ";"
        private const val targetNone = "\\b(None|NONE|none)\\b"
        private const val targetPrefix = "\\b(Class|CLASS|class|Line|LINE|line|Branch|BRANCH|branch|MethodReplacement|METHODREPLACEMENT|method[r|R]eplacement|Success_Call|SUCCESS_CALL|success_[c|C]all|Local|LOCAL|local|PotentialFault|POTENTIALFAULT|potential[f|F]ault)\\b"
        private const val targetExclusionRegex = "^($targetNone|($targetPrefix($targetSeparator$targetPrefix)*))\$"

        private const val maxTcpPort = 65535.0

        /**
         * Maximum possible length for strings.
         * Really, having something longer would make little to no sense
         */
        const val stringLengthHardLimit = 20_000

        private const val defaultExternalServiceIP = "127.0.0.4"

        //leading zeros are allowed
        private const val lz = "0*"
        //should start with local 127
        private const val _eip_s = "^${lz}127"
        // other numbers could be anything between 0 and 255
        private const val _eip_e = "(\\.${lz}(25[0-5]|2[0-4][0-9]|1?[0-9]?[0-9])){3}$"
        // the first four numbers (127.0.0.0 to 127.0.0.3) are reserved
        // this is done with a negated lookahead ?!
        private const val _eip_n = "(?!${_eip_s}(\\.${lz}0){2}\\.${lz}[0123]$)"

        private const val externalServiceIPRegex = "$_eip_n$_eip_s$_eip_e"

        private val defaultAlgorithmForBlackBox = Algorithm.SMARTS

        private val defaultAlgorithmForWhiteBox = Algorithm.MIO

        private val defaultOutputFormatForBlackBox = OutputFormat.PYTHON_UNITTEST

        private val defaultTestCaseNamingStrategy = NamingStrategy.ACTION

        private val defaultTestCaseSortingStrategy = SortingStrategy.TARGET_INCREMENTAL

        fun validateOptions(args: Array<String>): OptionParser {

            val config = EMConfig() // tmp config object used only for validation.
                                    // actual singleton instance created with Guice

            val parser = getOptionParser()

            val options = try{
                parser.parse(*args)
            }catch (e: joptsimple.OptionException){
                throw ConfigProblemException("Wrong input configuration parameters. ${e.message}")
            }

            if (!options.has("help")) {
                //actual validation is done here when updating
                config.updateProperties(options)
            }

            return parser
        }

        /**
         * Having issue with types/kotlin/reflection...
         * Therefore, need custom output formatting.
         * However, easier way (for now) is to just override
         * what we want to change
         *
         *  TODO: groups and ordering
         */
        private class MyHelpFormatter : BuiltinHelpFormatter(80, 2) {
            override fun extractTypeIndicator(descriptor: OptionDescriptor): String? {
                return null
            }
        }

        /**
         * Get all available "console options" for the annotated properties
         */
        fun getOptionParser(): OptionParser {

            val defaultInstance = EMConfig()

            val parser = OptionParser()

            parser.accepts("help", "Print this help documentation")
                    .forHelp()

            getConfigurationProperties().forEach { m ->
                /*
                    Note: here we could use typing in the options,
                    instead of converting everything to string.
                    But it looks bit cumbersome to do it in Kotlin,
                    at least for them moment

                    Until we make a complete MyHelpFormatter, here
                    for the types we "hack" the default one, ie,
                    we set the type description to "null", but then
                    the argument description will contain the type
                 */

                val argTypeName = m.returnType.toString()
                        .run { substring(lastIndexOf('.') + 1) }

                parser.accepts(m.name, getDescription(m).toString())
                        .withRequiredArg()
                        .describedAs(argTypeName)
                        .defaultsTo(m.call(defaultInstance).toString())
            }

            parser.formatHelpWith(MyHelpFormatter())

            return parser
        }

        class ConfigDescription(
                val text: String,
                val constraints: String,
                val enumExperimentalValues: String,
                val enumValidValues: String,
                val experimental: Boolean,
                val debug: Boolean
        ) {
            override fun toString(): String {

                var description = text

                if (debug) {
                    description += " [DEBUG option]."
                }
                if (constraints.isNotBlank()) {
                    description += " [Constraints: $constraints]."
                }
                if (enumValidValues.isNotBlank()) {
                    description += " [Values: $enumValidValues]."
                }
                if (enumExperimentalValues.isNotBlank()) {
                    description += " [Experimental Values: $enumExperimentalValues]."
                }

                if (experimental) {
                    /*
                    TODO: For some reasons, coloring is not working here.
                    Could open an issue at:
                    https://github.com/jopt-simple/jopt-simple
                    */
                    //description = AnsiColor.inRed("EXPERIMENTAL: $description")
                    description = "EXPERIMENTAL: $description"
                }

                return description
            }
        }

        fun getDescription(m: KMutableProperty<*>): ConfigDescription {

            val cfg = (m.annotations.find { it is Cfg } as? Cfg)
                    ?: throw IllegalArgumentException("Property ${m.name} is not annotated with @Cfg")

            val text = cfg.description.trim().run {
                when {
                    isBlank() -> "No description."
                    !endsWith(".") -> "$this."
                    else -> this
                }
            }

            val min = (m.annotations.find { it is Min } as? Min)?.min
            val max = (m.annotations.find { it is Max } as? Max)?.max
            val probability = m.annotations.find { it is Probability }
            val url = m.annotations.find { it is Url }
            val regex = (m.annotations.find { it is Regex } as? Regex)

            var constraints = ""
            if (min != null || max != null || probability != null || url != null || regex != null) {
                if (min != null) {
                    constraints += "min=$min"
                }
                if (max != null) {
                    if (min != null) constraints += ", "
                    constraints += "max=$max"
                }
                if (probability != null) {
                    constraints += "probability 0.0-1.0"
                }
                if (url != null) {
                    constraints += "URL"
                }
                if (regex != null) {
                    constraints += "regex ${regex.regex}"
                }
            }

            var experimentalValues = ""
            var validValues = ""
            val returnType = m.returnType.javaType as Class<*>

            if (returnType.isEnum) {
                val elements = returnType.getDeclaredMethod("values")
                        .invoke(null) as Array<*>
                val experimentElements = elements.filter { it is WithExperimentalOptions && it.isExperimental() }
                val validElements = elements.filter { it !is WithExperimentalOptions || !it.isExperimental() }
                experimentalValues = experimentElements.joinToString(", ")
                validValues = validElements.joinToString(", ")
            }

            val experimental = (m.annotations.find { it is Experimental } as? Experimental)
            val debug = (m.annotations.find { it is Debug } as? Debug)

            return ConfigDescription(
                    text,
                    constraints,
                    experimentalValues,
                    validValues,
                    experimental != null,
                    debug != null
            )
        }


        fun getConfigurationProperties(): List<KMutableProperty<*>> {
            return EMConfig::class.members
                    .filterIsInstance(KMutableProperty::class.java)
                    .filter { it.annotations.any { it is Cfg } }
        }
    }

    /**
     * Update the values of the properties based on the options
     * chosen on the command line
     *
     *
     * @throws ConfigProblemException if there are constraint violations
     */
    fun updateProperties(options: OptionSet) {

        val properties = getConfigurationProperties()

        // command-line arguments are applied last (most important, overriding config file),
        // but would need to check first for config path location...
        val configPath = properties.first { it.name == "configPath" }
        updateProperty(options, configPath)
        checkPropertyConstraints(configPath)

        // First apply all settings in config file, if any
        val cff = loadConfigFile()
        if(cff != null){
            applyConfigFromFile(cff)
            authFromFile = cff.auth
        }

        // the apply command-line arguments
        properties.forEach { m ->

            updateProperty(options, m)

            checkPropertyConstraints(m)
        }

        //why was this done for each updateProperty???
        excludedTargetsForImpactCollection = extractExcludedTargetsForImpactCollection()

        checkMultiFieldConstraints()

        handleDeprecated()

        handleCreateConfigPathIfMissing(properties)
    }

    private fun handleCreateConfigPathIfMissing(properties: List<KMutableProperty<*>>) {
        if (createConfigPathIfMissing && !Path(configPath).exists() && configPath == defaultConfigPath) {

            val cff = ConfigsFromFile()
            val important = properties.filter { it.annotations.any { a -> a is Important } }
            important.forEach {
                var default = it.call(this).toString()
                val type = (it.returnType.javaType as Class<*>)
                if(default == "null"){
                    default = "null"
                }else if (default.isBlank()) {
                    default = "\"\""
                } else if(type.isEnum || String::class.java.isAssignableFrom(type)){
                    default = "\"$default\""
                }

                cff.configs[it.name] = default
            }

            if(! avoidNonDeterministicLogs) {
                LoggingUtil.uniqueUserInfo("Going to create configuration file at: ${Path(configPath).toAbsolutePath()}")
            }
            ConfigUtil.createConfigFileTemplate(configPath, cff)
        }
    }

    private fun loadConfigFile(): ConfigsFromFile?{

        //if specifying one manually, file MUST exist. otherwise might be missing
        if(!Path(configPath).exists()) {
            if (configPath == defaultConfigPath) {
                return null
            } else {
                throw ConfigProblemException("There is no configuration file at custom path: $configPath")
            }
        }

        if(! avoidNonDeterministicLogs) {
            LoggingUtil.uniqueUserInfo("Loading configuration file from: ${Path(configPath).toAbsolutePath()}")
        }

        try {
            val cf = ConfigUtil.readFromFile(configPath)
            cf.validateAndNormalizeAuth()
            return cf
        }catch (e: Exception){
            val cause = if(e.cause!=null) "\nCause:${e.cause!!.message}" else ""
            throw ConfigProblemException("Failed when reading configuration file at $configPath." +
                    "\nError: ${e.message}" +
                    "$cause")
        }
    }

    private fun applyConfigFromFile(cff: ConfigsFromFile) {

        val properties = getConfigurationProperties()

        val missing = cff.configs.keys
                .filter { name -> properties.none { it.name == name } }

        if (missing.isNotEmpty()) {
            throw ConfigProblemException("Configuration file defines the following non-existing properties: ${missing.joinToString(", ")}")
        }

        if(cff.configs.isEmpty()){
            //nothing to do
            return
        }

        LoggingUtil.uniqueUserInfo("Applying following ${cff.configs.size} configuration settings: [${cff.configs.keys.joinToString(", ")}]")

        properties.forEach {
            if (cff.configs.contains(it.name)) {
                val value = cff.configs[it.name]
                //TODO is right to assume we are not going to handle null values??? maybe not...
                updateValue(value!!, it)
            }
        }
    }

    private fun handleDeprecated() {
        /*
            TODO If this happens often, then should use annotations.
            eg, could handle specially in Markdown all the deprecated fields
         */
        if (testSuiteFileName.isNotBlank()) {
            LoggingUtil.uniqueUserWarn("Using deprecated option 'testSuiteFileName'")
            outputFilePrefix = testSuiteFileName
            outputFileSuffix = ""
            testSuiteFileName = ""
        }
    }

    /**
     * Note: this can have side-effect of updating some DEFAULT settings
     */
    fun checkMultiFieldConstraints() {
        /*
            Each option field might have specific constraints, setup with @annotations.
            However, there can be multi-field constraints as well.
            Those are defined here.
            They can be checked only once all fields have been updated
         */

        /*
            First start from updating DEFAULT
         */
        if(blackBox){
            if (problemType == ProblemType.DEFAULT) {
                LoggingUtil.uniqueUserWarn("You are doing Black-Box testing, but you did not specify the" +
                        " 'problemType'. The system will default to RESTful API testing.")
                problemType = ProblemType.REST
            }
            if (outputFormat == OutputFormat.DEFAULT) {
                LoggingUtil.uniqueUserWarn("You are doing Black-Box testing, but you did not specify the" +
                        " 'outputFormat'. The system will default to $defaultOutputFormatForBlackBox.")
                outputFormat = defaultOutputFormatForBlackBox
            }
        }
        /*
            the "else" cannot be implemented here, as it will come from the Driver, which has not been called yet.
            It is handled directly in Main
         */
        if(algorithm == Algorithm.DEFAULT ){
            algorithm = if(blackBox) defaultAlgorithmForBlackBox else defaultAlgorithmForWhiteBox
        }


        if (!blackBox && bbSwaggerUrl.isNotBlank()) {
            throw ConfigProblemException("'bbSwaggerUrl' should be set only in black-box mode")
        }
        if (!blackBox && bbTargetUrl.isNotBlank()) {
            throw ConfigProblemException("'bbTargetUrl' should be set only in black-box mode")
        }

        // ONUR, this line is changed since it did not compile in the previous case.
        if (!endpointFocus.isNullOrBlank() && !endpointPrefix.isNullOrBlank()) {
            throw ConfigProblemException("both 'endpointFocus' and 'endpointPrefix' are set")
        }

        if (blackBox && !bbExperiments) {

            if (problemType == ProblemType.REST && bbSwaggerUrl.isNullOrBlank()) {
                throw ConfigProblemException("In black-box mode for REST APIs, you must set the bbSwaggerUrl option")
            }
            if (problemType == ProblemType.GRAPHQL && bbTargetUrl.isNullOrBlank()) {
                throw ConfigProblemException("In black-box mode for GraphQL APIs, you must set the bbTargetUrl option")
            }
        }

        if (!blackBox && bbExperiments) {
            throw ConfigProblemException("Cannot setup bbExperiments without black-box mode")
        }

        if (!blackBox && ratePerMinute > 0) {
            throw ConfigProblemException("ratePerMinute is used only for black-box testing")
        }

        if (blackBox && ratePerMinute <= 0) {
            LoggingUtil.uniqueUserWarn("You have not setup 'ratePerMinute'. If you are doing testing of" +
                    " a remote service which you do not own, you might want to put a rate-limiter to prevent" +
                    " EvoMaster from bombarding such service with HTTP requests.")
        }

        if (!blackBox && outputFormat == OutputFormat.PYTHON_UNITTEST) {
            throw ConfigProblemException("Python output is used only for black-box testing")
        }

        when (stoppingCriterion) {
            StoppingCriterion.TIME -> if (maxEvaluations != defaultMaxEvaluations) {
                throw ConfigProblemException("Changing number of max actions, but stopping criterion is time")
            }

            StoppingCriterion.ACTION_EVALUATIONS, StoppingCriterion.INDIVIDUAL_EVALUATIONS ->
                if (maxTimeInSeconds != defaultMaxTimeInSeconds || maxTime != defaultMaxTime) {
                throw ConfigProblemException("Changing max time, but stopping criterion is based on evaluations")
            }
        }

        if (shouldGenerateSqlData() && !heuristicsForSQL) {
            throw ConfigProblemException("Cannot generate SQL data if you not enable " +
                    "collecting heuristics with 'heuristicsForSQL'")
        }
        if (generateSqlDataWithDSE && generateSqlDataWithSearch) {
            throw ConfigProblemException("Cannot generate SQL data with both DSE and search")
        }

        if (heuristicsForSQL && !extractSqlExecutionInfo) {
            throw ConfigProblemException("Cannot collect heuristics SQL data if you not enable " +
                    "extracting SQL execution info with 'extractSqlExecutionInfo'")
        }
        if (!heuristicsForSQL && heuristicsForSQLAdvanced) {
            throw ConfigProblemException("Advanced SQL heuristics requires enabling base ones as well")
        }

        if (shouldGenerateMongoData() && !heuristicsForMongo) {
            throw ConfigProblemException("Cannot generate Mongo data if you not enable " +
                    "collecting heuristics with 'heuristicsForMongo'")
        }

        if (shouldGenerateMongoData() && !extractMongoExecutionInfo) {
            throw ConfigProblemException("Cannot generate Mongo data if you not enable " +
                    "extracting Mongo execution info with 'extractMongoExecutionInfo'")
        }

        if (enableTrackEvaluatedIndividual && enableTrackIndividual) {
            throw ConfigProblemException("When tracking EvaluatedIndividual, it is not necessary to track individual")
        }

        if (adaptiveGeneSelectionMethod != GeneMutationSelectionMethod.NONE && probOfArchiveMutation > 0 && !weightBasedMutationRate)
            throw ConfigProblemException("When applying adaptive gene selection, weight-based mutation rate should be enabled")

        if (probOfArchiveMutation > 0 && !enableTrackEvaluatedIndividual)
            throw ConfigProblemException("Archive-based solution is only applicable when enable of tracking of EvaluatedIndividual.")

        if (doCollectImpact && !enableTrackEvaluatedIndividual)
            throw ConfigProblemException("Impact collection should be applied together with tracking EvaluatedIndividual")

        if (isEnabledTaintAnalysis() && !useMethodReplacement) {
            throw ConfigProblemException("Base Taint Analysis requires 'useMethodReplacement' option")
        }

        if ((outputFilePrefix.contains("-") || outputFileSuffix.contains("-"))
                && outputFormat.isJavaOrKotlin()) { //TODO also for C#?
            throw ConfigProblemException("In JVM languages, you cannot use the symbol '-' in test suite file name")
        }

        if (seedTestCases && seedTestCasesPath.isNullOrBlank()) {
            throw ConfigProblemException("When using the seedTestCases option, you must specify the file path of the test cases with the seedTestCasesPath option")
        }

        // Clustering constraints: the executive summary is not really meaningful without the clustering
//        if (executiveSummary && testSuiteSplitType != TestSuiteSplitType.FAULTS) {
//            executiveSummary = false
//            LoggingUtil.uniqueUserWarn("The option to turn on Executive Summary is only meaningful when clustering is turned on (--testSuiteSplitType CLUSTERING). " +
//                    "The option has been deactivated for this run, to prevent a crash.")
//            //throw ConfigProblemException("The option to turn on Executive Summary is only meaningful when clustering is turned on (--testSuiteSplitType CLUSTERING).")
//        }

        if (problemType == ProblemType.RPC
                && createTests
                && (enablePureRPCTestGeneration || enableRPCAssertionWithInstance)
                && outputFormat != OutputFormat.DEFAULT && (!outputFormat.isJavaOrKotlin())) {
            throw ConfigProblemException("when generating RPC tests with actual object instances in specified format, outputFormat only supports Java or Kotlin now")
        }

        val jaCoCo_on = jaCoCoAgentLocation.isNotBlank() && jaCoCoCliLocation.isNotBlank() && jaCoCoOutputFile.isNotBlank()
        val jaCoCo_off = jaCoCoAgentLocation.isBlank() && jaCoCoCliLocation.isBlank() && jaCoCoOutputFile.isBlank()

        if (!jaCoCo_on && !jaCoCo_off) {
            throw ConfigProblemException("JaCoCo location for agent/cli and output options must be all set or all left empty")
        }

        if (!taintOnSampling && useGlobalTaintInfoProbability > 0) {
            throw ConfigProblemException("Need to activate taintOnSampling to use global taint info")
        }

        if (maxLengthForStringsAtSamplingTime > maxLengthForStrings) {
            throw ConfigProblemException("Max length at sampling time $maxLengthForStringsAtSamplingTime" +
                    " cannot be greater than maximum string length $maxLengthForStrings")
        }

        if (saveMockedResponseAsSeparatedFile && testResourcePathToSaveMockedResponse.isBlank())
            throw ConfigProblemException("testResourcePathToSaveMockedResponse cannot be empty if it is required to save mocked responses in separated files (ie, saveMockedResponseAsSeparatedFile=true)")

        if (saveScheduleTaskInvocationAsSeparatedFile && testResourcePathToSaveMockedResponse.isBlank())
            throw ConfigProblemException("testResourcePathToSaveMockedResponse cannot be empty if it is required to save schedule task invocation in separated files (ie, saveScheduleTaskInvocationAsSeparatedFile=true)")

        if (probRestDefault + probRestExamples > 1) {
            throw ConfigProblemException("Invalid combination of probabilities for probRestDefault and probRestExamples. " +
                    "Their sum should be lower or equal to 1.")
        }

        if(security && !minimize){
            throw ConfigProblemException("The use of 'security' requires 'minimize'")
        }

        if(!security && ssrf) {
            throw ConfigProblemException("The use of 'ssrf' requires 'security'")
        }

        if (ssrf &&
            vulnerableInputClassificationStrategy == VulnerableInputClassificationStrategy.LLM &&
            !languageModelConnector) {
            throw ConfigProblemException("Language model connector is disabled. Unable to run the input classification using LLM.")
        }

        if (languageModelConnector && languageModelServerURL.isNullOrEmpty()) {
            throw ConfigProblemException("Language model server URL cannot be empty.")
        }

        if (languageModelConnector && languageModelName.isNullOrEmpty()) {
            throw ConfigProblemException("Language model name cannot be empty.")
        }

        if(prematureStop.isNotEmpty() && stoppingCriterion != StoppingCriterion.TIME){
            throw ConfigProblemException("The use of 'prematureStop' is meaningful only if the stopping criterion" +
                    " 'stoppingCriterion' is based on time")
        }

        if(blackBox){
            if(sutControllerHost != ControllerConstants.DEFAULT_CONTROLLER_HOST){
                throw ConfigProblemException("Changing 'sutControllerHost' has no meaning in black-box testing, as no controller is used")
            }
            if(!overrideOpenAPIUrl.isNullOrBlank()){
                throw ConfigProblemException("Changing 'overrideOpenAPIUrl' has no meaning in black-box testing, as no controller is used")
            }
        }
        if(dockerLocalhost && !runningInDocker){
            throw ConfigProblemException("Specifying 'dockerLocalhost' only makes sense when running EvoMaster inside Docker.")
        }
        /*
            FIXME: we shouldn't crash if a user put createTests to false and does not update all setting depending on it,
            like writeWFCReport.
            TODO however, we should issue some WARN message.
            ie. we should have a distinction between @Requires (which should crash) and something like
            @DependOn that does not lead to a crash, but just a warning
         */
//        if(writeWFCReport && !createTests){
//            throw ConfigProblemException("Cannot create a WFC Report if tests are not generated (i.e., 'createTests' is false)")
//        }
    }

    private fun checkPropertyConstraints(m: KMutableProperty<*>) {
        val parameterValue = m.getter.call(this).toString()

        //check value against constraints on its field, if any
        m.annotations.find { it is Min }?.also {
            it as Min
            if (parameterValue.toDouble() < it.min) {
                throw ConfigProblemException("Failed to handle Min ${it.min} constraint for" +
                        " parameter '${m.name}' with value $parameterValue")
            }
        }

        m.annotations.find { it is Max }?.also {
            it as Max
            if (parameterValue.toDouble() > it.max) {
                throw ConfigProblemException("Failed to handle Max ${it.max} constraint for" +
                        " parameter '${m.name}' with value $parameterValue")
            }
        }

        m.annotations.find { it is Probability }?.also {
            it as Probability
            val p = parameterValue.toDouble()
            if (p < 0 || p > 1) {
                throw ConfigProblemException("Failed to handle probability constraint for" +
                        " parameter '${m.name}' with value $parameterValue. The value must be in [0,1].")
            }
        }

        m.annotations.find { it is Url }?.also {
            if (!parameterValue.isNullOrBlank()) {
                try {
                    URL(parameterValue)
                } catch (e: MalformedURLException) {
                    throw ConfigProblemException("Parameter '${m.name}' with value $parameterValue is" +
                            " not a valid URL: ${e.message}")
                }
            }
        }

        m.annotations.find { it is Regex }?.also {
            it as Regex
            if (!parameterValue.matches(kotlin.text.Regex(it.regex))) {
                throw ConfigProblemException("Parameter '${m.name}' with value $parameterValue is" +
                        " not matching the regex: ${it.regex}")
            }
        }

        m.annotations.find { it is Folder }?.also {
            val path = try {
                Paths.get(parameterValue).toAbsolutePath()
            } catch (e: InvalidPathException) {
                throw ConfigProblemException("Parameter '${m.name}' is not a valid FS path: ${e.message}")
            }

            // here, it first checks if the path exists,since the path does not exist it does not check
            // if it is writable
            if (Files.exists(path) && !Files.isWritable(path)) {
                throw ConfigProblemException("Parameter '${m.name}' refers to a folder that already" +
                        " exists, but that cannot be written to: $path")
            }

            if (Files.exists(path) && !Files.isDirectory(path)) {
                throw ConfigProblemException("Parameter '${m.name}' refers to a file that already" +
                        " exists, but that it is not a folder: $path")
            }

            // if the path does not exist and if the directory structure cannot be created, inform the user
            if(!Files.exists(path)) {
                try {
                    Files.createDirectories(path)
                }
                catch(e : Exception) {
                    throw ConfigProblemException("Parameter '${m.name}' refers to a file that does not exist" +
                            ", but the provided file path cannot be used to create a directory: $path" +
                            "\nPlease check file permissions of parent directories")
                }
            }
        }

        m.annotations.find { it is FilePath }?.also {
            val fp = it as FilePath
            if (!fp.canBeBlank || parameterValue.isNotBlank()) {

                val path = try {
                    Paths.get(parameterValue).toAbsolutePath()
                } catch (e: InvalidPathException) {
                    throw ConfigProblemException("Parameter '${m.name}' is not a valid FS path: ${e.message}")
                }

                if (Files.exists(path) && !Files.isWritable(path)) {
                    throw ConfigProblemException("Parameter '${m.name}' refers to a file that already" +
                            " exists, but that cannot be written/replace to: $path")
                }
                if (Files.exists(path) && Files.isDirectory(path)) {
                    throw ConfigProblemException("Parameter '${m.name}' refers to a file that is instead an" +
                            " existing folder: $path")
                }
            }
        }
    }

    private fun updateProperty(options: OptionSet, m: KMutableProperty<*>) {
        //update value, but only if it was in the specified options.
        //WARNING: without this check, it would reset to default for fields not in "options"
        if (!options.has(m.name)) {
            return
        }

        val opt = try{
            options.valueOf(m.name)?.toString()
        } catch (e: OptionException){
          throw  ConfigProblemException("Error in parsing configuration option '${m.name}'. Library message: ${e.message}")
        } ?: throw ConfigProblemException("Value not found for property '${m.name}'")

        updateValue(opt, m)
    }

    private fun updateValue(optionValue: String, m: KMutableProperty<*>) {

        val returnType = m.returnType.javaType as Class<*>

        /*
                TODO: ugly checks. But not sure yet if can be made better in Kotlin.
                Could be improved with isSubtypeOf from 1.1?
                http://stackoverflow.com/questions/41553647/kotlin-isassignablefrom-and-reflection-type-checks
             */
        try {
            if (Integer.TYPE.isAssignableFrom(returnType)) {
                m.setter.call(this, Integer.parseInt(optionValue))

            } else if (java.lang.Long.TYPE.isAssignableFrom(returnType)) {
                m.setter.call(this, java.lang.Long.parseLong(optionValue))

            } else if (java.lang.Double.TYPE.isAssignableFrom(returnType)) {
                m.setter.call(this, java.lang.Double.parseDouble(optionValue))

            } else if (java.lang.Boolean.TYPE.isAssignableFrom(returnType)) {
                m.setter.call(this, parseBooleanStrict(optionValue))

            } else if (java.lang.String::class.java.isAssignableFrom(returnType)) {
                m.setter.call(this, optionValue)

            } else if (returnType.isEnum) {
                val valueOfMethod = returnType.getDeclaredMethod("valueOf",
                        java.lang.String::class.java)
                m.setter.call(this, valueOfMethod.invoke(null, optionValue))

            } else {
                throw IllegalStateException("BUG: cannot handle type $returnType")
            }
        } catch (e: Exception) {
            throw ConfigProblemException("Failed to handle property '${m.name}': ${e.message}")
        }
    }

    private fun parseBooleanStrict(s: String?) : Boolean{
        if(s==null){
            throw IllegalArgumentException("value is 'null'")
        }
        if(s.equals("true", true)) return true
        if(s.equals("false", true)) return false
        throw IllegalArgumentException("Invalid boolean value: $s")
    }

    fun shouldGenerateSqlData() = isUsingAdvancedTechniques() && (generateSqlDataWithDSE || generateSqlDataWithSearch)

    fun shouldGenerateMongoData() = generateMongoData

    fun experimentalFeatures(): List<String> {

        val properties = getConfigurationProperties()
                .filter { it.annotations.find { it is Experimental } != null }
                .filter {
                    val returnType = it.returnType.javaType as Class<*>
                    when {
                        java.lang.Boolean.TYPE.isAssignableFrom(returnType) -> it.getter.call(this) as Boolean
                        it.annotations.find { p -> p is Probability && p.activating } != null -> (it.getter.call(this) as Double) > 0
                        else -> false
                    }
                }
                .map { it.name }

        val enums = getConfigurationProperties()
                .filter {
                    val returnType = it.returnType.javaType as Class<*>
                    if (returnType.isEnum) {
                        val e = it.getter.call(this)
                        val f = returnType.getField(e.toString())
                        f.annotations.find { it is Experimental } != null
                    } else {
                        false
                    }
                }
                .map { "${it.name}=${it.getter.call(this)}" }

        return properties.plus(enums)
    }

//------------------------------------------------------------------------
//--- custom annotations

    /**
     * Configuration (CFG in short) for EvoMaster.
     * Properties annotated with [Cfg] can be set from
     * command line.
     * The code in this class uses reflection, on each property
     * marked with this annotation, to build the list of available
     * modifiable configurations.
     */
    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class Cfg(val description: String)

    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class Min(val min: Double)

    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class Max(val max: Double)

    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class Url

    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class Regex(val regex: String)


    /**
     * For internal configurations that we introduced just to get more info on EM,
     * with aim of debugging issues.
     */
    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class Debug


    /**
     * A double value between 0 and 1
     */
    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class Probability(
            /**
             * Specify if this probability would activate a functionality if greater than 0.
             * If not, it might still not be used, depending on other configurations.
             * This is mainly needed when dealing with @Experimental probabilities that must
             * be put to 0 if they would activate a new feature that is still unstable
             */
            val activating: Boolean = true
    )


    /**
     * This annotation is used to represent properties controlling
     * features that are still work in progress.
     * Do not use them (yet) in production.
     */
    @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
    @MustBeDocumented
    annotation class Experimental


    /**
     * This represent one of the main properties to set in EvoMaster.
     * Those are the ones most likely going to be set by practitioners.
     * Note: most of the other properties are mainly for experiments
     */
    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class Important(
            /**
             * The lower value, the more importance.
             * This only impact of options are sorted when displayed
             */
            val priority: Double
    )

    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class Folder

    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    annotation class FilePath(val canBeBlank: Boolean = false)

//------------------------------------------------------------------------

    /**
     * Info for authentication, read from configuration file, if any
     */
    var authFromFile: List<AuthenticationDto>? = null


//------------------------------------------------------------------------
//--- properties

    /*
        WARNING
        if any change is made here, the "options.md" MUST be recreated
        with ConfigToMarkdown

        You will also need to check if any special character you use in the
        descriptions end-up in some screwed-up Markdown layout
     */

    //----- "Important" options, sorted by priority --------------

    val defaultMaxTime = "60s"

    @Important(1.0)
    @Cfg("Maximum amount of time allowed for the search. " +
            " The time is expressed with a string where hours (`h`), minutes (`m`) and" +
            " seconds (`s`) can be specified, e.g., `1h10m120s` and `72m` are both valid" +
            " and equivalent." +
            " Each component (i.e., `h`, `m` and `s`) is optional, but at least one must be specified. " +
            " In other words, if you need to run the search for just `30` seconds, you can write `30s` " +
            " instead of `0h0m30s`." +
            " **The more time is allowed, the better results one can expect**." +
            " But then of course the test generation will take longer." +
            " For how long should _EvoMaster_ be left run?" +
            " The default 1 _minute_ is just for demonstration." +
            " __We recommend to run it between 1 and 24 hours__, depending on the size and complexity " +
            " of the tested application." +
            " You can get better results by combining this option with `--prematureStop`." +
            " For example, something like `--maxTime 24h --prematureStop 1h` will run the search for 24 hours," +
            " but then it will stop at any point in time in which there has be no improvement in the last hour."
    )
    @Regex(timeRegex)
    var maxTime = defaultMaxTime

    @Important(1.01)
    @Cfg("Max amount of time the search is going to wait since last improvement (on metrics we optimize for," +
            " like fault finding and code/schema coverage)." +
            " If there is no improvement within this allotted max time, then the search will be prematurely stopped," +
            " regardless of what specified in --maxTime option.")
    @Regex("($timeRegex)|(^$)")
    var prematureStop : String = ""

    enum class PrematureStopStrategy{
        ANY, NEW
    }

    @Experimental
    @Cfg("Specify how 'improvement' is defined: either any kind of improvement even if partial (ANY)," +
            " or at least one new target is fully covered (NEW).")
    var prematureStopStrategy = PrematureStopStrategy.NEW

    @Important(1.1)
    @Cfg("The path directory of where the generated test classes should be saved to")
    @Folder
    var outputFolder = "generated_tests"


    val defaultConfigPath = "em.yaml"

    @Important(1.2)
    @Cfg("File path for file with configuration settings. Supported formats are YAML and TOML." +
            " When EvoMaster starts, it will read such file and import all configurations from it.")
    @Regex(".*\\.(yml|yaml|toml)")
    @FilePath
    var configPath: String = defaultConfigPath


    @Important(2.0)
    @Cfg("The name prefix of generated file(s) with the test cases, without file type extension." +
            " In JVM languages, if the name contains '.', folders will be created to represent" +
            " the given package structure." +
            " Also, in JVM languages, should not use '-' in the file name, as not valid symbol" +
            " for class identifiers." +
            " This prefix be combined with the outputFileSuffix to combined the final name." +
            " As EvoMaster can split the generated tests among different files, each will get a label," +
            " and the names will be in the form prefix+label+suffix.")
    @Regex("[-a-zA-Z\$_][-0-9a-zA-Z\$_]*(.[-a-zA-Z\$_][-0-9a-zA-Z\$_]*)*")
    var outputFilePrefix = "EvoMaster"

    @Important(2.0)
    @Cfg("The name suffix for the generated file(s), to be added before the file type extension." +
            " As EvoMaster can split the generated tests among different files, each will get a label," +
            " and the names will be in the form prefix+label+suffix.")
    @Regex("[-a-zA-Z\$_][-0-9a-zA-Z\$_]*(.[-a-zA-Z\$_][-0-9a-zA-Z\$_]*)*")
    var outputFileSuffix = "Test"


    @Deprecated("Should use outputFilePrefix and outputFileSuffix")
    @Cfg("DEPRECATED. Rather use _outputFilePrefix_ and _outputFileSuffix_")
    var testSuiteFileName = ""

    @Important(2.0)
    @Cfg("Specify in which format the tests should be outputted." +
            " If left on `DEFAULT`, for white-box testing then the value specified in the _EvoMaster Driver_ will be used." +
            " On the other hand, for black-box testing it will default to a predefined type (e.g., Python).")
    var outputFormat = OutputFormat.DEFAULT

    @Important(2.1)
    @Cfg("Enforce timeout (in seconds) in the generated tests." +
            " This feature might not be supported in all frameworks." +
            " If 0 or negative, the timeout is not applied.")
    var testTimeout = 60

    @Important(3.0)
    @Cfg("Use EvoMaster in black-box mode. This does not require an EvoMaster Driver up and running. However, you will need to provide further option to specify how to connect to the SUT")
    var blackBox = false

    @Important(3.2)
    @Cfg("When in black-box mode for REST APIs, specify the URL of where the OpenAPI/Swagger schema can be downloaded from." +
            " If the schema is on the local machine, you can use a URL starting with 'file://'." +
            " If the given URL is neither starting with 'file' nor 'http', then it will be treated as a local file path.")
    var bbSwaggerUrl: String = ""

    @Important(3.5)
    @Url
    @Cfg("When in black-box mode, specify the URL of where the SUT can be reached, e.g.," +
            " http://localhost:8080 ." +
            " In REST, if this is missing, the URL will be inferred from OpenAPI/Swagger schema." +
            " In GraphQL, this must point to the entry point of the API, e.g.," +
            " http://localhost:8080/graphql .")
    var bbTargetUrl: String = ""


    @Important(3.7)
    @Cfg("Rate limiter, of how many actions to do per minute. For example, when making HTTP calls towards" +
            " an external service, might want to limit the number of calls to avoid bombarding such service" +
            " (which could end up becoming equivalent to a DoS attack)." +
            " A value of zero or negative means that no limiter is applied." +
            " This is needed only for black-box testing of remote services.")
    var ratePerMinute = 0

    @Important(4.0)
    @Regex(headerRegex)
    @Cfg("In black-box testing, we still need to deal with authentication of the HTTP requests." +
            " With this parameter it is possible to specify a HTTP header that is going to be added to most requests." +
            " This should be provided in the form _name:value_. If more than 1 header is needed, use as well the" +
            " other options _header1_ and _header2_.")
    var header0 = ""

    @Important(4.1)
    @Regex(headerRegex)
    @Cfg("See documentation of _header0_.")
    var header1 = ""

    @Important(4.2)
    @Regex(headerRegex)
    @Cfg("See documentation of _header0_.")
    var header2 = ""


    @Important(5.0)
    @Cfg("Concentrate search on only one single REST endpoint")
    var endpointFocus: String? = null

    @Important(5.1)
    @Cfg("Concentrate search on a set of REST endpoints defined by a common prefix")
    var endpointPrefix: String? = null

    @Important(5.2)
    @Cfg("Comma-separated list of OpenAPI/Swagger 'tags' definitions." +
            " Only the REST endpoints having at least one of such tags will be fuzzed." +
            " If no tag is specified here, then such filter is not applied.")
    var endpointTagFilter: String? = null

    @Important(6.0)
    @Cfg("Host name or IP address of where the SUT EvoMaster Controller Driver is listening on." +
            " This option is only needed for white-box testing.")
    var sutControllerHost = ControllerConstants.DEFAULT_CONTROLLER_HOST


    @Important(6.1)
    @Cfg("TCP port of where the SUT EvoMaster Controller Driver is listening on." +
            " This option is only needed for white-box testing.")
    @Min(0.0)
    @Max(maxTcpPort)
    var sutControllerPort = ControllerConstants.DEFAULT_CONTROLLER_PORT


    @Important(7.0)
    @Url
    @Cfg("If specified, override the OpenAPI URL location given by the EvoMaster Driver." +
        " This option is only needed for white-box testing.")
    var overrideOpenAPIUrl = ""

    //-------- other options -------------

    @Cfg("Inform EvoMaster process that it is running inside Docker." +
            " Users should not modify this parameter, as it is set automatically in the Docker image of EvoMaster.")
    var runningInDocker = false

    /**
     * TODO this is currently not implemented.
     * Even if did, there would still be major issues with handling WireMock.
     * Until we can think of a good solution there, no point in implementing this.
     */
    @Experimental
    @Cfg("Replace references to 'localhost' to point to the actual host machine." +
            " Only needed when running EvoMaster inside Docker.")
    var dockerLocalhost = false


    @FilePath
    @Cfg("When generating tests in JavaScript, there is the need to know where the driver is located in respect to" +
            " the generated tests")
    var jsControllerPath = "./app-driver.js"


    @Cfg("At times, we need to run EvoMaster with printed logs that are deterministic." +
            " For example, this means avoiding printing out time-stamps.")
    var avoidNonDeterministicLogs = false

    enum class Algorithm {
        DEFAULT, SMARTS, MIO, RANDOM, WTS, MOSA, RW,
        StandardGA, MonotonicGA, SteadyStateGA // These 3 are still work-in-progress
    }

    @Cfg("The algorithm used to generate test cases. The default depends on whether black-box or white-box testing is done.")
    var algorithm = Algorithm.DEFAULT

    /**
     * Workaround for issues with annotations that can not be applied on ENUM values,
     * like @Experimental
     * */
    interface WithExperimentalOptions {
        fun isExperimental(): Boolean
    }

    enum class ProblemType(private val experimental: Boolean) : WithExperimentalOptions {
        DEFAULT(experimental = false),
        REST(experimental = false),
        GRAPHQL(experimental = false),
        RPC(experimental = true),
        WEBFRONTEND(experimental = true);

        override fun isExperimental() = experimental
    }

    @Cfg("The type of SUT we want to generate tests for, e.g., a RESTful API." +
            " If left to DEFAULT, the type will be inferred from the EM Driver." +
            " However, in case of ambiguities (e.g., the driver specifies more than one type)," +
            " then this field must be set with a specific type." +
            " This is also the case for Black-Box testing where there is no EM Driver." +
            " In this latter case, the system defaults to handle REST APIs.")
    var problemType = ProblemType.DEFAULT


    @Cfg("Specify if test classes should be created as output of the tool. " +
            "Usually, you would put it to 'false' only when debugging EvoMaster itself")
    var createTests = true

    enum class TestSuiteSplitType {
        NONE,
        FAULTS
        //CODE //This was never properly implemented
    }

    @Cfg("Instead of generating a single test file, it could be split in several files, according to different strategies")
    var testSuiteSplitType = TestSuiteSplitType.FAULTS

    @Experimental
    @Cfg("Specify the maximum number of tests to be generated in one test suite. " +
            "Note that a negative number presents no limit per test suite")
    var maxTestsPerTestSuite = -1

    @Experimental
    @Deprecated("Temporarily removed, due to oracle refactoring. It might come back in future in a different form")
    @Cfg("Generate an executive summary, containing an example of each category of potential faults found." +
            "NOTE: This option is only meaningful when used in conjunction with test suite splitting.")
    var executiveSummary = false

    @Cfg("The Distance Metric Last Line may use several values for epsilon." +
            "During experimentation, it may be useful to adjust these values. Epsilon describes the size of the neighbourhood used for clustering, so may result in different clustering results." +
            "Epsilon should be between 0.0 and 1.0. If the value is outside of that range, epsilon will use the default of 0.8.")
    @Min(0.0)
    @Max(1.0)
    var lastLineEpsilon = 0.8

    @Cfg("The Distance Metric Error Text may use several values for epsilon." +
            "During experimentation, it may be useful to adjust these values. Epsilon describes the size of the neighbourhood used for clustering, so may result in different clustering results." +
            "Epsilon should be between 0.0 and 1.0. If the value is outside of that range, epsilon will use the default of 0.8.")
    @Min(0.0)
    @Max(1.0)
    var errorTextEpsilon = 0.8

    @Cfg("The seed for the random generator used during the search. " +
            "A negative value means the CPU clock time will be rather used as seed")
    var seed: Long = -1

    @Cfg("Limit of number of individuals per target to keep in the archive")
    @Min(1.0)
    var archiveTargetLimit = 10

    @Cfg("Probability of sampling a new individual at random")
    @Probability
    var probOfRandomSampling = 0.8

    @Cfg("The percentage of passed search before starting a more focused, less exploratory one")
    @PercentageAsProbability(true)
    var focusedSearchActivationTime = 0.8

    @Cfg("Number of applied mutations on sampled individuals, at the start of the search")
    @Min(0.0)
    var startNumberOfMutations = 1

    @Cfg("Number of applied mutations on sampled individuals, by the end of the search")
    @Min(0.0)
    var endNumberOfMutations = 10

    enum class StoppingCriterion {
        TIME,
        ACTION_EVALUATIONS,
        INDIVIDUAL_EVALUATIONS
    }

    @Cfg("Stopping criterion for the search")
    var stoppingCriterion = StoppingCriterion.TIME


    val defaultMaxEvaluations = 1000

    @Cfg("Maximum number of action or individual evaluations (depending on chosen stopping criterion)" +
            " for the search. A fitness evaluation can be composed of 1 or more actions," +
            " like for example REST calls or SQL setups." +
            " The more actions are allowed, the better results one can expect." +
            " But then of course the test generation will take longer." +
            " Only applicable depending on the stopping criterion.")
    @Min(1.0)
    var maxEvaluations = defaultMaxEvaluations

    val defaultMaxTimeInSeconds = 0

    @Cfg("Maximum number of seconds allowed for the search." +
            " The more time is allowed, the better results one can expect." +
            " But then of course the test generation will take longer." +
            " Only applicable depending on the stopping criterion." +
            " If this value is 0, the setting 'maxTime' will be used instead.")
    @Min(0.0)
    var maxTimeInSeconds = defaultMaxTimeInSeconds

    @Cfg("Whether or not writing statistics of the search process. " +
            "This is only needed when running experiments with different parameter settings")
    var writeStatistics = false

    @Cfg("Where the statistics file (if any) is going to be written (in CSV format)")
    @FilePath
    var statisticsFile = "statistics.csv"


    enum class AIResponseClassifierModel {
        /**
         * No classification is performed.
         */
        NONE,

        /**
         * Gaussian Model.
         * Assumes the data follows a bell-shaped curve, parameterized by mean and variance.
         */
        GAUSSIAN,

        /**
         * Kernel Density Estimation (KDE).
         * A non-parametric method for estimating the probability density function.
         */
        KDE,

        /**
         * K-Nearest Neighbors (KNN).
         * Classifies a point based on the majority label among its k closest neighbors.
         */
        KNN,

        /**
         * Neural Network (NN).
         * A computational model inspired by biological neural systems, consisting of layers of interconnected neurons.
         * Neural networks learn patterns from data to capture underlying nonlinear relationships
         * and to perform flexible classification
         */
        NN,

        /**
         * Generalized Linear Model (GLM).
         * Extends linear regression to handle non-normal response distributions.
         */
        GLM,

        /**
         * Rule-Based Deterministic Model.
         * Uses predefined, fixed rules for classification,
         * providing clear and structured decision logic as an
         * alternative to probabilistic or statistical methods.
         */
        DETERMINISTIC
    }



    @Experimental
    @Cfg("Model used to learn input constraints and infer response status before making request.")
    var aiModelForResponseClassification = AIResponseClassifierModel.NONE

    @Experimental
    @Cfg("Learning rate controlling the step size during parameter updates in classifiers. " +
            "Relevant for gradient-based models such as GLM and neural networks. " +
            "A smaller value ensures stable but slower convergence, while a larger value speeds up " +
            "training but may cause instability.")
    var aiResponseClassifierLearningRate: Double = 0.01

    @Experimental
    @Cfg("Number of training iterations required to update classifier parameters. " +
                "For example, in the Gaussian model this affects mean and variance updates. " +
                "For neural network (NN) models, the warm-up should typically be larger than 1000.")
    var aiResponseClassifierWarmup : Int = 10


    enum class EncoderType {

        /** Use raw values without any transformation. */
        RAW,

        /** Normalize values to a standard scale (e.g., zero mean and unit variance). */
        NORMAL,

        /** Scale the vector to have unit length, making it a point on the unit sphere. */
        UNIT_NORMAL
    }

    @Experimental
    @Cfg("The encoding strategy applied to transform raw data to the encoded version.")
    var aiEncoderType = EncoderType.RAW


    @Experimental
    @Min(1.0)
    @Cfg("When the Response Classifier determines an action is going to fail, specify how many attempts will" +
            " be tried at fixing it.")
    var maxRepairAttemptsInResponseClassification = 100

    enum class AIClassificationRepairActivation{
        /*
            TODO we might think of other techniques as well... and then experiment with them
         */
        PROBABILITY, THRESHOLD
    }

    @Experimental
    @PercentageAsProbability
    @Cfg("If using THRESHOLD for AI Classification Repair, specify its value." +
            " All classifications with probability equal or above such threshold value will be accepted.")
    var classificationRepairThreshold = 0.8

    @Experimental
    @Cfg("Specify how the classification of actions's response will be used to execute a possible repair on the action.")
    var aiClassifierRepairActivation = AIClassificationRepairActivation.THRESHOLD

    @Cfg("Output a JSON file representing statistics of the fuzzing session, written in the WFC Report format." +
            " This also includes a index.html web application to visualize such data.")
    var writeWFCReport = true

    @Cfg("If creating a WFC Report as output, specify if should not generate the index.html web app, i.e., only" +
            " the JSON report file will be created.")
    var writeWFCReportExcludeWebApp = false

    @Cfg("Whether should add to an existing statistics file, instead of replacing it")
    var appendToStatisticsFile = false

    @Cfg("If positive, check how often, in percentage % of the budget, to collect statistics snapshots." +
            " For example, every 5% of the time.")
    @Max(50.0)
    var snapshotInterval = -1.0

    @Cfg("Where the snapshot file (if any) is going to be written (in CSV format)")
    @FilePath
    var snapshotStatisticsFile = "snapshot.csv"

    @Cfg("An id that will be part as a column of the statistics file (if any is generated)")
    var statisticsColumnId = "-"

    @Cfg("When running experiments and statistic files are generated, all configs are saved." +
            " So, this one can be used as extra label for classifying the experiment")
    var labelForExperiments = "-"

    @Cfg("Further label to represent the names of CONFIGS sets in experiment scripts, e.g., exp.py")
    var labelForExperimentConfigs = "-"

    @Cfg("Whether we should collect data on the extra heuristics. Only needed for experiments.")
    var writeExtraHeuristicsFile = false

    @Cfg("Where the extra heuristics file (if any) is going to be written (in CSV format)")
    @FilePath
    var extraHeuristicsFile = "extra_heuristics.csv"

    @Experimental
    @Cfg("Enable to print snapshots of the generated tests during the search in an interval defined in snapshotsInterval.")
    var enableWriteSnapshotTests = false

    @Experimental
    @Cfg("The size (in seconds) of the interval that the snapshots will be printed, if enabled.")
    var writeSnapshotTestsIntervalInSeconds = 3600 // ie, 1 hour

    enum class SecondaryObjectiveStrategy {
        AVG_DISTANCE,
        AVG_DISTANCE_SAME_N_ACTIONS,
        BEST_MIN
    }

    @Cfg("Strategy used to handle the extra heuristics in the secondary objectives")
    var secondaryObjectiveStrategy = SecondaryObjectiveStrategy.AVG_DISTANCE_SAME_N_ACTIONS

    @Cfg("Whether secondary objectives are less important than test bloat control")
    var bloatControlForSecondaryObjective = false

    @Cfg("Specify minimum size when bloatControlForSecondaryObjective")
    @Min(0.0)
    var minimumSizeControl = 2

    @Cfg("Probability of applying a mutation that can change the structure of a test")
    @Probability
    var structureMutationProbability = 0.5

    @Experimental
    @Cfg("Probability of applying a mutation that can change the structure of test's initialization if it has")
    @Probability
    var initStructureMutationProbability = 0.0

    @Experimental
    @Cfg("Specify a maximum number of handling (remove/add) init actions at once, e.g., add 3 init actions at most")
    @Min(0.0)
    var maxSizeOfMutatingInitAction = 0

    // Man: need to check it with Andrea about whether we consider it as a generic option
    @Experimental
    @Cfg("Specify a probability of applying a smart structure mutator for initialization of the individual")
    @Probability
    var probOfSmartInitStructureMutator = 0.0

    enum class GeneMutationStrategy {
        ONE_OVER_N,
        ONE_OVER_N_BIASED_SQL
    }

    @Cfg("Strategy used to define the mutation probability")
    var geneMutationStrategy = GeneMutationStrategy.ONE_OVER_N_BIASED_SQL

    enum class FeedbackDirectedSampling {
        NONE,
        LAST,
        FOCUSED_QUICKEST
    }

    @Cfg("Specify whether when we sample from archive we do look at the most promising targets for which we have had a recent improvement")
    var feedbackDirectedSampling = FeedbackDirectedSampling.FOCUSED_QUICKEST

    //Warning: this is off in the tests, as it is a source of non-determinism
    @Cfg("Whether to use timestamp info on the execution time of the tests for sampling (e.g., to reward the quickest ones)")
    var useTimeInFeedbackSampling = true


    @Experimental
    @Cfg("When sampling from archive based on targets, decide whether to use weights based on properties of the targets (e.g., a target likely leading to a flag will be sampled less often)")
    var useWeightedSampling = false


    @Cfg("Define the population size in the search algorithms that use populations (e.g., Genetic Algorithms, but not MIO)")
    @Min(1.0)
    var populationSize = 30

    @Cfg("Define the probability of happening mutation in the genetic algorithms")
    @Probability
    var fixedRateMutation = 0.04

    @Cfg("Define the maximum number of tests in a suite in the search algorithms that evolve whole suites, e.g. WTS")
    @Min(1.0)
    var maxSearchSuiteSize = 50

    @Cfg("Probability of applying crossover operation (if any is used in the search algorithm)")
    @Probability
    var xoverProbability = 0.7

    @Cfg("Number of elements to consider in a Tournament Selection (if any is used in the search algorithm)")
    @Min(1.0)
    var tournamentSize = 10

    @Cfg("When sampling new test cases to evaluate, probability of using some smart strategy instead of plain random")
    @Probability
    var probOfSmartSampling = 0.95

    @Cfg("Max number of 'actions' (e.g., RESTful calls or SQL commands) that can be done in a single test")
    @Min(1.0)
    var maxTestSize = 10

    @Cfg("Based on some heuristics, there are cases in which 'maxTestSize' can be overridden at runtime")
    var enableOptimizedTestSize = true

    @Cfg("Tracking of SQL commands to improve test generation")
    var heuristicsForSQL = true

    @Experimental
    @Cfg("If using SQL heuristics, enable more advanced version")
    var heuristicsForSQLAdvanced = false

    @Cfg("Tracking of Mongo commands to improve test generation")
    var heuristicsForMongo = true

    @Cfg("Enable extracting SQL execution info")
    var extractSqlExecutionInfo = true

    @Cfg("Enable extracting Mongo execution info")
    var extractMongoExecutionInfo = true

    @Experimental
    @Cfg("Enable EvoMaster to generate SQL data with direct accesses to the database. Use Dynamic Symbolic Execution")
    var generateSqlDataWithDSE = false

    @Cfg("Enable EvoMaster to generate SQL data with direct accesses to the database. Use a search algorithm")
    var generateSqlDataWithSearch = true

    @Cfg("Enable EvoMaster to generate Mongo data with direct accesses to the database")
    var generateMongoData = true

    @Cfg("When generating SQL data, how many new rows (max) to generate for each specific SQL Select")
    @Min(1.0)
    var maxSqlInitActionsPerMissingData = 1


    @Cfg("Force filling data of all columns when inserting new row, instead of only minimal required set.")
    var forceSqlAllColumnInsertion = true


    @Cfg("Maximum size (in bytes) that EM handles response payloads in the HTTP responses. " +
            "If larger than that, a response will not be stored internally in EM during the test generation. " +
            "This is needed to avoid running out of memory.")
    var maxResponseByteSize = 1_000_000

    @Cfg("Whether to print how much search done so far")
    var showProgress = true

    @Debug
    @Cfg("Whether or not enable a search process monitor for archiving evaluated individuals and Archive regarding an evaluation of search. " +
            "This is only needed when running experiments with different parameter settings")
    var enableProcessMonitor = false

    @Debug
    @Cfg("Specify a format to save the process data")
    var processFormat = ProcessDataFormat.JSON_ALL

    enum class ProcessDataFormat {
        /**
         * save evaluated individuals and Archive with a json format
         */
        JSON_ALL,

        /**
         * only save the evaluated individual with the specified test format
         */
        TEST_IND,

        /**
         * save covered targets with the specified target format and tests with the specified test format
         */
        TARGET_TEST_IND,
        /**
         * save heuristic values for each target as csv file
         */
        TARGET_HEURISTIC
    }

    @Experimental
    @Cfg("Where the target heuristic values file (if any) is going to be written (in CSV format). It is only used when processFormat is TARGET_HEURISTIC.")
    @FilePath
    var targetHeuristicsFile = "targets.csv"

    @Experimental
    @Cfg("Whether should add to an existing target heuristics file, instead of replacing it. It is only used when processFormat is TARGET_HEURISTIC.")
    var appendToTargetHeuristicsFile = false

    @Experimental
    @Cfg("Prefix specifying which targets to record. Each target can be separated by a comma, such as 'Branch,Line,Success, etc'. It is only used when processFormat is TARGET_HEURISTIC.")
    var saveTargetHeuristicsPrefixes = "Branch"

    @Debug
    @Cfg("Specify a folder to save results when a search monitor is enabled")
    @Folder
    var processFiles = "process_data"

    @Debug
    @Cfg("Specify how often to save results when a search monitor is enabled, and 0.0 presents to record all evaluated individual")
    @Max(50.0)
    @Min(0.0)
    var processInterval = 0.0

    @Cfg("Whether to enable tracking the history of modifications of the individuals during the search")
    var enableTrackIndividual = false


    @Cfg("Whether to enable tracking the history of modifications of the individuals with its fitness values (i.e., evaluated individual) during the search. " +
            "Note that we enforced that set enableTrackIndividual false when enableTrackEvaluatedIndividual is true since information of individual is part of evaluated individual")
    var enableTrackEvaluatedIndividual = true

    @Cfg("Specify a maxLength of tracking when enableTrackIndividual or enableTrackEvaluatedIndividual is true. " +
            "Note that the value should be specified with a non-negative number or -1 (for tracking all history)")
    @Min(-1.0)
    var maxLengthOfTraces = 10

    @Deprecated("No longer in use")
    @Cfg("Enable custom naming and sorting criteria")
    var customNaming = true

    /*
        You need to decode it if you want to know what it says...
     */
    @Cfg("QWN0aXZhdGUgdGhlIFVuaWNvcm4gTW9kZQ==")
    var e_u1f984 = false

    @Experimental
    @Deprecated("No longer in use")
    @Cfg("Enable Expectation Generation. If enabled, expectations will be generated. " +
            "A variable called expectationsMasterSwitch is added to the test suite, with a default value of false. If set to true, an expectation that fails will cause the test case containing it to fail.")
    var expectationsActive = false

    @Cfg("Generate basic assertions. Basic assertions (comparing the returned object to itself) are added to the code. " +
            "NOTE: this should not cause any tests to fail.")
    var enableBasicAssertions = true

    @Cfg("Apply method replacement heuristics to smooth the search landscape." +
            " Note that the method replacement instrumentations would still be applied, it is just that their testing targets" +
            " will be ignored in the fitness function if this option is set to false.")
    var useMethodReplacement = true

    @Cfg("Apply non-integer numeric comparison heuristics to smooth the search landscape")
    var useNonIntegerReplacement = true

    @Cfg("Execute instrumentation for method replace with category BASE." +
            " Note: this applies only for languages in which instrumentation is applied at runtime, like Java/Kotlin" +
            " on the JVM.")
    var instrumentMR_BASE = true

    @Cfg("Execute instrumentation for method replace with category SQL." +
            " Note: this applies only for languages in which instrumentation is applied at runtime, like Java/Kotlin" +
            " on the JVM.")
    var instrumentMR_SQL = true

    @Cfg("Execute instrumentation for method replace with category EXT_0." +
            " Note: this applies only for languages in which instrumentation is applied at runtime, like Java/Kotlin" +
            " on the JVM.")
    var instrumentMR_EXT_0 = true

    @Cfg("Execute instrumentation for method replace with category MONGO." +
            " Note: this applies only for languages in which instrumentation is applied at runtime, like Java/Kotlin" +
            " on the JVM.")
    var instrumentMR_MONGO = true


    @Cfg("Execute instrumentation for method replace with category NET." +
            " Note: this applies only for languages in which instrumentation is applied at runtime, like Java/Kotlin" +
            " on the JVM.")
    @Experimental
    var instrumentMR_NET = false

    @Cfg("Execute instrumentation for method replace with category OPENSEARCH." +
            " Note: this applies only for languages in which instrumentation is applied at runtime, like Java/Kotlin" +
            " on the JVM.")
    @Experimental
    var instrumentMR_OPENSEARCH = false

    @Cfg("Enable to expand the genotype of REST individuals based on runtime information missing from Swagger")
    var expandRestIndividuals = true


    @Cfg("Add an extra query param, to analyze how it is used/read by the SUT. Needed to discover new query params" +
            " that were not specified in the schema.")
    var extraQueryParam = true


    @Cfg("Add an extra HTTP header, to analyze how it is used/read by the SUT. Needed to discover new headers" +
            " that were not specified in the schema.")
    var extraHeader = true


    @Cfg("Percentage [0.0,1.0] of elapsed time in the search while trying to infer any extra query parameter and" +
            " header. After this time has passed, those attempts stop. ")
    @PercentageAsProbability(false)
    var searchPercentageExtraHandling = 0.1

    enum class ResourceSamplingStrategy(val requiredArchive: Boolean = false) {
        NONE,

        /**
         * probability for applicable strategy is specified
         */
        Customized,

        /**
         * probability for applicable strategy is equal
         */
        EqualProbability,

        /**
         * probability for applicable strategy is derived based on actions
         */
        Actions,

        /**
         * probability for applicable strategy is adaptive with time
         */
        TimeBudgets,

        /**
         * probability for applicable strategy is adaptive with performance, i.e., Archive
         */
        Archive(true),

        /**
         * probability for applicable strategy is adaptive with performance, i.e., Archive
         */
        ConArchive(true)
    }

    @Cfg("Specify whether to enable resource-based strategy to sample an individual during search. " +
            "Note that resource-based sampling is only applicable for REST problem with MIO algorithm.")
    var resourceSampleStrategy = ResourceSamplingStrategy.ConArchive

    @Cfg("Specify whether to enable resource dependency heuristics, i.e, probOfEnablingResourceDependencyHeuristics > 0.0. " +
            "Note that the option is available to be enabled only if resource-based smart sampling is enable. " +
            "This option has an effect on sampling multiple resources and mutating a structure of an individual.")
    @Probability
    var probOfEnablingResourceDependencyHeuristics = 0.95

    @Debug
    @Cfg("Specify whether to export derived dependencies among resources")
    var exportDependencies = false

    @Debug
    @Cfg("Specify a file that saves derived dependencies")
    @FilePath
    var dependencyFile = "dependencies.csv"

    @Cfg("Specify a probability to apply SQL actions for preparing resources for REST Action")
    @Probability
    var probOfApplySQLActionToCreateResources = 0.1


    @Experimental
    @Cfg("Probability of sampling a new individual with schedule tasks. Note that schedule task is only enabled for RPCProblem")
    @Probability
    var probOfSamplingScheduleTask = 0.0

    @Experimental
    @Cfg("Specify a maximum number of handling (remove/add) resource size at once, e.g., add 3 resource at most")
    @Min(0.0)
    var maxSizeOfHandlingResource = 0

    @Experimental
    @Cfg("Specify a strategy to determinate a number of resources to be manipulated throughout the search.")
    var employResourceSizeHandlingStrategy = SqlInitResourceStrategy.NONE

    enum class SqlInitResourceStrategy {
        NONE,

        /**
         * determinate a number of resource to be manipulated at random between 1 and [maxSizeOfHandlingResource]
         */
        RANDOM,

        /**
         * adaptively decrease a number of resources to be manipulated from [maxSizeOfHandlingResource] to 1
         */
        DPC
    }

    enum class StructureMutationProbStrategy {
        /**
         * apply the specified probability
         */
        SPECIFIED,

        /**
         * deactivated structure mutator when focused search starts
         */
        SPECIFIED_FS,

        /**
         * gradually update the structure mutator probability from [structureMutationProbability] to [structureMutationProFS] before focused search
         */
        DPC_TO_SPECIFIED_BEFORE_FS,

        /**
         * gradually update the structure mutator probability from [structureMutationProbability] to [structureMutationProFS] after focused search
         */
        DPC_TO_SPECIFIED_AFTER_FS,

        /**
         * apply a probability which is adaptive to the impact
         */
        ADAPTIVE_WITH_IMPACT
    }

    @Cfg("Whether or not to enable a structure mutation for mutating individuals." +
            " This feature can only be activated for algorithms that support structural mutation, such as MIO or RW.")
    var enableStructureMutation = true

    @Experimental
    @Cfg("Specify a max size of resources in a test. 0 means the there is no specified restriction on a number of resources")
    @Min(0.0)
    var maxResourceSize = 0

    @Experimental
    @Cfg("Specify a strategy to handle a probability of applying structure mutator during the focused search")
    var structureMutationProbStrategy = StructureMutationProbStrategy.SPECIFIED

    @Experimental
    @Cfg("Specify a probability of applying structure mutator during the focused search")
    @Probability
    var structureMutationProFS = 0.0

    enum class MaxTestSizeStrategy {
        /**
         * apply the specified max size of a test
         */
        SPECIFIED,

        /**
         * gradually increasing a size of test until focused search
         */
        DPC_INCREASING,

        /**
         * gradually decreasing a size of test until focused search
         */
        DPC_DECREASING
    }

    @Experimental
    @Cfg("Specify a strategy to handle a max size of a test")
    var maxTestSizeStrategy = MaxTestSizeStrategy.SPECIFIED

    @Experimental
    @Cfg("Specify whether to decide the resource-based structure mutator and resource to be mutated adaptively based on impacts during focused search." +
            "Note that it only works when resource-based solution is enabled for solving REST problem")
    var enableAdaptiveResourceStructureMutation = false

    @Experimental
    @Cfg("Specify a probability of applying length handling")
    @Probability
    var probOfHandlingLength = 0.0

    @Experimental
    @Cfg("Specify a max size of a test to be targeted when either DPC_INCREASING or DPC_DECREASING is enabled")
    var dpcTargetTestSize = 1

    @Cfg("Specify a minimal number of rows in a table that enables selection (i.e., SELECT sql) to prepare resources for REST Action. " +
            "In other words, if the number is less than the specified, insertion is always applied.")
    @Min(0.0)
    var minRowOfTable = 10

    @Cfg("Specify a probability that enables selection (i.e., SELECT sql) of data from database instead of insertion (i.e., INSERT sql) for preparing resources for REST actions")
    @Probability(false)
    var probOfSelectFromDatabase = 0.1

    @Cfg("Whether to apply text/name analysis to derive relationships between name entities, e.g., a resource identifier with a name of table")
    var doesApplyNameMatching = true

    @Deprecated("Experiment results were not good, and library is huge in terms of MBs...")
    @Cfg("Whether to employ NLP parser to process text. " +
            "Note that to enable this parser, it is required to build the EvoMaster with the resource profile, i.e., mvn clean install -Presourceexp -DskipTests")
    var enableNLPParser = false

    @Debug
    @Cfg("Whether to save mutated gene info, which is typically used for debugging mutation")
    var saveMutationInfo = false

    @Debug
    @Cfg("Specify a path to save mutation details which is useful for debugging mutation")
    @FilePath
    var mutatedGeneFile = "mutatedGeneInfo.csv"

    @Experimental
    @Cfg("Specify a strategy to select targets for evaluating mutation")
    var mutationTargetsSelectionStrategy = MutationTargetsSelectionStrategy.FIRST_NOT_COVERED_TARGET

    enum class MutationTargetsSelectionStrategy {
        /**
         * employ not covered target obtained by archive at first for all upTimesMutations
         *
         * e.g., mutate an individual with 10times, at first, the current not covered target is {A, B}
         * after the 2nd mutation, A is covered, C is newly reached,
         * for next mutation, that target employed for the comparison is still {A, B}
         */
        FIRST_NOT_COVERED_TARGET,

        /**
         * expand targets with updated not covered targets
         *
         * e.g., mutate an individual with 10times, at first, the current not covered target is {A, B}
         * after the 2nd mutation, A is covered, C is newly reached,
         * for next mutation, that target employed for the comparison is {A, B, C}
         */
        EXPANDED_UPDATED_NOT_COVERED_TARGET,

        /**
         * only employ current not covered targets obtained by archive
         *
         * e.g., mutate an individual with 10times, at first, the current not covered target is {A, B}
         * after the 2nd mutation, A is covered, C is newly reached,
         * for next mutation, that target employed for the comparison is {B, C}
         */
        UPDATED_NOT_COVERED_TARGET
    }

    @Debug
    @Cfg("Whether to record targets when the number is more than 100")
    var recordExceededTargets = false

    @Debug
    @Cfg("Specify a path to save all not covered targets when the number is more than 100")
    @FilePath
    var exceedTargetsFile = "exceedTargets.txt"


    @Cfg("Specify a probability to apply S1iR when resource sampling strategy is 'Customized'")
    @Probability(false)
    var S1iR: Double = 0.25

    @Cfg("Specify a probability to apply S1dR when resource sampling strategy is 'Customized'")
    @Probability(false)
    var S1dR: Double = 0.25

    @Cfg("Specify a probability to apply S2dR when resource sampling strategy is 'Customized'")
    @Probability(false)
    var S2dR: Double = 0.25

    @Cfg("Specify a probability to apply SMdR when resource sampling strategy is 'Customized'")
    @Probability(false)
    var SMdR: Double = 0.25

    @Cfg("Whether to enable a weight-based mutation rate")
    var weightBasedMutationRate = true

    @Cfg("Whether to specialize sql gene selection to mutation")
    var specializeSQLGeneSelection = true

    @Cfg("Specify a starting percentage of genes of an individual to mutate")
    @PercentageAsProbability(false)
    var startingPerOfGenesToMutate = 0.5

    @Cfg("When weight-based mutation rate is enabled, specify a percentage of calculating mutation rate based on a number of candidate genes to mutate. " +
            "For instance, d = 1.0 means that the mutation rate fully depends on a number of candidate genes to mutate, " +
            "and d = 0.0 means that the mutation rate fully depends on weights of candidates genes to mutate.")
    @PercentageAsProbability(false)
    var d = 0.8

    @Cfg("Specify a probability to enable archive-based mutation")
    @Probability
    var probOfArchiveMutation = 0.5

    @Debug
    @Cfg("Specify whether to collect impact info that provides an option to enable of collecting impact info when archive-based gene selection is disable. ")
    var doCollectImpact = false

    @Experimental
    @Cfg("During mutation, whether to abstract genes for repeated SQL actions")
    var abstractInitializationGeneToMutate = false

    @Cfg("Specify a strategy to calculate a weight of a gene based on impacts")
    var geneWeightBasedOnImpactsBy = GeneWeightBasedOnImpact.RATIO

    enum class GeneWeightBasedOnImpact {
        /**
         * using rank of counter
         */
        SORT_COUNTER,

        /**
         * using rank of ratio
         */
        SORT_RATIO,

        /**
         * using counter
         */
        COUNTER,

        /**
         * using ratio, ie, counter/total manipulated times
         */
        RATIO
    }

    @Cfg("Specify a strategy to select genes for mutation adaptively")
    var adaptiveGeneSelectionMethod = GeneMutationSelectionMethod.APPROACH_IMPACT

    @Cfg("Specify whether to enable weight-based mutation selection for selecting genes to mutate for a gene")
    var enableWeightBasedMutationRateSelectionForGene = true

    @Debug
    @Cfg("Whether to save archive info after each of mutation, which is typically useful for debugging mutation and archive")
    var saveArchiveAfterMutation = false

    @Debug
    @Cfg("Specify a path to save archive after each mutation during search, only useful for debugging")
    @FilePath
    var archiveAfterMutationFile = "archive.csv"

    @Debug
    @Cfg("Whether to save impact info after each of mutation, which is typically useful debugging impact driven solutions and mutation")
    var saveImpactAfterMutation = false

    @Debug
    @Cfg("Specify a path to save collected impact info after each mutation during search, only useful for debugging")
    @FilePath
    var impactAfterMutationFile = "impactSnapshot.csv"

    @Cfg("Whether to enable archive-based gene mutation")
    var archiveGeneMutation = ArchiveGeneMutation.SPECIFIED_WITH_SPECIFIC_TARGETS

    @Cfg("Specify a maximum length of history when applying archive-based gene mutation")
    var maxlengthOfHistoryForAGM = 10

    /**
     * archive-based gene value mutation
     */
    enum class ArchiveGeneMutation(val withTargets: Int = 0, val withDirection: Boolean = false) {
        /**
         * do not apply archive-based gene mutation
         */
        NONE,

        /**
         * mutate with history but not related to any target
         */
        SPECIFIED,

        /**
         * mutate individual with history based on targets
         * but not specific to actions
         */
        SPECIFIED_WITH_TARGETS(1, false),

        /**
         * mutate individual with history based on targets
         * and the targets are linked to the action level
         */
        SPECIFIED_WITH_SPECIFIC_TARGETS(2, false),

        /**
         * mutate individual with history and directions based on targets
         * but not specific to actions
         */
        SPECIFIED_WITH_TARGETS_DIRECTION(1, true),

        /**
         * mutate individual with history and directions based on targets
         * and the targets are linked to the action level
         */
        SPECIFIED_WITH_SPECIFIC_TARGETS_DIRECTION(2, true),

        /**
         * mutate individual with history with consideration of dependency among genes
         * (not done yet)
         */
        ADAPTIVE
    }

    @Debug
    @Cfg("Specify whether to export derived impacts among genes")
    var exportImpacts = false

    @Debug
    @Cfg("Specify a path to save derived genes")
    @FilePath
    var impactFile = "impact.csv"

    @Cfg("Probability to use input tracking (i.e., a simple base form of taint-analysis) to determine how inputs are used in the SUT")
    @Probability
    var baseTaintAnalysisProbability = 0.5

    @Cfg("Whether input tracking is used on sampling time, besides mutation time")
    var taintOnSampling = true

    @Cfg("Apply taint analysis to handle special cases of Maps and Arrays")
    var taintAnalysisForMapsAndArrays = true

    @Probability
    @Experimental
    @Cfg("When sampling new individual, check whether to use already existing info on tainted values")
    var useGlobalTaintInfoProbability = 0.0


    @Experimental
    @Cfg("If there is new discovered information from a test execution, reward it in the fitness function")
    var discoveredInfoRewardedInFitness = false

    @Experimental
    @Cfg("During mutation, force the mutation of genes that have newly discovered specialization from previous fitness evaluations," +
            " based on taint analysis.")
    var taintForceSelectionOfGenesWithSpecialization = false

    @Probability
    @Cfg("Probability of removing a tainted value during mutation")
    var taintRemoveProbability = 0.5

    @Probability
    @Cfg("Probability of applying a discovered specialization for a tainted value")
    var taintApplySpecializationProbability = 0.5

    @Probability
    @Cfg("Probability of changing specialization for a resolved taint during mutation")
    var taintChangeSpecializationProbability = 0.1

    @Min(0.0)
    @Max(stringLengthHardLimit.toDouble())
    @Cfg("The maximum length allowed for evolved strings. Without this limit, strings could in theory be" +
            " billions of characters long")
    var maxLengthForStrings = 1024


    @Min(0.0)
    @Cfg("Maximum length when sampling a new random string. Such limit can be bypassed when a string is mutated.")
    var maxLengthForStringsAtSamplingTime = 16


    @Deprecated("Should not use this option any more, but rather run proper BB experiments")
    @Cfg("Only used when running experiments for black-box mode, where an EvoMaster Driver would be present, and can reset state after each experiment")
    var bbExperiments = false

    @Cfg("Specify whether to export covered targets info")
    var exportCoveredTarget = false

    @Cfg("Specify a file which saves covered targets info regarding generated test suite")
    @FilePath
    var coveredTargetFile = "coveredTargets.txt"

    @Cfg("Specify a format to organize the covered targets by the search")
    var coveredTargetSortedBy = SortCoveredTargetBy.NAME

    enum class SortCoveredTargetBy {
        /**
         * sorted by ids of targets alphabetically
         */
        NAME,

        /**
         * grouped by tests and sorted by index of tests.
         * it may help to analyze the individuals regarding different strategies.
         */
        TEST
        /**
         * there might be other options, e.g., based on class,
         * but we need to follow rules to encode and decode regarding id.
         */
    }


    //TODO Andrea/Man. will need to discuss how this can be refactored for RPC as well

    @Experimental
    @Cfg("Whether to seed EvoMaster with some initial test cases. These test cases will be used and evolved throughout the search process")
    var seedTestCases = false

    enum class SeedTestCasesFormat {
        POSTMAN
    }

    @Experimental
    @Cfg("Whether to export test cases during seeding as a separate file")
    var exportTestCasesDuringSeeding = false

    @Experimental
    @Cfg("Format of the test cases seeded to EvoMaster")
    var seedTestCasesFormat = SeedTestCasesFormat.POSTMAN

    @Experimental
    @FilePath
    @Cfg("File path where the seeded test cases are located")
    var seedTestCasesPath: String = "postman.postman_collection.json"

    @Cfg("Try to enforce the stopping of SUT business-level code." +
            " This is needed when TCP connections timeouts, to avoid thread executions" +
            " from previous HTTP calls affecting the current one")
    var killSwitch = true

    @Cfg("Number of milliseconds we are going to wait to get a response on a TCP connection, e.g., " +
            "when making HTTP calls to a Web API")
    var tcpTimeoutMs = 30_000

    @Cfg("Whether to skip failed SQL commands in the generated test files")
    var skipFailureSQLInTestFile = true

    /**
     *  TODO Better to have something like 11, based on some statistics of graphs that we analyzed,
     *  but there are issues of performance (time and memory) in analysis of large graphs, that
     *  would need to be optimized
     */
    val defaultTreeDepth = 4

    @Cfg("Maximum tree depth in mutations/queries to be evaluated." +
            " This is to avoid issues when dealing with huge graphs in GraphQL")
    @Min(1.0)
    var treeDepth = defaultTreeDepth


    @Experimental
    @Cfg("Specify a maximum number of existing data in the database to sample in a test when SQL handling is enabled. " +
            "Note that a negative number means all existing data would be sampled")
    var maxSizeOfExistingDataToSample = -1


    @Experimental
    @Cfg("Specify whether insertions should be used to calculate SQL heuristics instead of retrieving data from real databases.")
    var useInsertionForSqlHeuristics = false

    @Debug
    @Cfg("Whether to output executed sql info")
    var outputExecutedSQL = OutputExecutedSQL.NONE

    enum class OutputExecutedSQL {
        /**
         * do not output executed sql info
         */
        NONE,

        /**
         * output all executed sql info at the end
         */
        ALL_AT_END,

        /**
         * output executed info once they were executed per test
         */
        ONCE_EXECUTED
    }

    @Debug
    @Cfg("Specify a path to save all executed sql commands to a file (default is 'sql.txt')")
    var saveExecutedSQLToFile: String = "sql.txt"

    @Cfg("Whether to enable extra targets for responses, e.g., regarding nullable response, having extra targets for whether it is null")
    var enableRPCExtraResponseTargets = true

    @Cfg("Whether to enable customized responses indicating business logic")
    var enableRPCCustomizedResponseTargets = true

    @Cfg("Whether to generate RPC endpoint invocation which is independent from EM driver.")
    var enablePureRPCTestGeneration = true

    @Cfg("Whether to generate RPC Assertions based on response instance")
    var enableRPCAssertionWithInstance = true

    @Experimental
    @Cfg("Whether to enable customized RPC Test output if 'customizeRPCTestOutput' is implemented")
    var enableRPCCustomizedTestOutput = false

    @Cfg("Specify a maximum number of data in a collection to be asserted in the generated tests." +
            " Note that zero means that only the size of the collection will be asserted." +
            " A negative value means all data in the collection will be asserted (i.e., no limit).")
    var maxAssertionForDataInCollection = 3

    @Cfg("Specify whether to employ smart database clean to clear data in the database if the SUT has." +
            "`null` represents to employ the setting specified on the EM driver side")
    var employSmartDbClean: Boolean? = null


    @Cfg("Add predefined tests at the end of the search. An example is a test to fetch the schema of RESTful APIs.")
    var addPreDefinedTests: Boolean = true


    @Cfg("Apply a minimization phase to make the generated tests more readable." +
            " Achieved coverage would stay the same." +
            " Generating shorter test cases might come at the cost of having more test cases.")
    var minimize: Boolean = true


    @Cfg("Maximum number of minutes that will be dedicated to the minimization phase." +
            " A negative number mean no timeout is considered." +
            " A value of 0 means minimization will be skipped, even if minimize=true.")
    var minimizeTimeout = 5


    @Cfg("When applying minimization phase, and some targets get lost when re-computing coverage," +
            " then printout a detailed description.")
    var minimizeShowLostTargets = true

    @PercentageAsProbability
    @Cfg("Losing targets when recomputing coverage is expected (e.g., constructors of singletons)," +
            " but problematic if too much")
    var minimizeThresholdForLoss = 0.2

    @FilePath(true)
    @Regex("(.*jacoco.*\\.jar)|(^$)")
    @Cfg("Path on filesystem of where JaCoCo Agent jar file is located." +
            " Option meaningful only for External Drivers for JVM." +
            " If left empty, it is not used." +
            " Note that this only impact the generated output test cases.")
    var jaCoCoAgentLocation = ""

    @FilePath(true)
    @Regex("(.*jacoco.*\\.jar)|(^$)")
    @Cfg("Path on filesystem of where JaCoCo CLI jar file is located." +
            " Option meaningful only for External Drivers for JVM." +
            " If left empty, it is not used." +
            " Note that this only impact the generated output test cases.")
    var jaCoCoCliLocation = ""

    @FilePath(true)
    @Cfg(" Destination file for JaCoCo." +
            " Option meaningful only for External Drivers for JVM." +
            " If left empty, it is not used." +
            " Note that this only impact the generated output test cases.")
    var jaCoCoOutputFile = ""

    @Min(0.0)
    @Max(maxTcpPort)
    @Cfg("Port used by JaCoCo to export coverage reports")
    var jaCoCoPort = 8899

    @FilePath
    @Cfg("Command for 'java' used in the External Drivers." +
            " Useful for when there are different JDK installed on same machine without the need" +
            " to update JAVA_HOME." +
            " Note that this only impact the generated output test cases.")
    var javaCommand = "java"

    enum class ExternalServiceIPSelectionStrategy {
        /**
         * To disabled external service handling
         */
        NONE,

        /**
         * Default will assign 127.0.0.5
         */
        DEFAULT,

        /**
         * User provided IP address
         */
        USER,

        /**
         * Random IP address will be generated within the loopback range
         */
        RANDOM
    }

    @Experimental
    @Cfg("Specify a method to select the first external service spoof IP address.")
    var externalServiceIPSelectionStrategy = ExternalServiceIPSelectionStrategy.NONE

    @Experimental
    @Cfg("User provided external service IP." +
            " When EvoMaster mocks external services, mock server instances will run on local addresses starting from" +
            " this provided address." +
            " Min value is ${defaultExternalServiceIP}." +
            " Lower values like ${ExternalServiceSharedUtils.RESERVED_RESOLVED_LOCAL_IP} and ${ExternalServiceSharedUtils.DEFAULT_WM_LOCAL_IP} are reserved.")
    @Regex(externalServiceIPRegex)
    var externalServiceIP : String = defaultExternalServiceIP

    @Experimental
    @Cfg("Whether to apply customized method (i.e., implement 'customizeMockingRPCExternalService' for external services or 'customizeMockingDatabase' for database) to handle mock object.")
    var enableCustomizedMethodForMockObjectHandling = false


    @Experimental
    @Cfg("Whether to apply customized method (i.e., implement 'customizeScheduleTaskInvocation' for invoking schedule task) to invoke schedule task.")
    var enableCustomizedMethodForScheduleTaskHandling = false

    @Experimental
    @Cfg("Whether to save mocked responses as separated files")
    var saveMockedResponseAsSeparatedFile = false

    @Experimental
    @Cfg("Whether to save schedule task invocation as separated files")
    var saveScheduleTaskInvocationAsSeparatedFile = false

    @Experimental
    @Cfg("Specify test resource path where to save mocked responses as separated files")
    //TODO need proper constraint checking
    var testResourcePathToSaveMockedResponse = ""

    @Cfg("Whether to analyze how SQL databases are accessed to infer extra constraints from the business logic." +
            " An example is javax/jakarta annotation constraints defined on JPA entities.")
    @Probability(true)
    var useExtraSqlDbConstraintsProbability = 0.9

    @Experimental
    @Cfg("a probability of harvesting actual responses from external services as seeds.")
    @Probability(activating = true)
    var probOfHarvestingResponsesFromActualExternalServices = 0.0

    @Experimental
    @Cfg("a probability of prioritizing to employ successful harvested actual responses from external services as seeds (e.g., 2xx from HTTP external service).")
    @Probability(activating = true)
    var probOfPrioritizingSuccessfulHarvestedActualResponses = 0.0

    @Experimental
    @Cfg("a probability of mutating mocked responses based on actual responses")
    @Probability(activating = true)
    var probOfMutatingResponsesBasedOnActualResponse = 0.0

    @Experimental
    @Cfg("Number of threads for external request harvester. No more threads than numbers of processors will be used.")
    @Min(1.0)
    var externalRequestHarvesterNumberOfThreads: Int = 2


    enum class ExternalRequestResponseSelectionStrategy {
        /**
         * Selects the exact matching response for the request.
         */
        EXACT,

        /**
         * If there is no exact match, selects the closest matching response from the same domain based on the
         * request path.
         */
        CLOSEST_SAME_DOMAIN,

        /**
         * If there is no exact match, selects the closest matching response from the same path based on the
         * request path.
         */
        CLOSEST_SAME_PATH,

        /**
         * If there is no exact match, selects a random response for the request from the captured responses
         * regardless of the domain.
         */
        RANDOM
    }

    @Experimental
    @Cfg("Harvested external request response selection strategy")
    var externalRequestResponseSelectionStrategy = ExternalRequestResponseSelectionStrategy.EXACT

    @Cfg("Whether to employ constraints specified in API schema (e.g., OpenAPI) in test generation")
    var enableSchemaConstraintHandling = true

    @Cfg("a probability of enabling single insertion strategy to insert rows into database.")
    @Probability(activating = true)
    var probOfEnablingSingleInsertionForTable = 0.5

    @Debug
    @Cfg("Whether to record info of executed actions during search")
    var recordExecutedMainActionInfo = false

    @Debug
    @Cfg("Specify a path to save all executed main actions to a file (default is 'executedMainActions.txt')")
    var saveExecutedMainActionInfo = "executedMainActions.txt"


    @Cfg("Specify prefixes of targets (e.g., MethodReplacement, Success_Call, Local) which will exclude in impact collection. " +
            "Multiple exclusions should be separated with semicolon (i.e., ;).")
    @Regex(targetExclusionRegex)
    var excludeTargetsForImpactCollection = "${IdMapper.LOCAL_OBJECTIVE_KEY};${ObjectiveNaming.METHOD_REPLACEMENT}"

    var excludedTargetsForImpactCollection: List<String> = extractExcludedTargetsForImpactCollection()
        private set


    @Cfg("In REST, specify probability of using 'default' values, if any is specified in the schema")
    @Probability(true)
    var probRestDefault = 0.05

    @Cfg("In REST, specify probability of using 'example(s)' values, if any is specified in the schema")
    @Probability(true)
    var probRestExamples = 0.20

    @Cfg("In REST, enable the supports of 'links' between resources defined in the OpenAPI schema, if any." +
            " When sampling a test case, if the last call has links, given this probability new calls are" +
            " added for the link.")
    @Probability(true)
    var probUseRestLinks = 0.5

    //TODO mark as deprecated once we support proper Robustness Testing
    @Cfg("When generating data, allow in some cases to use invalid values on purpose")
    var allowInvalidData: Boolean = true

    @Cfg("Apply a security testing phase after functional test cases have been generated.")
    var security = true

    @Experimental
    @Cfg("To apply SSRF detection as part of security testing.")
    var ssrf = false

    enum class VulnerableInputClassificationStrategy {
        /**
         * Uses the manual methods to select the vulnerable inputs.
         */
        MANUAL,

        /**
         * Use LLMs to select potential vulnerable inputs.
         */
        LLM,
    }

    @Experimental
    @Cfg("Strategy to classify inputs for potential vulnerability classes related to an REST endpoint.")
    var vulnerableInputClassificationStrategy = VulnerableInputClassificationStrategy.MANUAL

    @Experimental
    @Cfg("HTTP callback verifier hostname. Default is set to 'localhost'. If the SUT is running inside a " +
            "container (i.e., Docker), 'localhost' will refer to the container. This can be used to change the hostname.")
    var callbackURLHostname = "localhost"

    @Experimental
    @Cfg("Enable language model connector")
    var languageModelConnector = false

    @Experimental
    @Cfg("Large-language model external service URL. Default is set to Ollama local instance URL.")
    var languageModelServerURL: String = "http://localhost:11434/"

    @Experimental
    @Cfg("Large-language model name as listed in Ollama")
    var languageModelName: String = "llama3.2:latest"

    @Experimental
    @Cfg("Number of threads for language model connector. No more threads than numbers of processors will be used.")
    @Min(1.0)
    var languageModelConnectorNumberOfThreads: Int = 2


    @Cfg("If there is no configuration file, create a default template at given configPath location." +
            " However this is done only on the 'default' location. If you change 'configPath', no new file will be" +
            " created.")
    var createConfigPathIfMissing: Boolean = true


    @Experimental
    @Cfg("Extra checks on HTTP properties in returned responses, used as automated oracles to detect faults.")
    var httpOracles = false

    @Cfg("Validate responses against their schema, to check for inconsistencies. Those are treated as faults.")
    var schemaOracles = true

    @Cfg("Apply more advanced coverage criteria for black-box testing. This can result in larger generated test suites.")
    var advancedBlackBoxCoverage = true

    @Cfg("In black-box testing, aim at adding calls to reset the state of the SUT after it has been modified by the test." +
            " For example, in REST APIs, DELETE operations are added (if any exist) after each successful POST/PUT." +
            " However, this is done heuristically." +
            " There is no guarantee the state will be properly cleaned-up, this is just a best effort attempt.")
    var blackBoxCleanUp = true

    fun timeLimitInSeconds(): Int {
        if (maxTimeInSeconds > 0) {
            return maxTimeInSeconds
        }

        return convertToSeconds(maxTime)
    }

    fun improvementTimeoutInSeconds() : Int {
        if(prematureStop.isNullOrBlank()){
            return Int.MAX_VALUE
        }
        return convertToSeconds(prematureStop)
    }

    private fun convertToSeconds(time: String): Int {
        val h = time.indexOf('h')
        val m = time.indexOf('m')
        val s = time.indexOf('s')

        val hours = if (h >= 0) {
            time.subSequence(0, h).toString().trim().toInt()
        } else 0

        val minutes = if (m >= 0) {
            time.subSequence(if (h >= 0) h + 1 else 0, m).toString().trim().toInt()
        } else 0

        val seconds = if (s >= 0) {
            time.subSequence(if (m >= 0) m + 1 else (if (h >= 0) h + 1 else 0), s).toString().trim().toInt()
        } else 0

        return (hours * 60 * 60) + (minutes * 60) + seconds
    }

    @Experimental
    @Cfg("How much data elements, per key, can be stored in the Data Pool." +
            " Once limit is reached, new old will replace old data. ")
    @Min(1.0)
    var maxSizeDataPool = 100

    @Experimental
    @Cfg("Threshold of Levenshtein Distance for key-matching in Data Pool")
    @Min(0.0)
    var thresholdDistanceForDataPool = 2

    @Cfg("Enable the collection of response data, to feed new individuals based on field names matching.")
    var useResponseDataPool = true

    @Experimental
    @Probability(false)
    @Cfg("Specify the probability of using the data pool when sampling test cases." +
            " This is for black-box (bb) mode")
    var bbProbabilityUseDataPool = 0.8

    @Experimental
    @Probability(false)
    @Cfg("Specify the probability of using the data pool when sampling test cases." +
            " This is for white-box (wb) mode")
    var wbProbabilityUseDataPool = 0.2

    @Cfg("Specify the naming strategy for test cases.")
    var namingStrategy = defaultTestCaseNamingStrategy

    @Cfg("Specify the hard limit for test case name length")
    var maxTestCaseNameLength = 80

    @Cfg("Specify if true boolean query parameters are included in the test case name." +
            " Used for test case naming disambiguation. Only valid for Action based naming strategy.")
    var nameWithQueryParameters = true

    @Cfg("Specify the test case sorting strategy")
    var testCaseSortingStrategy = defaultTestCaseSortingStrategy

    @Experimental
    @Cfg("Adds TestMethodOrder annotation for JUnit 5 tests")
    var useTestMethodOrder = false

    @Experimental
    @Probability(true)
    @Cfg("When sampling a new individual, probability that ALL optional choices are ON, or ALL are OFF." +
            " The choice between ON and OFF depends on probabilityOfOnVsOffInAllOptionals.")
    var probabilityAllOptionalsAreOnOrOff = 0.0

    @Experimental
    @Cfg("If all-optionals is activated with probabilityAllOptionalsAreOnOrOff, specifying probability of using ON" +
            " instead of OFF.")
    val probabilityOfOnVsOffInAllOptionals = 0.8

    @Cfg("Add summary comments on each test")
    var addTestComments = true

    @Min(1.0)
    @Cfg("Max length for test comments. Needed when enumerating some names/values, making comments too long to be" +
            " on a single line")
    var maxLengthForCommentLine = 80

    @Cfg(description = "Number of elite individuals to be preserved when forming the next population in population-based search algorithms that do not use an archive, like for example Genetic Algorithms")
    @Min(0.0)
    var elitesCount: Int = 1

    @Experimental
    @Cfg("In REST APIs, when request Content-Type is JSON, POJOs are used instead of raw JSON string. " +
            "Only available for JVM languages")
    var dtoForRequestPayload = false

    @Cfg("Override the value of externalEndpointURL in auth configurations." +
            " This is useful when the auth server is running locally on an ephemeral port, or when several instances" +
            " are run in parallel, to avoid creating/modifying auth configuration files." +
            " If what provided is a URL starting with 'http', then full replacement will occur." +
            " Otherwise, the input will be treated as a 'hostname:port', and only that info will be updated (e.g.," +
            " path element of the URL will not change).")
    var overrideAuthExternalEndpointURL : String? = null


    fun getProbabilityUseDataPool() : Double{
        return if(blackBox){
            bbProbabilityUseDataPool
        } else {
            wbProbabilityUseDataPool
        }
    }

    fun trackingEnabled() = isUsingAdvancedTechniques() && (enableTrackEvaluatedIndividual || enableTrackIndividual)

    /**
     * impact info can be collected when archive-based solution is enabled or doCollectImpact
     */
    fun isEnabledImpactCollection() = isUsingAdvancedTechniques() && doCollectImpact || isEnabledArchiveGeneSelection()

    /**
     * @return whether archive-based gene selection is enabled
     */
    fun isEnabledArchiveGeneSelection() = isUsingAdvancedTechniques() && probOfArchiveMutation > 0.0 && adaptiveGeneSelectionMethod != GeneMutationSelectionMethod.NONE

    /**
     * @return whether archive-based gene mutation is enabled based on the configuration, ie, EMConfig
     */
    fun isEnabledArchiveGeneMutation() = isUsingAdvancedTechniques() && archiveGeneMutation != ArchiveGeneMutation.NONE && probOfArchiveMutation > 0.0

    fun isEnabledArchiveSolution() = isEnabledArchiveGeneMutation() || isEnabledArchiveGeneSelection()


    /**
     * @return whether enable resource-based method
     */
    fun isEnabledResourceStrategy() = isUsingAdvancedTechniques() && resourceSampleStrategy != ResourceSamplingStrategy.NONE

    /**
     * @return whether enable resource-dependency based method
     */
    fun isEnabledResourceDependency() = isEnabledSmartSampling() && isEnabledResourceStrategy()

    /**
     * @return whether to generate SQL between rest actions
     */
    fun isEnabledSQLInBetween() = isEnabledResourceDependency() && heuristicsForSQL && probOfApplySQLActionToCreateResources > 0.0

    /**
     * Return a "," comma separated list of categories of Method Replacements that should be applied
     */
    fun methodReplacementCategories(): String {
        val categories = mutableListOf<String>()
        if (instrumentMR_BASE) categories.add(ReplacementCategory.BASE.toString())
        if (instrumentMR_SQL) categories.add(ReplacementCategory.SQL.toString())
        if (instrumentMR_EXT_0) categories.add(ReplacementCategory.EXT_0.toString())
        if (instrumentMR_NET) categories.add(ReplacementCategory.NET.toString())
        if (instrumentMR_MONGO) categories.add(ReplacementCategory.MONGO.toString())
        if (instrumentMR_OPENSEARCH) categories.add(ReplacementCategory.OPENSEARCH.toString())
        return categories.joinToString(",")
    }

    /**
     * @return whether to handle the external service mocking
     */
    fun isEnabledExternalServiceMocking(): Boolean {
        return externalServiceIPSelectionStrategy != ExternalServiceIPSelectionStrategy.NONE
    }


    private fun extractExcludedTargetsForImpactCollection(): List<String> {
        if (excludeTargetsForImpactCollection.equals("None", ignoreCase = true)) return emptyList()
        val excluded = excludeTargetsForImpactCollection.split(targetSeparator).map { it.lowercase() }.toSet()
        return IdMapper.ALL_ACCEPTED_OBJECTIVE_PREFIXES.filter { excluded.contains(it.lowercase()) }
    }

    fun isEnabledMutatingResponsesBasedOnActualResponse() = isUsingAdvancedTechniques() && (probOfMutatingResponsesBasedOnActualResponse > 0)

    fun isEnabledHarvestingActualResponse(): Boolean = isUsingAdvancedTechniques() && (probOfHarvestingResponsesFromActualExternalServices > 0 || probOfMutatingResponsesBasedOnActualResponse > 0)

    /**
     * MIO is the default search algorithm in EM for white-box testing.
     * Many techniques in EM are defined only for MIO, ie most improvements in EM are
     * done as an extension of MIO.
     * Other search algorithms might use these advanced techniques, but would require non-standard exceptions.
     *
     */
    fun isUsingAdvancedTechniques() =
        algorithm == Algorithm.MIO
                || algorithm == Algorithm.RW // Random Walk is just used to study Fitness Landscape in MIO
                || (algorithm == Algorithm.DEFAULT && !blackBox)

    fun isEnabledTaintAnalysis() = isUsingAdvancedTechniques() && baseTaintAnalysisProbability > 0

    fun isEnabledSmartSampling() = (isUsingAdvancedTechniques() || algorithm == Algorithm.SMARTS) && probOfSmartSampling > 0

    fun isEnabledWeightBasedMutation() = isUsingAdvancedTechniques() && weightBasedMutationRate

    fun isEnabledInitializationStructureMutation() = isUsingAdvancedTechniques() && initStructureMutationProbability > 0 && maxSizeOfMutatingInitAction > 0

    fun isEnabledResourceSizeHandling() = isUsingAdvancedTechniques() && probOfHandlingLength > 0 && maxSizeOfHandlingResource > 0

    fun getTagFilters() = endpointTagFilter?.split(",")?.map { it.trim() } ?: listOf()

    fun isEnabledAIModelForResponseClassification() = aiModelForResponseClassification != AIResponseClassifierModel.NONE
}
