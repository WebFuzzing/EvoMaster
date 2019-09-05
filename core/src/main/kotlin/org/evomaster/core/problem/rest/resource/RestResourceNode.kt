package org.evomaster.core.problem.rest.resource

import org.evomaster.core.database.DbAction
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestAction
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestPath
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.resource.dependency.*
import org.evomaster.core.problem.rest.util.ParamUtil
import org.evomaster.core.problem.rest.util.ParserUtil
import org.evomaster.core.problem.rest.util.RestResourceTemplateHandler
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @property path resource path
 * @property actions actions under the resource, with references of tables
 * @property initMode configurable option to init resource with additional info, e.g., related tables
 */
class RestResourceNode(
        val path : RestPath,
        val actions: MutableList<RestAction> = mutableListOf(),
        val initMode : InitMode
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
                    ParserUtil.parsePathTokens(this.path, tokens, initMode != InitMode.WITH_DEPENDENCY)
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
     * this init occurs after actions and ancestors are set up
     */
    fun init(){
        initVerbs()
        initCreationPoints()
        when(initMode){
            InitMode.WITH_TOKEN, InitMode.WITH_DERIVED_DEPENDENCY, InitMode.WITH_DEPENDENCY -> initParamInfo()
            else -> { }
        }
    }

    fun initAncestors(resources : List<RestResourceNode>){
        resources.forEach {r ->
            if(!r.path.isEquivalent(this.path) && r.path.isAncestorOf(this.path))
                ancestors.add(r)
        }
    }

    private fun initVerbs(){
        actions.forEach { a->
            if(a is RestCallAction){
                RestResourceTemplateHandler.getIndexOfHttpVerb(a.verb).let {
                    if(it == -1)
                        throw IllegalArgumentException("cannot handle the action with ${a.verb}")
                    else
                        verbs[it] = true
                }
            }
        }
        verbs[verbs.size - 1] = verbs[RestResourceTemplateHandler.getIndexOfHttpVerb(HttpVerb.POST)]
        if (!verbs[verbs.size - 1]){
            if(ancestors.isNotEmpty())
                verbs[verbs.size - 1] = ancestors.any { a -> a.actions.any { ia-> ia is RestCallAction && ia.verb == HttpVerb.POST } }
        }

        RestResourceTemplateHandler.initSampleSpaceOnlyPOST(verbs, templates)

        assert(templates.isNotEmpty())

    }

    //if only get
    fun isIndependent() : Boolean{
        return templates.all { it.value.independent } && (creations.none { c->c.isComplete() } || resourceToTable.paramToTable.isEmpty())
        //resourceToTable.paramToTable.isEmpty() && verbs[RestResourceTemplateHandler.getIndexOfHttpVerb(HttpVerb.GET)] && verbs.filter {it}.size == 1
    }

    // if only post, the resource does not contain any independent action
    fun hasIndependentAction() : Boolean{
        return (1 until (verbs.size - 1)).find { verbs[it]} != null
    }

    /************************** creation manage*********************************/

    private fun initCreationPoints(){

        val postCreation = PostCreationChain(mutableListOf())
        val posts = actions.filter { it is RestCallAction && it.verb == HttpVerb.POST}
        val post : RestCallAction? = if(posts.isEmpty()){
            chooseClosestAncestor(path, listOf(HttpVerb.POST))
        }else if(posts.size == 1){
            posts[0] as RestCallAction
        }else null

        if(post != null){
            postCreation.actions.add(0, post)
            if ((post).path.hasVariablePathParameters() &&
                    (!post.path.isLastElementAParameter()) ||
                    post.path.getVariableNames().size >= 2) {
                nextCreationPoints(post.path, postCreation)
            }else
                postCreation.confirmComplete()
        }else{
            if(path.hasVariablePathParameters()) {
                postCreation.confirmIncomplete(path.toString())
            }else
                postCreation.confirmComplete()
        }

        creations.add(postCreation)
    }

    private fun nextCreationPoints(path:RestPath, postCreationChain: PostCreationChain){
        val post = chooseClosestAncestor(path, listOf(HttpVerb.POST))
        if(post != null){
            postCreationChain.actions.add(0, post)
            if (post.path.hasVariablePathParameters() &&
                    (!post.path.isLastElementAParameter()) ||
                    post.path.getVariableNames().size >= 2) {
                nextCreationPoints(post.path, postCreationChain)
            }else
                postCreationChain.confirmComplete()
        }else{
            postCreationChain.confirmIncomplete(path.toString())
        }
    }

    private fun checkDifferenceOrInit(dbactions : MutableList<DbAction> = mutableListOf(), postactions: MutableList<RestCallAction> = mutableListOf()) : Pair<Boolean, CreationChain>{
        when{
            dbactions.isNotEmpty() && postactions.isNotEmpty() ->{
                creations.find { it is CompositeCreationChain }?.let {
                    return Pair(
                            (it as CompositeCreationChain).actions.map { a-> if(a is DbAction) a.table.name else if (a is RestCallAction) a.getName() else ""}.toHashSet()
                                    == dbactions.map { a-> a.table.name}.plus(postactions.map { p->p.getName() }).toHashSet(),
                            it
                    )
                }
                val composite = CompositeCreationChain(dbactions.plus(postactions).toMutableList()).also {
                    creations.add(it)
                }
                return Pair(true, composite)
            }
            dbactions.isNotEmpty() && postactions.isEmpty() ->{
                creations.find { it is DBCreationChain }?.let {
                    return Pair(
                            (it as DBCreationChain).actions.map { a-> a.table.name }.toHashSet() == dbactions.map { a-> a.table.name}.toHashSet(),
                            it
                    )
                }
                val db = DBCreationChain(dbactions).also {
                    creations.add(it)
                }
                return Pair(true, db)
            }
            dbactions.isEmpty() && postactions.isNotEmpty() ->{
                creations.find { it is PostCreationChain }?.let {
                    return Pair(
                            (it as PostCreationChain).actions.map { a-> a.getName() }.toHashSet() == postactions.map { a-> a.getName()}.toHashSet(),
                            it
                    )
                }
                val post = PostCreationChain(postactions).also {
                    creations.add(it)
                }
                return Pair(true, post)
            }
            else->{
                throw IllegalArgumentException("cannot manipulate creations with the inputs")
            }
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


    private fun updateTemplateSize(){
        getCreation { creationChain : CreationChain-> creationChain is PostCreationChain  }?.let {c->
            val dif = (c as PostCreationChain).actions.size - (if(verbs[RestResourceTemplateHandler.getIndexOfHttpVerb(HttpVerb.POST)]) 1 else 0)
            templates.values.filter { it.template.contains("POST") }.forEach { u ->
                if(!u.sizeAssured){
                    u.size += dif
                    u.sizeAssured = true
                }
            }
        }
    }

    fun generateAnother(calls : RestResourceCalls, randomness: Randomness, maxTestSize: Int) : RestResourceCalls?{
        val current = calls.template?.template?:RestResourceTemplateHandler.getStringTemplateByActions(calls.actions.filterIsInstance<RestCallAction>())
        val rest = templates.filter { it.value.template != current}
        if(rest.isEmpty()) return null
        val selected = randomness.choose(rest.keys)
        return genCalls(selected,randomness, maxTestSize)

    }

    fun numOfDepTemplate() : Int{
        return templates.values.count { !it.independent }
    }

    fun numOfTemplates() : Int{
        return templates.size
    }

    private fun randomizeActionGenes(action: Action, randomness: Randomness) {
        action.seeGenes().forEach { it.randomize(randomness, false) }
        if(action is RestCallAction)
            repairRandomGenes(action.parameters)
    }

    private fun repairRandomGenes(params : List<Param>){
        if(ParamUtil.existBodyParam(params)){
            params.filter { p -> p is BodyParam }.forEach { bp->
                ParamUtil.bindParam(bp, path, path, params.filter { p -> !(p is BodyParam )}, true)
            }
        }
        params.forEach { p->
            params.find { sp -> sp != p && p.name == sp.name && p::class.java.simpleName == sp::class.java.simpleName }?.apply {
                ParamUtil.bindParam(this, path, path, mutableListOf(p))
            }
        }
    }

    fun randomRestResourceCalls(randomness: Randomness, maxTestSize: Int): RestResourceCalls{
        val randomTemplates = templates.filter { e->
            e.value.size in 1..maxTestSize
        }.map { it.key }
        if(randomTemplates.isEmpty()) return sampleOneAction(null, randomness)
        return genCalls(randomness.choose(randomTemplates), randomness, maxTestSize)
    }

    fun sampleIndResourceCall(randomness: Randomness, maxTestSize: Int) : RestResourceCalls{
        selectTemplate({ call : CallsTemplate -> call.independent || (call.template == HttpVerb.POST.toString() && call.size > 1)}, randomness)?.let {
            return genCalls(it.template, randomness, maxTestSize, false, false)
        }
        return genCalls(HttpVerb.POST.toString(), randomness,maxTestSize)
    }


    fun sampleOneAction(verb : HttpVerb? = null, randomness: Randomness) : RestResourceCalls{
        val al = if(verb != null) getActionByHttpVerb(actions, verb) else randomness.choose(actions).copy() as RestAction
        return sampleOneAction(al!!, randomness)
    }

    fun sampleOneAction(action : RestAction, randomness: Randomness) : RestResourceCalls{
        val copy = action.copy()
        randomizeActionGenes(copy as RestCallAction, randomness)

        val template = templates[copy.verb.toString()]
                ?: throw IllegalArgumentException("${copy.verb} is not one of templates of ${this.path}")
        val call =  RestResourceCalls(template, RestResourceInstance(this, copy.parameters), mutableListOf(copy))

        if(action is RestCallAction && action.verb == HttpVerb.POST){
            getCreation { c : CreationChain -> (c is PostCreationChain) }.let {
                if(it != null && (it as PostCreationChain).actions.size == 1 && it.isComplete()){
                    call.status = ResourceStatus.CREATED
                }else{
                    call.status = ResourceStatus.NOT_FOUND_DEPENDENT
                }
            }
        }else
            call.status = ResourceStatus.NOT_EXISTING

        return call
    }

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
            return genCalls(randomness.choose(fchosen).template,randomness, maxTestSize)
        return genCalls(randomness.choose(chosen).template,randomness, maxTestSize)
    }


    fun sampleRestResourceCalls(template: String, randomness: Randomness, maxTestSize: Int) : RestResourceCalls{
        assert(maxTestSize > 0)
        return genCalls(template,randomness, maxTestSize)
    }

    fun genPostChain(randomness: Randomness, maxTestSize: Int) : RestResourceCalls?{
        val template = templates["POST"]?:
            return null

        return genCalls(template.template, randomness, maxTestSize)
    }

    //TODO update postCreation accordingly
    fun genCalls(
            template : String,
            randomness: Randomness,
            maxTestSize : Int = 1,
            checkSize : Boolean = true,
            createResource : Boolean = true,
            additionalPatch : Boolean = true) : RestResourceCalls{
        if(!templates.containsKey(template))
            throw IllegalArgumentException("$template does not exist in $path")
        val ats = RestResourceTemplateHandler.parseTemplate(template)
        val result : MutableList<RestAction> = mutableListOf()
        var resource : RestResourceInstance? = null

        val skipBind : MutableList<RestAction> = mutableListOf()

        var isCreated = 1
        var creation : CreationChain? = null
        if(createResource && ats[0] == HttpVerb.POST){
            val nonPostIndex = ats.indexOfFirst { it != HttpVerb.POST }
            val ac = getActionByHttpVerb(actions, if(nonPostIndex==-1) HttpVerb.POST else ats[nonPostIndex])!!.copy() as RestCallAction
            randomizeActionGenes(ac, randomness)
            result.add(ac)
            isCreated = createResourcesFor(ac, result, maxTestSize , randomness, checkSize && (!templates.getValue(template).sizeAssured))

            if(!templates.getValue(template).sizeAssured){
                getPostChain()?:throw IllegalStateException("fail to init post creation")
                val pair = checkDifferenceOrInit(postactions = (if(ac.verb == HttpVerb.POST) result else result.subList(0, result.size - 1)).map { (it as RestCallAction).copy() as RestCallAction}.toMutableList())
                if (!pair.first) {
                    log.warn("the post action are not matched with initialized post creation.")
                }
                else {
                    creation = pair.second
                    updateTemplateSize()
                }

            }

            val lastPost = result.last()
            resource = RestResourceInstance(this, (lastPost as RestCallAction).parameters)
            skipBind.addAll(result)
            if(nonPostIndex == -1){
                (1 until ats.size).forEach{
                    result.add(lastPost.copy().also {
                        skipBind.add(it as RestAction)
                    } as RestAction)
                }
            }else{
                if(nonPostIndex != ats.size -1){
                    (nonPostIndex + 1 until ats.size).forEach {
                        val action = getActionByHttpVerb(actions, ats[it])!!.copy() as RestCallAction
                        randomizeActionGenes(action, randomness)
                        result.add(action)
                    }
                }
            }

        }else{
            ats.forEach {at->
                val ac = (getActionByHttpVerb(actions, at)?:throw IllegalArgumentException("cannot find $at verb in ${actions.map {a->a.getName() }.joinToString(",")}")).copy() as RestCallAction
                randomizeActionGenes(ac, randomness)
                result.add(ac)
            }

            if(resource == null)
                resource = RestResourceInstance(this, chooseLongestPath(result, randomness).also {
                    skipBind.add(it)
                }.parameters)

            if(checkSize){
                templates.getValue(template).sizeAssured = (result.size  == templates.getValue(template).size)
            }
        }

        if(result.size > 1)
            result.filterNot { ac -> skipBind.contains(ac) }.forEach { ac ->
                if((ac as RestCallAction).parameters.isNotEmpty()){
                    ac.bindToSamePathResolution(ac.path, resource.params)
                }
            }

        assert(result.isNotEmpty())

        if(additionalPatch && randomness.nextBoolean(PROB_EXTRA_PATCH) &&!templates.getValue(template).independent && template.contains(HttpVerb.PATCH.toString()) && result.size + 1 <= maxTestSize){
            val index = result.indexOfFirst { (it is RestCallAction) && it.verb == HttpVerb.PATCH }
            val copy = result.get(index).copy() as RestAction
            result.add(index, copy)
        }
        val calls = RestResourceCalls(templates[template]!!, resource, result)

        when(isCreated){
            1 ->{
                calls.status = ResourceStatus.NOT_EXISTING
            }
            0 ->{
                calls.status = ResourceStatus.CREATED
            }
            -1 -> {
                calls.status = ResourceStatus.NOT_ENOUGH_LENGTH
            }
            -2 -> {
                calls.status = ResourceStatus.NOT_FOUND
            }
            -3 -> {
                calls.status = ResourceStatus.NOT_FOUND_DEPENDENT
            }
        }

        return calls
    }

    private fun templateSelected(callsTemplate: CallsTemplate){
        templates.getValue(callsTemplate.template).times += 1
    }
    
    private fun selectTemplate(predicate: (CallsTemplate) -> Boolean, randomness: Randomness, chosen : Map<String, CallsTemplate>?=null, chooseLessVisit : Boolean = false) : CallsTemplate?{
        val ts = if(chosen == null) templates.filter { predicate(it.value) } else chosen.filter { predicate(it.value) }
        if(ts.isEmpty())
            return null
        var template =  if(chooseLessVisit) ts.asSequence().sortedBy { it.value.times }.first().value
                    else randomness.choose(ts.values)
        templateSelected(template)
        return template
    }


    private fun getActionByHttpVerb(actions : List<RestAction>, verb : HttpVerb) : RestAction? {
        return actions.find { a -> a is RestCallAction && a.verb == verb }
    }

    private fun chooseLongestPath(actions: List<RestAction>, randomness: Randomness? = null): RestCallAction {

        if (actions.isEmpty()) {
            throw IllegalArgumentException("Cannot choose from an empty collection")
        }

        val max = actions.filter { it is RestCallAction }.asSequence().map { a -> (a as RestCallAction).path.levels() }.max()!!
        val candidates = actions.filter { a -> a is RestCallAction && a.path.levels() == max }

        if(randomness == null){
            return candidates.first() as RestCallAction
        }else
            return randomness.choose(candidates).copy() as RestCallAction
    }

    private fun chooseClosestAncestor(target: RestCallAction, verbs: List<HttpVerb>, randomness: Randomness): RestCallAction? {
        var others = sameOrAncestorEndpoints(target)
        others = hasWithVerbs(others, verbs).filter { t -> t.getName() != target.getName() }
        if(others.isEmpty()) return null
        return chooseLongestPath(others, randomness)
    }

    private fun chooseClosestAncestor(path: RestPath, verbs: List<HttpVerb>): RestCallAction? {
        val ar = if(path.toString() == this.path.toString()){
            this
        }else{
            ancestors.find { it.path.toString() == path.toString() }
        }
        ar?.let{
            val others = hasWithVerbs(it.ancestors.flatMap { it.actions }.filterIsInstance<RestCallAction>(), verbs)
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

    private fun sameOrAncestorEndpoints(target: RestCallAction): List<RestCallAction> {
        if(target.path.toString() == this.path.toString()) return ancestors.flatMap { a -> a.actions }.plus(actions).filterIsInstance<RestCallAction>()
        else {
            ancestors.find { it.path.toString() == target.path.toString() }?.let {
                return it.ancestors.flatMap { a -> a.actions }.plus(it.actions).filterIsInstance<RestCallAction>()
            }
        }
        return mutableListOf()
    }


    private fun createActionFor(template: RestCallAction, target: RestCallAction, randomness: Randomness): RestCallAction {
        val restAction = template.copy() as RestCallAction
        randomizeActionGenes(restAction, randomness)
        restAction.auth = target.auth
        restAction.bindToSamePathResolution(restAction.path, target.parameters)
        return restAction
    }



    private fun independentPost() : RestAction? {
        if(!verbs.last()) return null
        val post = getActionByHttpVerb(actions, HttpVerb.POST) as RestCallAction
        if(post.path.hasVariablePathParameters() &&
                (!post.path.isLastElementAParameter()) ||
                post.path.getVariableNames().size >= 2){
            return post
        }
        return null
    }

    private fun createResourcesFor(target: RestCallAction, test: MutableList<RestAction>, maxTestSize: Int, randomness: Randomness, forCheckSize : Boolean)
            : Int {

        if (!forCheckSize && test.size >= maxTestSize) {
            return -1
        }

        var template = chooseClosestAncestor(target, listOf(HttpVerb.POST), randomness)?:
                    return (if(target.verb == HttpVerb.POST) 0 else -2)

        val post = createActionFor(template, target, randomness)

        test.add(0, post)

        /*
            Check if POST depends itself on the postCreation of
            some intermediate resource
         */
        if (post.path.hasVariablePathParameters() &&
                (!post.path.isLastElementAParameter()) ||
                post.path.getVariableNames().size >= 2) {
            val dependencyCreated = createResourcesFor(post, test, maxTestSize, randomness, forCheckSize)
            if (0 != dependencyCreated) {
                return -3
            }
        }

        /*
            Once the POST is fully initialized, need to fix
            links with target
         */
        if (!post.path.isEquivalent(target.path)) {
            /*
                eg
                POST /x
                GET  /x/{id}
             */
            post.saveLocation = true
            target.locationId = post.path.lastElement()
        } else {
            /*
                eg
                POST /x
                POST /x/{id}/y
                GET  /x/{id}/y
             */
            //not going to save the position of last POST, as same as target
            post.saveLocation = false

            // the target (eg GET) needs to use the location of first POST, or more correctly
            // the same location used for the last POST (in case there is a deeper chain)
            target.locationId = post.locationId
        }

        return 0
    }

    /********************** utility *************************/
    fun isPartOfStaticTokens(text : String) : Boolean{
        return tokens.any { token ->
            token.equals(text)
        }
    }

    fun getDerivedTables() : Set<String> = resourceToTable.derivedMap.flatMap { it.value.map { m->m.targetMatched } }.toHashSet()

    fun isAnyAction() : Boolean{
        verbs.forEach {
            if (it) return true
        }
        return false
    }

    fun getName() : String = path.toString()

    fun getTokenMap() : Map<String, PathRToken> = tokens.toMap()

    fun getFlatViewOfTokens(excludeStar : Boolean = true) : List<PathRToken>
            =  tokens.values.filter { !excludeStar || !it.isStar()}.flatMap { p -> if(p.subTokens.isNotEmpty()) p.subTokens else mutableListOf(p) }.toList()


    /******************** manage param *************************/

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

    fun getRefTypes() : Set<String>{
        return paramsInfo.filter {  it.value.referParam is BodyParam && it.value.referParam.gene is ObjectGene && (it.value.referParam.gene as ObjectGene).refType != null}.map {
            ((it.value.referParam as BodyParam).gene as ObjectGene).refType!!
        }.toSet()
    }


    fun anyParameterChanged(action : RestCallAction) : Boolean{
        val target = actions.find { it.getName() == action.getName() }
                ?: throw IllegalArgumentException("cannot find the action ${action.getName()} in the resource ${getName()}")
        return action.parameters.size != (target as RestCallAction).parameters.size
    }

    fun updateAdditionalParams(action: RestCallAction) : Map<String, ParamInfo>?{
        (actions.find { it is RestCallAction && it.getName() == action.getName() }
                ?: throw IllegalArgumentException("cannot find the action ${action.getName()} in the resource ${getName()}")) as RestCallAction

        val additionParams = action.parameters.filter { p-> paramsInfo[getParamId(action.parameters, p)] == null}
        if(additionParams.isEmpty()) return null
        return additionParams.map { p-> Pair(getParamId(action.parameters, p), initParamInfo(action.verb, action.parameters, p)) }.toMap()
    }

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
            if(a is RestCallAction){
                a.parameters.forEach{p->
                    initParamInfo(a.verb, a.parameters, p)
                }
            }
        }
    }

    private fun initParamInfo(verb: HttpVerb, params: List<Param>, param: Param) : ParamInfo{

        val key = getParamId(params,param)

        val segment = getSegment(flatten = true, params = params,param = param)
        val level = getAllSegments(true).indexOf(segment)
        val missing = when(param){
            /*
            if has POST, ignore the last path param, otherwise all path param
             */
            is PathParam->{
                !verbs[RestResourceTemplateHandler.getIndexOfHttpVerb(HttpVerb.POST)] || getParamLevel(params, param) < tokens.size - 1
            }else->{
                false
            }
        }

        val paramInfo = paramsInfo.getOrPut(key){
            ParamInfo(param.name, key, segment, level, param, missing)
        }

        paramInfo.involvedAction.add(verb)
        return paramInfo
    }

    fun getMissingParams(actionTemplate: String) : List<ParamInfo>{
        val actions = RestResourceTemplateHandler.parseTemplate(actionTemplate)
        assert(actions.isNotEmpty())

        when(actions[0]){
            HttpVerb.POST->{
                return paramsInfo.values.filter { it.missing }
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

    fun getTemplates() : Map<String, CallsTemplate> = templates.toMap()

    fun confirmFailureCreationByPost(calls: RestResourceCalls){
        if (creations.isNotEmpty()){
            creations.filter { it is PostCreationChain && calls.actions.map { a->a.getName() }.containsAll(it.actions.map { a-> a.getName() }) }.apply {
                if (size == 1)
                    (first() as PostCreationChain).confirmFailure()
            }
        }
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
 */
class ParamInfo(
        val name : String,
        val key : String,
        val preSegment : String, //by default is flatten segment
        val segmentLevel : Int,
        val referParam : Param,
        val missing : Boolean,
        val involvedAction : MutableSet<HttpVerb> = mutableSetOf(),
        var fromAdditionInfo : Boolean = false
)