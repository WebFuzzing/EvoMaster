package org.evomaster.core.problem.rest.resource

import org.evomaster.core.Lazy
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.rest.builder.CreateResourceUtils
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.data.RestPath
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.resource.dependency.*
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.problem.rest.util.ParserUtil
import org.evomaster.core.problem.util.RestResourceTemplateHandler
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @property path resource path
 * @property actions actions under the resource, with references of tables
 * @property initMode configurable option to init resource with additional info, e.g., related tables
 * @property employNLP specified whether to employ natural language parser
 */
open class RestResourceNode(
    val path : RestPath,
    val actions: MutableList<RestCallAction> = mutableListOf(),
    val initMode : InitMode,
    val employNLP : Boolean
) {

    companion object {
        private const val PROB_EXTRA_PATCH = 0.8
        val log: Logger = LoggerFactory.getLogger(RestResourceNode::class.java)
    }

    /**
     * key is original text of the token
     * value is PathRToken which contains more analysis info about the original text
     */
    private val tokens : MutableMap<String, PathRToken> = mutableMapOf()

    /**
     * segments of a path
     * since a token may be a combined word, the word can be separator by processing text analysis,
     * the [segments] can be a flatten list of words for the path (at index 1) or a list of original tokens (at index 0).
     */
    private val segments : MutableList<List<String>> = mutableListOf()

    init {
        when(initMode){
            InitMode.WITH_TOKEN, InitMode.WITH_DERIVED_DEPENDENCY, InitMode.WITH_DEPENDENCY ->{
                if(path.getNonParameterTokens().isNotEmpty()){
                    tokens.clear()
                    ParserUtil.parsePathTokens(this.path, tokens, employNLP && initMode != InitMode.WITH_DEPENDENCY)
                }
                initSegments()
            }
            else ->{
                //do nothing
            }
        }
    }

    /**
     * [ancestors] is ordered, first is closest ancestor, and last is deepest one.
     */
    private val ancestors : MutableList<RestResourceNode> = mutableListOf()

    /**
     * [otherPostResourceNode] contains resource nodes which have POST action, but not part of [ancestors]
     *
     * Man: current consider all other resources which have POST,
     * TODO might only consider sibling resources or need some strategies
     */
    private val otherPostResourceNode : MutableList<RestResourceNode> = mutableListOf()


    /**
     * possible solutions to prepare resources
     */
    private val creations : MutableList<CreationChain> = mutableListOf()

    /**
     * key is id of param which is [getLastTokensOfPath] + [Param.name]
     * value is detailed info [ParamInfo] including
     *          e.g., whether the param is required to be bound with existing resource (i.e., POST action or table),
     */
    val paramsInfo : MutableMap<String, ParamInfo> = mutableMapOf()


    /**
     * collect related tables
     */
    val resourceToTable : ResourceRelatedToTable = ResourceRelatedToTable(path.toString())

    /**
     * HTTP methods under the resource, including possible POST in its ancestors'
     * last means if there are post actions in its ancestors
     */
    private val verbs : Array<Boolean> = Array(RestResourceTemplateHandler.getSizeOfHandledVerb() + 1){false}

    /**
     * key is template with string format
     * value is template info
     */
    private val templates : MutableMap<String, CallsTemplate> = mutableMapOf()

    /**
     * In REST, params of the action might be modified, e.g., for WebRequest
     * In this case, we modify the [actions] with updated action with new params if there exist,
     * and backup its original form with [originalActions]
     */
    private val originalActions : MutableList<RestCallAction> = mutableListOf()

    /**
     * this init occurs after actions and ancestors are set up
     */
    fun init(){
        initVerbs()
        initCreationPoints()
        initTemplates()
        when(initMode){
            InitMode.WITH_TOKEN, InitMode.WITH_DERIVED_DEPENDENCY, InitMode.WITH_DEPENDENCY -> initParamInfo()
            else -> { }
        }
    }

    /**
     * init ancestors of [this] resource node
     */
    fun initAncestors(resources : List<RestResourceNode>){
        resources.forEach {r ->
            if(!r.path.isEquivalent(this.path) && r.path.isDirectOrPossibleAncestorOf(this.path))
                ancestors.add(r)
            else{
                if(r.getPotentialRestActionForCreation().isNotEmpty())
                    otherPostResourceNode.add(r)
            }
        }
    }

    /**
     * @return resource node based on [path]
     */
    fun getResourceNode(path: RestPath) : RestResourceNode?{
        if (path.toString() == path.toString()) return this
        return ancestors.find { it.path.toString() == path.toString() }
    }


    /**
     * @return mutable genes in [dbactions] and they do not bind with rest actions.
     */
    fun getMutableSQLGenes(dbactions: List<SqlAction>, template: String, is2POST : Boolean) : List<out Gene>{

        val related = getPossiblyBoundParams(template, is2POST, null).map {
            resourceToTable.paramToTable[it.key]
        }

        return dbactions.filterNot { it.representExistingData }.flatMap { db->
            val exclude = related.flatMap { r-> r?.getRelatedColumn(db.table.name)?.toList()?:listOf() }
            db.seeGenesForInsertion(exclude)
        }.filter{it.isMutable() && it !is SqlForeignKeyGene && it !is SqlPrimaryKeyGene}
    }

    /**
     * @return mutable genes in [actions] which perform action on current [this] resource node
     *          with [callsTemplate] template, e.g., POST-GET
     */
    private fun getMutableRestGenes(actions: List<RestCallAction>, template: String) : List<out Gene>{

        if (!RestResourceTemplateHandler.isNotSingleAction(template)) return actions.flatMap(RestCallAction::seeTopGenes).filter(Gene::isMutable)

        val missing = getPossiblyBoundParams(template, false, null)
        val params = mutableListOf<Param>()
        (actions.indices).forEach { i ->
            val a = actions[i]
            if (i != actions.size-1 && (i == 0 || a.verb == HttpVerb.POST)) {
                params.addAll(a.parameters)
            } else{
                //add the parameters which does not bind with POST if exist
                params.addAll(a.parameters.filter { p->
                    missing.none { m->
                        m.key == getParamId(a.parameters, p)
                    }
                })
            }
        }
        return params.flatMap(Param::seeGenes).filter(Gene::isMutable)
    }

    private fun initVerbs(){
        actions.forEach { a->
            RestResourceTemplateHandler.getIndexOfHttpVerb(a.verb).let {
                if(it == -1)
                    throw IllegalArgumentException("cannot handle the action with ${a.verb}")
                else
                    verbs[it] = true
            }
        }


        verbs[verbs.size - 1] = verbs[RestResourceTemplateHandler.getIndexOfHttpVerb(HttpVerb.POST)]
    }

    private fun initTemplates(){
        if (!verbs[verbs.size - 1]){
            verbs[verbs.size - 1] = hasPostCreation()
        }

        RestResourceTemplateHandler.initSampleSpaceOnlyPOST(verbs, templates)

        assert(templates.isNotEmpty())
    }

    //if only get
    fun isIndependent() : Boolean{
        return templates.all { it.value.independent } && (creations.none { c->c.isComplete() } && resourceToTable.paramToTable.isEmpty())
    }

    // if only post, the resource does not contain any independent action
    fun hasIndependentAction() : Boolean{
        return (1 until (verbs.size - 1)).find { verbs[it]} != null
    }

    /************************** creation manage*********************************/

    /**
     * @return related table for creating resource for [this] node with sql
     */
    fun getSqlCreationPoints() : List<String>{
        if (resourceToTable.confirmedSet.isNotEmpty()) return resourceToTable.confirmedSet.keys.toList()
        return resourceToTable.derivedMap.keys.toList()
    }

    /**
     * @return whether there exist POST action (either from [this] node or its [ancestors]) to create the resource
     */
    fun hasPostCreation() = creations.any { it is PostCreationChain && it.actions.isNotEmpty() } || verbs.first()

    private fun initCreationPoints(){

        val postCreation = PostCreationChain(mutableListOf())
        val posts = mutableMapOf<String, List<RestCallAction>>()

        val currentPost = actions.filter { it.isPotentialActionForCreation()}
        if (currentPost.isNotEmpty()){
            posts[currentPost.first().path.toString()] = currentPost.groupBy { it.verb }.map { (t, u) ->
                if (u.size > 1){
                    LoggingUtil.uniqueWarn(log, "more than ${t.name} Actions for resource node $path")
                }
                u.first()
            }
        }


        if (posts.isEmpty()){
            val siblingPost = getSiblingPostAction()
            if (siblingPost != null && siblingPost.isNotEmpty())
                posts[siblingPost.first().path.toString()] = siblingPost
            else{
                val ancestorPosts = chooseAllClosestAncestor(path, RestCallAction.CONFIG_POTENTIAL_VERB_FOR_CREATION)
                if (ancestorPosts != null) posts[ancestorPosts.first().path.toString()] = ancestorPosts
            }

        }

        if (posts.isNotEmpty()){
            postCreation.addActions(0, posts.values.flatten())
            for (post in posts.values.map { it.first() }){
                /*
                    in terms of eg, /v3/api/rfoo/{rfooId}
                    if the verb is PUT, it is still needed to check its ancestor for preparing resources
                 */
                if ((post.path.hasVariablePathParameters() && (post.verb == HttpVerb.PUT || !post.path.isLastElementAParameter())) || post.path.getVariableNames().size >= 2) {
                    nextCreationPoints(post.path, postCreation)
                }else
                    postCreation.confirmComplete()
            }
        }else{
            if(path.hasVariablePathParameters()) {
                postCreation.confirmIncomplete(path.toString())
            }else
                postCreation.confirmComplete()
        }

        postCreation.prioritizePostChain()

        if (postCreation.hasAnyAction())
            creations.add(postCreation)
    }

    private fun getSiblingPostAction() : List<RestCallAction>?{
        return otherPostResourceNode.find { it.path.isSiblingForPreparingResource(this.path) }?.getPotentialRestActionForCreation()
    }


    private fun nextCreationPoints(path: RestPath, postCreationChain: PostCreationChain){
        val posts = chooseAllClosestAncestor(path, RestCallAction.CONFIG_POTENTIAL_VERB_FOR_CREATION)?.map { it.copy() as RestCallAction }
        if(!posts.isNullOrEmpty()){
            val newPosts = posts.filter { !postCreationChain.hasAction(it) }
            postCreationChain.addActions(0, newPosts)
            for (post in newPosts){
                if (post.path.hasVariablePathParameters() &&
                    (!post.path.isLastElementAParameter()) ||
                    post.path.getVariableNames().size >= 2) {
                    nextCreationPoints(post.path, postCreationChain)
                }else
                    postCreationChain.confirmComplete()
            }
        }else{
            postCreationChain.confirmIncomplete(path.toString())
        }
    }

    private fun getCreation(predicate: (CreationChain) -> Boolean) : CreationChain?{
        return creations.find(predicate)
    }

    fun getPostChain() : PostCreationChain?{
        return getCreation { creationChain : CreationChain -> (creationChain is PostCreationChain) }?.run {
            this as PostCreationChain
        }
    }

    /***********************************************************/

    /**
     * generated another resource calls which differs from [calls]
     */
    fun generateAnother(calls : RestResourceCalls, randomness: Randomness, maxTestSize: Int) : RestResourceCalls?{
        val current = calls.template?.template?: RestResourceTemplateHandler.getStringTemplateByActions(calls.seeActions(
            ActionFilter.NO_SQL).filterIsInstance<RestCallAction>())
        val rest = templates.filter { it.value.template != current}
        if(rest.isEmpty()) return null
        val selected = randomness.choose(rest.keys)
        return createRestResourceCallBasedOnTemplate(selected,randomness, maxTestSize)

    }

    /**
     * @return a number of dependent templates in [this] resource node
     */
    fun numOfDepTemplate() : Int{
        return templates.values.count { !it.independent }
    }

    /**
     * @return a number of templates in [this] resource node
     */
    fun numOfTemplates() : Int{
        return templates.size
    }

    /**
     * @return a rest resource call at random
     */
    fun randomRestResourceCalls(randomness: Randomness, maxTestSize: Int): RestResourceCalls{
        val randomTemplates = templates.filter { e->
            e.value.size in 1..maxTestSize
        }.map { it.key }
        if(randomTemplates.isEmpty()) return sampleOneAction(null, randomness)
        return createRestResourceCallBasedOnTemplate(randomness.choose(randomTemplates), randomness, maxTestSize)
    }

    /**
     * sample an independent rest resource call, i.e., with an independent template
     */
    fun sampleIndResourceCall(randomness: Randomness, maxTestSize: Int) : RestResourceCalls{
        selectTemplate({ call : CallsTemplate -> call.independent || (call.template == HttpVerb.POST.toString() && call.size > 1)}, randomness)?.let {
            return createRestResourceCallBasedOnTemplate(it.template, randomness, maxTestSize)
        }
        return createRestResourceCallBasedOnTemplate(HttpVerb.POST.toString(), randomness,maxTestSize)
    }


    /**
     * sample a rest resource with one action based on the specified [verb]
     * if [verb] is null, select an action at random from available [actions] in this node
     */
    fun sampleOneAction(verb : HttpVerb? = null, randomness: Randomness) : RestResourceCalls{
        val al = if(verb != null) getActionByHttpVerb(verb) else randomness.choose(actions)
        return sampleOneAction(al!!.copy() as RestCallAction, randomness)
    }

    /**
     * sample a rest resource call with given [action].
     * The return action is initialized
     */
    fun sampleOneAction(action : RestCallAction, randomness: Randomness) : RestResourceCalls{
        val copy = action.copy() as RestCallAction
        if(copy.isInitialized()){
            copy.randomize(randomness,false)
        } else {
            copy.doInitialize(randomness)
        }

        val template = templates[copy.verb.toString()]
                ?: throw IllegalArgumentException("${copy.verb} is not one of templates of ${this.path}")

        val call =  RestResourceCalls(template, this, mutableListOf(EnterpriseActionGroup(copy)))

        if(action.verb == HttpVerb.POST){
            getCreation { c : CreationChain -> (c is PostCreationChain) }.let {
                if(it != null && (it as PostCreationChain).actions.size == 1 && it.isComplete()){
                    call.status = ResourceStatus.CREATED_REST
                }else{
                    call.status = ResourceStatus.NOT_FOUND_DEPENDENT
                }
            }
        }else
            call.status = ResourceStatus.NOT_NEEDED

        return call
    }

    /**
     * sample a rest resource call
     * @param randomness
     * @param maxTestSize specified the max size of rest actions in this call
     * @param prioriDependent specified whether it is perferred to sample an independent call
     * @param prioriIndependent specified whether it is perferred to sample a dependent call
     */
    fun sampleAnyRestResourceCalls(randomness: Randomness, maxTestSize: Int, prioriIndependent : Boolean = false, prioriDependent : Boolean = false) : RestResourceCalls{
        if (maxTestSize < 1 && prioriDependent == prioriIndependent && prioriDependent){
            throw IllegalArgumentException("unaccepted args")
        }
        val fchosen = templates.filter { it.value.size <= maxTestSize }
        if(fchosen.isEmpty())
            return sampleOneAction(null,randomness)
        val chosen =
            if (prioriDependent)  fchosen.filter { !it.value.independent }
            else if (prioriIndependent) fchosen.filter { it.value.independent }
            else fchosen
        if (chosen.isEmpty())
            return createRestResourceCallBasedOnTemplate(randomness.choose(fchosen).template,randomness, maxTestSize)
        return createRestResourceCallBasedOnTemplate(randomness.choose(chosen).template,randomness, maxTestSize)
    }

    /**
     * sample a resource call with the specified [template]
     */
    fun sampleRestResourceCalls(template: String, randomness: Randomness, maxTestSize: Int) : RestResourceCalls{
        assert(maxTestSize > 0)
        return createRestResourceCallBasedOnTemplate(template,randomness, maxTestSize)
    }

    /**
     * @return creation chain with POST
     */
    fun genPostChain(randomness: Randomness, maxTestSize: Int) : RestResourceCalls?{
        val template = templates["POST"]?:
            return null

        return createRestResourceCallBasedOnTemplate(template.template, randomness, maxTestSize)
    }


    private fun handleHeadLocation(actions: List<RestCallAction>) : RestCallAction {

        if (actions.size == 1) return actions.first()

        (1 until actions.size).forEach { i->
            CreateResourceUtils.linkDynamicCreateResource(actions[i-1], actions[i])
        }

        return actions.last()
    }


    /**
     * create rest resource call based on the specified [template]
     */
    fun createRestResourceCallBasedOnTemplate(template: String, randomness: Randomness, maxTestSize: Int): RestResourceCalls{
        if(!templates.containsKey(template))
            throw IllegalArgumentException("$template does not exist in $path")
        val ats = RestResourceTemplateHandler.parseTemplate(template)
        // POST-*, *
        val results = mutableListOf<RestCallAction>()
        var status = ResourceStatus.NOT_NEEDED
        val first = ats.first()
        var lastPost: RestCallAction? = null
        if (first == HttpVerb.POST){
            val post = getPostChain()
            if (post == null)
                status = ResourceStatus.NOT_FOUND
            else{
                results.addAll(post.createPostChain(randomness))
                // handle header location
                lastPost = handleHeadLocation(results)
                if (!post.isComplete())
                    status = ResourceStatus.NOT_FOUND_DEPENDENT
                else{
                    status = ResourceStatus.CREATED_REST
                }
            }
        }else{
            results.add(createActionByVerb(first, randomness))
        }

        if (ats.size == 2){
            val action = createActionByVerb(ats[1], randomness)
            if (lastPost != null)
                CreateResourceUtils.linkDynamicCreateResource(lastPost, action)
            results.add(action)
        }else if (ats.size > 2){
            throw IllegalStateException("the size of action with $template should be less than 2, but it is ${ats.size}")
        }

        //append extra patch
        if (ats.last() == HttpVerb.PATCH && results.size +1 <= maxTestSize && randomness.nextBoolean(PROB_EXTRA_PATCH)){
            val second =  results.last().copy() as RestCallAction
            if (lastPost != null)
                CreateResourceUtils.linkDynamicCreateResource(lastPost, second)
            results.add(second)
        }

        if (results.size > maxTestSize){
            log.info("the size (${results.size}) of actions exceeds the max size ($maxTestSize) in resource node $path")
            val removeFirst = results.size - maxTestSize
            results.drop(removeFirst)
            status = ResourceStatus.NOT_ENOUGH_LENGTH
        }

        //TODO unsure about this one
        results.forEach { if(!it.isInitialized()) it.doInitialize(randomness) }

        return RestResourceCalls(
            templates[template]!!,
            this,
            results.map { EnterpriseActionGroup(it) }.toMutableList(),
            withBinding= true,
            randomness = randomness
        ).apply { this.status = status }
    }


    private fun createActionByVerb(verb : HttpVerb, randomness: Randomness) : RestCallAction {
        val action = (getActionByHttpVerb(verb)
                ?:throw IllegalStateException("cannot get $verb action in the resource $path"))
                .copy() as RestCallAction

        if(action.isInitialized())
            action.randomize(randomness, false)
        else
            action.doInitialize(randomness)
        return action
    }


    private fun templateSelected(callsTemplate: CallsTemplate){
        templates.getValue(callsTemplate.template).times += 1
    }
    
    private fun selectTemplate(predicate: (CallsTemplate) -> Boolean, randomness: Randomness, chosen : Map<String, CallsTemplate>?=null, chooseLessVisit : Boolean = false) : CallsTemplate?{
        val ts = if(chosen == null) templates.filter { predicate(it.value) } else chosen.filter { predicate(it.value) }
        if(ts.isEmpty())
            return null
        val template =  if(chooseLessVisit) ts.asSequence().sortedBy { it.value.times }.first().value
                    else randomness.choose(ts.values)
        templateSelected(template)
        return template
    }


    private fun getActionByHttpVerb(verb : HttpVerb) : RestCallAction? {
        return actions.find { a -> a.verb == verb }
    }

    private fun getPotentialRestActionForCreation() : List<RestCallAction> = actions.filter { it.isPotentialActionForCreation() }

    private fun chooseOneLongestPath(actions: List<RestCallAction>, randomness: Randomness? = null): RestCallAction {

        if (actions.isEmpty()) {
            throw IllegalArgumentException("Cannot choose from an empty collection")
        }

        val candidates = ParamUtil.selectLongestPathAction(actions)

        if(randomness == null){
            return candidates.first()
        }else
            return randomness.choose(candidates).copy() as RestCallAction
    }

    private fun chooseLongestPath(actions: List<RestCallAction>): List<RestCallAction> {

        if (actions.isEmpty()) {
            throw IllegalArgumentException("Cannot choose from an empty collection")
        }

        return ParamUtil.selectLongestPathAction(actions)
    }


    private fun chooseOneClosestAncestor(path: RestPath, verbs: List<HttpVerb>): RestCallAction? {
        val ar = if(path.toString() == this.path.toString()){
            this
        }else{
            ancestors.find { it.path.toString() == path.toString() }
        }
        ar?.let{
            val others = hasWithVerbs(it.ancestors.flatMap { it.actions }, verbs)
            if(others.isEmpty()) return null
            return chooseOneLongestPath(others)
        }
        return null
    }

    private fun chooseAllClosestAncestor(path: RestPath, verbs: List<HttpVerb>): List<RestCallAction>? {
        val ar = if(path.toString() == this.path.toString()){
            this
        }else{
            ancestors.find { it.path.toString() == path.toString() }?:otherPostResourceNode.find { it.path.toString() == path.toString() }
        }
        ar?.let{
            val others = hasWithVerbs(it.ancestors.flatMap { it.actions }, verbs)
            if(others.isEmpty()) return null
            return chooseLongestPath(others)
        }
        return null
    }

    private fun hasWithVerbs(actions: List<RestCallAction>, verbs: List<HttpVerb>): List<RestCallAction> {
        return actions.filter { a ->
            verbs.contains(a.verb)
        }
    }

    /********************** utility *************************/

    /**
     *  during the search, params of the Rest Action might be updated,
     *  this method is to update [actions] in this node based on the updated [action]
     */
    open fun updateActionsWithAdditionalParams(action: RestCallAction){
        val org = actions.find {  it.verb == action.verb }
        org?:throw IllegalStateException("cannot find the action (${action.getName()}) in the node $path")
        if (action.parameters.size > org.parameters.size){
            originalActions.add(org)
            actions.remove(org)
            val example = action.copy() as RestCallAction
            example.resetLocalId()
            example.resetProperties()
            actions.add(example)
        }
    }

    /**
     * @return whether the [text] is part of static tokens in the path of [this] resource node
     */
    fun isPartOfStaticTokens(text : String) : Boolean{
        return tokens.any { token ->
            token.equals(text)
        }
    }

    /**
     * @return derived tables
     */
    fun getDerivedTables() : Set<String> = resourceToTable.derivedMap.flatMap { it.value.map { m->m.targetMatched } }.toHashSet()

    /**
     * @return is any POST, GET, PATCH, DELETE, PUT action?
     */
    fun isAnyAction() : Boolean{
        verbs.forEach {
            if (it) return true
        }
        return false
    }

    /**
     * @return name of the resource node
     */
    fun getName() : String = path.toString()

    /**
     * @return tokens map
     */
    fun getTokenMap() : Map<String, PathRToken> = tokens.toMap()

    /**
     * @return flatten tokens
     */
    fun getFlatViewOfTokens(excludeStar : Boolean = true) : List<PathRToken>
            =  tokens.values.filter { !excludeStar || !it.isStar()}.flatMap { p -> if(p.subTokens.isNotEmpty()) p.subTokens else mutableListOf(p) }.toList()


    /******************** manage param *************************/

    /**
     * @return param id of [param] with given [params]
     */
    fun getParamId(params: List<Param>, param : Param) : String = "${param::class.java.simpleName}:${getParamName(params, param)}"

    private fun getParamName(params: List<Param>, param : Param) : String = ParamUtil.appendParam(getSegment(false, params, param), param.name)

    /*
    e.g., /A/{a}/B/c/{b} return B@c
     */
    private fun getLastSegment() : String = if(tokens.isNotEmpty()) tokens.values.last().segment else ""

    private fun getLastSegment(flatten : Boolean) : String {
        if(tokens.isEmpty()) return ""
        return getSegment(flatten, tokens.values.last())
    }

    private fun getSegment(flatten : Boolean, level: Int) : String{
        if(tokens.isEmpty()) return ""
        val target = tokens.values.find { it.level == level }?:tokens.values.last()
        return getSegment(flatten, target)
    }


    private fun getSegment(flatten : Boolean, target: PathRToken) : String{
        if (!flatten) return target.segment
        val nearLevel = target.nearestParamLevel
        val array = tokens.values.filter { it.level > nearLevel && (if(target.isParameter) it.level < target.level else it.level <= target.level)}
                .flatMap { if(it.subTokens.isNotEmpty()) it.subTokens.map { s->s.getKey() } else mutableListOf(it.getKey()) }.toTypedArray()
        return ParamUtil.generateParamId(array)
    }

    private fun getParamLevel(params: List<Param>, param: Param) : Int{
        if (param !is PathParam) return tokens.size
        tokens.values.filter { it.isParameter && it.originalText.equals(param.name, ignoreCase = true) }.let {
            if(it.isEmpty()){
                //log.warn(("cannot find the path param ${param.name} in the path of the resource ${getName()}"))
                if(params.any { p-> param.name.equals(p.name, ignoreCase = true) }) return tokens.size
            }
            if(it.size == 1)
                return it.first().level
            val index = params.filter { p->p.name == param.name }.indexOf(param)
            return it[index].level
        }
    }

    private fun getSegment(flatten: Boolean, params: List<Param>, param: Param) : String{
        val level = getParamLevel(params, param)
        return getSegment(flatten, level)
    }


    /**
     * @return all segments of the path
     * @param flatten specified whether to return flatten segments or not
     */
    fun getAllSegments(flatten: Boolean) : List<String>{
        assert(segments.size == 2)
        return if(flatten) segments[1] else segments[0]
    }

    private fun initSegments(){
        val levels = mutableSetOf<Int>()
        tokens.values.filter { it.isParameter }.forEach { levels.add(it.level) }
        if (!path.isLastElementAParameter()) levels.add(tokens.size)
        segments.add(0, levels.toList().sorted().map { getSegment(false, it) })
        segments.add(1, levels.toList().sorted().map { getSegment(true, it) })
        assert(segments.size == 2)
    }

    /**
     * @return reference types in [this] resource node
     */
    fun getRefTypes() : Set<String>{
        return paramsInfo.filter {  it.value.referParam is BodyParam && it.value.referParam.gene is ObjectGene && (it.value.referParam.gene as ObjectGene).refType != null}.map {
            ((it.value.referParam as BodyParam).gene as ObjectGene).refType!!
        }.toSet()
    }


    /**
     * @return is any parameter different with the given [action]?
     * Note that here the difference does not mean the value, and it means e.g., whether there exist a new parameter
     */
    fun anyParameterChanged(action : RestCallAction) : Boolean{
        val target = actions.find { it.getName() == action.getName() }
                ?: throw IllegalArgumentException("cannot find the action ${action.getName()} in the resource ${getName()}")
        return action.parameters.size != target.parameters.size
    }

    /**
     * @return whether there exists any additional parameters by comparing with [action]?
     */
    fun updateAdditionalParams(action: RestCallAction) : Map<String, ParamInfo>?{
        (actions.find { it.getName() == action.getName() }
                ?: throw IllegalArgumentException("cannot find the action ${action.getName()} in the resource ${getName()}")) as RestCallAction

        val additionParams = action.parameters.filter { p-> paramsInfo[getParamId(action.parameters, p)] == null}
        if(additionParams.isEmpty()) return null
        return additionParams.map { p-> Pair(getParamId(action.parameters, p), initParamInfo(action.verb, action.parameters, p)) }.toMap()
    }

    /**
     * update param info of [param] based on [action] and [param]
     */
    fun updateAdditionalParam(action: RestCallAction, param: Param) : ParamInfo{
        return initParamInfo(action.verb, action.parameters, param).also { it.fromAdditionInfo = true }
    }

    private fun initParamInfo(){
        paramsInfo.clear()

        /*
         parameter that is required to bind with post action, or row of tables
         1) path parameter in the middle of the path, i.e., /A/{a}/B/{b}, {a} is required to bind
         2) GET, DELETE, PATCH, PUT(-prob), if the parameter refers to "id", it is required to bind, in most case, the parameter is either PathParam or QueryParam
         3) other parameter, it is not necessary to bind, but it helps if it is bound.
                e.g., Request to get a list of data whose value is less than "parameter", if bind with an existing data, the requests make more sentence than a random data
         */
        if (tokens.isEmpty()) return
        actions.forEach { a ->
            a.parameters.forEach{p->
                initParamInfo(a.verb, a.parameters, p)
            }
        }
    }

    private fun initParamInfo(verb: HttpVerb, params: List<Param>, param: Param) : ParamInfo{

        val key = getParamId(params,param)

        val segment = getSegment(flatten = true, params = params,param = param)
        val level = getAllSegments(true).indexOf(segment)
        val doesReferToOther = when(param){
            /*
                if has POST, 50% probability of ignore the last path param, otherwise all path param
             */
            is PathParam->{
               if (!verbs[RestResourceTemplateHandler.getIndexOfHttpVerb(HttpVerb.POST)] || getParamLevel(params, param) < tokens.size - 1) 1.0 else 0.5
            }else->{
                0.0
            }
        }

        val paramInfo = paramsInfo.getOrPut(key){
            ParamInfo(param.name, key, segment, level, param, doesReferToOther)
        }

        paramInfo.involvedAction.add(verb)
        return paramInfo
    }

    /**
     * @return params in a [RestResourceCalls] that are not bounded with POST actions if there exist based on the template [actionTemplate]
     *
     */
    open fun getPossiblyBoundParams(actionTemplate: String, withSql : Boolean, randomness: Randomness? = null) : List<ParamInfo>{
        val actions = RestResourceTemplateHandler.parseTemplate(actionTemplate)
        Lazy.assert {
            actions.isNotEmpty()
        }

        when(actions[0]){
            HttpVerb.POST->{
                if (withSql) return paramsInfo.values.toList()
                return paramsInfo.values.filter { it.requiredReferToOthers() || randomness?.nextBoolean(it.probOfReferringToOther) == true }
            }
            HttpVerb.PATCH, HttpVerb.PUT->{
                return paramsInfo.values.filter { it.involvedAction.contains(actions[0]) && (it.referParam is PathParam || it.name.toLowerCase().contains("id"))}
            }
            HttpVerb.GET, HttpVerb.DELETE->{
                return paramsInfo.values.filter { it.involvedAction.contains(actions[0]) }
            }
            else ->{
                return listOf()
            }
        }
    }

    /**
     * @return template based on the [key]
     */
    fun getTemplate(key: String) : CallsTemplate{
        if (templates.containsKey(key)) return templates.getValue(key)
        throw IllegalArgumentException("cannot find $key template in the node $path")
    }

    /**
     * @return all templates
     */
    fun getTemplates() : Map<String, CallsTemplate> = templates.toMap()

    /**
     * collect feedbacks of prepared resources based on the execution
     */
    fun confirmFailureCreationByPost(calls: RestResourceCalls, action: RestCallAction, result: ActionResult){
        if (result !is RestCallResult) return

        val fail = action.verb.run { this == HttpVerb.POST || this == HttpVerb.PUT} &&
                calls.status == ResourceStatus.CREATED_REST && result.getStatusCode().run { this !in 200..299}

        if (fail && creations.isNotEmpty()){
            creations.filter { it is PostCreationChain && calls.seeActions(ActionFilter.NO_SQL).map { a->a.getName() }.containsAll(it.actions.map { a-> a.getName() }) }.apply {
                if (size == 1)
                    (first() as PostCreationChain).confirmFailure()
            }
        }
    }

    override fun toString(): String {
        return getName()
    }
}


enum class InitMode{
    NONE,
    WITH_TOKEN,
    /**
     * [WITH_DERIVED_DEPENDENCY] subsume [WITH_TOKEN]
     */
    WITH_DERIVED_DEPENDENCY,
    WITH_DEPENDENCY
}

/**
 * extract info for a parm
 *
 * @property name a name of param
 * @property key is generated based on [getParamId]
 * @property preSegment refers to the segment of the param in the path
 * @property segmentLevel refers to the level of param
 * @property referParam refers to the instance of Param in the cluster
 * @property probOfReferringToOther indicates a probability that the param is required to refer to a resource,
 *              e.g., GET /foo/{id}, with GET, {id} refers to a resource which cannot be created by the current action
 * @property involvedAction indicates the actions which exists such param,
 *              e.g., GET, PATCH might have the same param named id
 * @property fromAdditionInfo indicates whether the param is added later,
 *              e.g., during the search
 */
data class ParamInfo(
    val name : String,
    val key : String,
    val preSegment : String, //by default is flatten segment
    val segmentLevel : Int,
    val referParam : Param,
    val probOfReferringToOther: Double,
    val involvedAction : MutableSet<HttpVerb> = mutableSetOf(),
    var fromAdditionInfo : Boolean = false
){

    /**
     * @return whether the param is required to refer to other params
     */
    fun requiredReferToOthers() = probOfReferringToOther == 1.0

    /**
     * @return whether the param is possibly needed to refer to other params
     */
    fun possiblyReferToOthers() = probOfReferringToOther > 0.0
}