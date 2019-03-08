package org.evomaster.core.problem.rest.serviceII.resources

import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.auth.AuthenticationInfo
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.serviceII.ParamHandler
import org.evomaster.core.problem.rest.serviceII.ActionsTemplateHandler
import org.evomaster.core.problem.rest.serviceII.RestIndividualII
import org.evomaster.core.problem.rest2.resources.CallsTemplate
import org.evomaster.core.problem.rest2.resources.dependency.CreationChain
import org.evomaster.core.problem.rest2.resources.dependency.ParamRelatedToTable
import org.evomaster.core.problem.rest2.resources.token.parser.ParserUtil
import org.evomaster.core.problem.rest2.resources.token.parser.PathRToken
import org.evomaster.core.search.Action
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.max

/**
 * @property path resource path
 * @property actions actions under the resource, with references of tables
 */
class RestAResource {

    val path : RestPath
    val actions: MutableList<RestAction>
    val isNoParamGet : Boolean

    companion object {
        var CONFIG_MAX_TEST_SIZE = -1
        val PROB_EXTRA_PATCH = 0.8
        private val log: Logger = LoggerFactory.getLogger(RestAResource::class.java)
    }

    constructor(path: RestPath, actions: MutableList<RestAction>){
        this.path = path
        this.actions = actions
        initTokens()
        val action = getActionByHttpVerb(actions, HttpVerb.GET)
        isNoParamGet = (action != null) && (action is RestCallAction) && (action.parameters.size == 0)
    }


    /**
     * [ancestors] is ordered, first is closest ancestor, and last is deepest one.
     */
    private val ancestors : MutableList<RestAResource> = mutableListOf()

    var creation : CreationChain = CreationChain(mutableListOf(), false)
    val paramsToTables : MutableMap<String, ParamRelatedToTable> = mutableMapOf()
    val tokens : MutableMap<String, PathRToken> = mutableMapOf()

    /**
     * HTTP methods under the resource, including possible POST in its ancestors'
     */
    var verbs : Array<Boolean> = Array(8){false}
    /**
     * possible templates
     */
    val templates : MutableMap<String, CallsTemplate> = mutableMapOf()

    val countTempVisiting = false

    /**
     *  By default, a RestAbstractResource is independent, and it also has independent actions
     */
    var independent = true
    var hasIndependentAction = true

    fun init(){
        initVerbs()
        initCreationPoints()
        updateTemplateSize()
    }

    private fun initCreationPoints(){

        val posts = actions.filter { it is RestCallAction && it.verb == HttpVerb.POST}
        val post = if(posts.isEmpty()){
            chooseClosestAncestor(path, listOf(HttpVerb.POST))
        }else if(posts.size == 1){
            posts[0]
        }else null

        if(post != null){
            this.creation.actions.add(0, post)
            if ((post as RestCallAction).path.hasVariablePathParameters() &&
                    (!post.path.isLastElementAParameter()) ||
                    post.path.getVariableNames().size >= 2) {
                nextCreationPoints(post.path, this.creation.actions)
            }else
                this.creation.confirmComplete()
        }else{
            if(path.hasVariablePathParameters()) {
                this.creation.confirmIncomplete(path.toString())
            }else
                this.creation.confirmComplete()
        }

    }

    fun initTokens(){
        if(path.getStaticTokens().isNotEmpty()){
            tokens.clear()
            ParserUtil.parsePathTokens(this.path.toString(), tokens)
        }
    }

    private fun nextCreationPoints(path:RestPath, points: MutableList<Action>){
        val post = chooseClosestAncestor(path, listOf(HttpVerb.POST))
        if(post != null){
            points.add(0, post)
            if (post.path.hasVariablePathParameters() &&
                    (!post.path.isLastElementAParameter()) ||
                    post.path.getVariableNames().size >= 2) {
                nextCreationPoints(post.path, points)
            }else
                this.creation.confirmComplete()
        }else{
            this.creation.confirmIncomplete(path.toString())
        }
    }

    fun initAncestors(resources : List<RestAResource>){
        resources.forEach {r ->
            if(!r.path.isEquivalent(this.path) && r.path.isAncestorOf(this.path))
                ancestors.add(r)
        }
    }
    private fun initVerbs(){
        actions.forEach { a->
            when((a as RestCallAction).verb){
                HttpVerb.POST -> verbs[ActionsTemplateHandler.arrayHttpVerbs.indexOf(HttpVerb.POST)] = true
                HttpVerb.GET -> verbs[ActionsTemplateHandler.arrayHttpVerbs.indexOf(HttpVerb.GET)] = true
                HttpVerb.PUT -> verbs[ActionsTemplateHandler.arrayHttpVerbs.indexOf(HttpVerb.PUT)] = true
                HttpVerb.PATCH -> verbs[ActionsTemplateHandler.arrayHttpVerbs.indexOf(HttpVerb.PATCH)] = true
                HttpVerb.DELETE ->verbs[ActionsTemplateHandler.arrayHttpVerbs.indexOf(HttpVerb.DELETE)] = true
                HttpVerb.OPTIONS ->verbs[ActionsTemplateHandler.arrayHttpVerbs.indexOf(HttpVerb.OPTIONS)] = true
                HttpVerb.HEAD -> verbs[ActionsTemplateHandler.arrayHttpVerbs.indexOf(HttpVerb.HEAD)] = true
            }
        }
        verbs[verbs.size - 1] = verbs[0]
        if (!verbs[0]){
            verbs[0] = if(ancestors.isEmpty()) false
            else ancestors.filter{ p -> p.actions.filter { a -> (a as RestCallAction).verb == HttpVerb.POST }.isNotEmpty() }.isNotEmpty()
        }

        ActionsTemplateHandler.initSampleSpaceOnlyPOST(verbs, templates)

        templates.values.find { !it.independent }?.let {
            independent = false
            hasIndependentAction = (templates.size != it.size)
        }

        assert(templates.isNotEmpty())

    }

    private fun updateTemplateSize(){
        if(creation.actions.size > 1){
            templates.values.filter { it.template.contains("POST") }.forEach {
                it.size = it.size + creation.actions.size - 1
            }
        }
    }
    fun generateAnother(calls : RestResourceCalls, randomness: Randomness, maxTestSize: Int) : RestResourceCalls?{
        val current = calls.actions.map { (it as RestCallAction).verb }.joinToString(ActionsTemplateHandler.SeparatorTemplate)
        val rest = templates.filterNot { it.key == current }
        if(rest.isEmpty()) return null
        val selected = randomness.choose(rest.keys)
        return genCalls(selected,randomness, maxTestSize)

    }

    fun initialIndividuals(str : Int, auth : AuthenticationInfo, adHocInitialIndividuals : MutableList<RestIndividualII>, randomness: Randomness, maxTestSize: Int)  {
        when(str){
            1 -> {
                getTemplates().forEach { t ->
                    val res = genCalls(t,randomness, maxTestSize, true)
                    res.actions.forEach {
                        (it as RestCallAction).auth = auth
                    }
                    adHocInitialIndividuals.add(RestIndividualII(mutableListOf(res), SampleType.SMART_RESOURCE))
                }


            }

            2-> actions.forEach {a->
                val res = sampleOneAction(a, randomness, maxTestSize)
                res.actions.forEach {
                    (it as RestCallAction).auth = auth
                }
                adHocInitialIndividuals.add(RestIndividualII( mutableListOf(res), SampleType.SMART_RESOURCE))
            }

            3-> {
                //add dependent actions
                independentPost()?.let { apost->
                    val res = genCalls(HttpVerb.POST.toString(),randomness, maxTestSize, false, false)
                    res.actions.forEach {
                        (it as RestCallAction).auth = auth
                    }
                    adHocInitialIndividuals.add(RestIndividualII( mutableListOf(res), SampleType.SMART_RESOURCE))
                }
            }
        }
    }

    fun numOfDepTemplate() : Int{
        return templates.values.count { !it.independent }
    }

    fun numOfTemplates() : Int{
        return templates.size
    }

    fun doesExecuteEveryAction(time : Int) : Boolean{
        templates.filter { e->!e.key.contains(ActionsTemplateHandler.SeparatorTemplate) }.values.forEach {
            if( it.times < time) return false
        }
        return true
    }

    fun doesExecuteEveryTemplate(time : Int) : Boolean{
        templates.values.forEach {
            if( it.times < time) return false
        }
        return true
    }

    fun getTemplates() : MutableList<String> {
        return templates.keys.toMutableList()
    }
    private fun randomizeActionGenes(action: Action, randomness: Randomness) {
        action.seeGenes().forEach { it.randomize(randomness, false) }
        if(action is RestCallAction)
            repairRandomGenes(action.parameters)
    }

    private fun repairRandomGenes(params : List<Param>){
        if(ParamHandler.numOfBodyParam(params) > 0){
            params.filter { p -> p is BodyParam }.forEach { bp->
                ParamHandler.bindParam(bp, path, path, params.filter { p -> !(p is BodyParam )}, true)
            }
//            val bp = params.find { p -> p is BodyParam }!! as BodyParam
//            ParamHandler.bindParam(bp, path, path, params.filter { p -> !(p is BodyParam )})
        }
        params.forEach { p->
            params.find { sp -> sp != p && p.name == sp.name && p::class.java.simpleName == sp::class.java.simpleName }?.apply {
                ParamHandler.bindParam(this, path, path, mutableListOf(p))
            }

        }
    }


    fun randomRestResourceCalls(randomness: Randomness, maxTestSize: Int): RestResourceCalls{
        val randomTemplates = templates.filter { e->
            e.value.size in 1..maxTestSize
        }.map { it.key }
        if(randomTemplates.isEmpty()) return sampleOneAction(null, randomness, maxTestSize)
        return genCalls(randomness.choose(randomTemplates), randomness, maxTestSize)
    }

    fun sampleIndResourceCall(randomness: Randomness, maxTestSize: Int) : RestResourceCalls{
        selectTemplate({ call : CallsTemplate -> call.independent || (call.template == HttpVerb.POST.toString() && call.size > 1)}, randomness)?.let {
            return genCalls(it.template, randomness, maxTestSize, false, false)
        }
        //return genCalls(HttpVerb.POST.toString(), randomness)
        //return sampleOneAction( null, randomness, 1)
        return genCalls(HttpVerb.POST.toString(), randomness,maxTestSize)
    }


    fun createCallByVerb(verb : HttpVerb, resources: RestResource) : RestResourceCalls?{
        val exist = verbs[ActionsTemplateHandler.arrayHttpVerbs.indexOf(verb)]
        if(exist){
            val template = templates[verb.toString()]?: return null
            var ac = getActionByHttpVerbUntil(verb)!!.copy()
            (ac as RestCallAction).bindToSamePathResolution(ac.path, resources.params)
            return RestResourceCalls(template, resources.copy(), mutableListOf(ac))
        }
        return null
    }
    fun sampleOneAction(verb : HttpVerb? = null, randomness: Randomness, maxTestSize: Int) : RestResourceCalls{
        val al = if(verb != null) getActionByHttpVerb(actions, verb) else randomness.choose(actions).copy() as RestAction
        return sampleOneAction(al!!, randomness, maxTestSize)
    }

    fun sampleOneAction(action : RestAction, randomness: Randomness, maxTestSize: Int) : RestResourceCalls{
        assert(maxTestSize > 0)
        return sampleOneAction(action, randomness)
    }

    fun sampleOneAction(action : RestAction, randomness: Randomness) : RestResourceCalls{
        val copy = action.copy()
        randomizeActionGenes(copy as RestCallAction, randomness)
        val template = templates[copy.verb.toString()]
                ?: throw IllegalArgumentException("${copy.verb} is not one of templates")
        return RestResourceCalls(template, RestResource(this, copy.parameters), mutableListOf(copy))
    }

    /**
     * sample a sequence of actions according to a non-independent template selected at random
     * if there does not exist non-independent template whose size < required size, use independent template
     *
     * to manipulate the resource, creation is prior to selection from db.
     */
    fun sampleRestResourceCalls(randomness: Randomness, maxTestSize: Int) : RestResourceCalls{
        assert(maxTestSize > 0)
        val chosen = templates.filter { !it.value.independent && it.value.size <= maxTestSize }
        if(chosen.isEmpty())
            return sampleIndResourceCall(randomness, maxTestSize)

            val chosenWithPost = selectTemplate({call : CallsTemplate -> !call.independent && call.size <= maxTestSize}, randomness, chosen)
            chosenWithPost?.let {
                return genCalls(it.template,randomness, maxTestSize)
            }
            return sampleIndResourceCall(randomness, maxTestSize)

    }

    fun sampleAnyRestResourceCalls(randomness: Randomness, maxTestSize: Int) : RestResourceCalls{
        assert(maxTestSize > 0)
        val chosen = templates.filter { it.value.size <= maxTestSize }
        if(chosen.isEmpty())
            return sampleOneAction(null,randomness, maxTestSize)
        return genCalls(randomness.choose(chosen).template,randomness, maxTestSize)
    }


    fun sampleRestResourceCalls(template: String, randomness: Randomness, maxTestSize: Int) : RestResourceCalls{
        assert(maxTestSize > 0)
        return genCalls(template,randomness, maxTestSize)
    }

    fun genPostChain(randomness: Randomness) : RestResourceCalls?{
        val template = templates["POST"]?:
            return null

        if(creation.actions.isEmpty())
            throw IllegalArgumentException("creation is not initialized!")

        val actions = creation.actions.filter { it is RestCallAction }.map { it.copy() }.toMutableList()
        actions.forEach { randomizeActionGenes(it, randomness) }

        val resource = RestResource(this, (actions.last() as RestCallAction).parameters)
        (1 until actions.size - 1).forEach {
            val ac = actions[it]
            if((ac as RestCallAction).parameters.isNotEmpty()){
                ac.bindToSamePathResolution(ac.path, resource!!.params)
            }
        }
        return RestResourceCalls(template, resource!!, actions as MutableList<RestAction>)
    }

    private fun genCalls(

            template : String,
            randomness: Randomness,
            maxTestSize : Int = 1,
            createResource : Boolean = true,
            additionalPatch : Boolean = true

    ) : RestResourceCalls{

        val callTemplate = templates[template]?:
        throw java.lang.IllegalArgumentException("$template does not exist in $path")

        val result : MutableList<RestAction> = mutableListOf()
        var resource : RestResource? = generateResource(template)


        val ats = ActionsTemplateHandler.parseTemplate(template)

        ats.forEachIndexed { index, httpVerb ->
            if(createResource && index == 0 && httpVerb == HttpVerb.POST){
                result.addAll(creation.actions.filter { it is RestCallAction }.map { it.copy() as RestAction })
            }else{
                result.add(getActionByHttpVerb(actions,httpVerb)!!.copy() as RestAction)
            }
        }
        result.forEach { randomizeActionGenes(it, randomness) }

        val action = chooseLongestPath(result, randomness)
        resource = RestResource(this, action.parameters)

        result.filter { it != action }.forEach { ac ->
            if((ac as RestCallAction).parameters.isNotEmpty()){
                ac.bindToSamePathResolution(ac.path, resource!!.params)
            }
        }

        if(additionalPatch
                && !templates.getValue(template).independent
                && template.contains(HttpVerb.PATCH.toString())
                && result.size + 1 <= maxTestSize
                && randomness.nextBoolean(PROB_EXTRA_PATCH)
        ){
            val index = result.indexOfFirst { (it is RestCallAction) && it.verb == HttpVerb.PATCH }
            val copy = result.get(index).copy() as RestAction
            result.add(index, copy)
        }
        return RestResourceCalls(callTemplate, resource!!, result)

    }


    private fun generateResource(template: String) : RestResource?{
        //TODO add some strategy to select existing resources
        return null
    }

    private fun templateSelected(callsTemplate: CallsTemplate){
        if(countTempVisiting)
            templates.getValue(callsTemplate.template).times += 1
    }


    private fun selectTemplate(predicate: (CallsTemplate) -> Boolean, randomness: Randomness, chosen : Map<String, CallsTemplate>?=null) : CallsTemplate?{
        val ts = if(chosen == null) templates.filter { predicate(it.value) } else chosen.filter { predicate(it.value) }
        if(ts.isEmpty())
            return null
        var template =  if(countTempVisiting) ts.asSequence().sortedBy { it.value.times }.first().value
                    else randomness.choose(ts.values)
        templateSelected(template)
        return template
    }


    private fun getActionByHttpVerb(actions : List<RestAction>, verb : HttpVerb) : RestAction? {
        return actions.find { a -> a is RestCallAction && a.verb == verb }
    }

    private fun getActionByHttpVerbUntil(verb : HttpVerb) : RestAction? {
        getActionByHttpVerb(actions, verb)?.let {
            return it
        }
        ancestors.forEach {
            getActionByHttpVerb(it.actions, verb)?.let {aa->
                return aa
            }
        }
        return null
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
            val others = hasWithVerbs(it.ancestors.flatMap { it.actions }.filter { it is RestCallAction } as List<RestCallAction>, verbs)
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
        if(target.path.toString() == this.path.toString()) return ancestors.flatMap { a -> a.actions }.plus(actions) as List<RestCallAction>
        else {
            ancestors.find { it.path.toString() == target.path.toString() }?.let {
                return it.ancestors.flatMap { a -> a.actions }.plus(it.actions) as List<RestCallAction>
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

    private fun createResourcesFor(target: RestCallAction, test: MutableList<RestAction>, maxTestSize: Int, randomness: Randomness, checkSize : Boolean)
            : Int {

        if(checkSize){
            if(test.size >= max(maxTestSize, CONFIG_MAX_TEST_SIZE))
                return -1
        }else{
            if (test.size >= maxTestSize) {
                return -1
            }
        }

        var template = chooseClosestAncestor(target, listOf(HttpVerb.POST), randomness)?:
                    return (if(target.verb == HttpVerb.POST) 0 else -2)

        val post = createActionFor(template, target, randomness)

        test.add(0, post)

        /*
            Check if POST depends itself on the creation of
            some intermediate resource
         */
        if (post.path.hasVariablePathParameters() &&
                (!post.path.isLastElementAParameter()) ||
                post.path.getVariableNames().size >= 2) {
            val dependencyCreated = createResourcesFor(post, test, maxTestSize, randomness, checkSize)
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

    private fun preventPathParamMutation(action: RestCallAction) {
        action.parameters.forEach { p -> if (p is PathParam) p.preventMutation() }
    }

    private fun actionSorted(action : RestAction, delfirtst : Boolean = false) : Int{
        if(action is RestCallAction){
            when (action.verb){
                HttpVerb.POST -> return 0
                HttpVerb.GET -> return 1
                HttpVerb.PUT -> return 2
                HttpVerb.PATCH -> return 3
                HttpVerb.DELETE -> return if(delfirtst) -1 else 4
            }
        }
        return -2
    }

    fun isAnyAction() : Boolean{
        return !(actions.size == 1 && isNoParamGet)
    }

//    private fun genCalls(
//
//            template : String,
//            randomness: Randomness,
//            maxTestSize : Int = 1,
//            checkSize : Boolean = true,
//            createResource : Boolean = true,
//            additionalPatch : Boolean = true
//
//    ) : RestResourceCalls{
//        val callTemplate = templates[template]?:
//        throw java.lang.IllegalArgumentException("$template does not exist in $path")
//        val ats = ActionsTemplateHandler.parseTemplate(template)
//        val result : MutableList<RestAction> = mutableListOf()
//        var resource : RestResource? = generateResource(template)
//
//        val skipBind : MutableList<RestAction> = mutableListOf()
//
//        if(createResource && ats[0] == HttpVerb.POST){
//            val nonPostIndex = ats.indexOfFirst { it != HttpVerb.POST }
//            val ac = getActionByHttpVerb(actions, if(nonPostIndex==-1) HttpVerb.POST else ats[nonPostIndex])!!.copy() as RestCallAction
//            randomizeActionGenes(ac, randomness)
//            result.add(ac)
//            var isCreated = createResourcesFor(ac, result, maxTestSize , randomness, checkSize )
//
//            val lastPost = result.last()
//            resource = RestResource(this, (lastPost as RestCallAction).parameters)
//            skipBind.addAll(result)
//            if(nonPostIndex == -1){
//                (1 until ats.size).forEach{
//                    result.add(lastPost.copy().also {
//                        skipBind.add(it as RestAction)
//                    } as RestAction)
//                }
//            }else{
//                if(nonPostIndex != ats.size -1){
//                    (nonPostIndex + 1 until ats.size).forEach {
//                        val ac = getActionByHttpVerb(actions, ats[it])!!.copy() as RestCallAction
//                        randomizeActionGenes(ac, randomness)
//                        result.add(ac)
//                    }
//                }
//            }
//
//        }else{
//            ats.forEach {at->
//                val ac = getActionByHttpVerb(actions, at)!!.copy() as RestCallAction
//                randomizeActionGenes(ac, randomness)
//                result.add(ac)
//            }
//
//            if(resource == null)
//                resource = RestResource(this, chooseLongestPath(result, randomness).also {
//                    skipBind.add(it)
//                }.parameters)
//
//        }
//
//        result.filterNot { ac -> skipBind.contains(ac) }.forEach { ac ->
//            if((ac as RestCallAction).parameters.isNotEmpty()){
//                ac.bindToSamePathResolution(ac.path, resource!!.params)
//            }
//        }
//        //FIXME repair params?
//        assert(resource!=null)
//        assert(result.isNotEmpty())
//
//        if(additionalPatch && !templates.getValue(template).independent && template.contains(HttpVerb.PATCH.toString())&&result.size + 1 <= maxTestSize){
//            val index = result.indexOfFirst { (it is RestCallAction) && it.verb == HttpVerb.PATCH }
//            val copy = result.get(index).copy() as RestAction
//            result.add(index, copy)
//        }
//        return RestResourceCalls(callTemplate, resource!!, result)
//
//
//    }

    //    fun handleAdded(calls: RestResourceCalls) : RestResourceCalls?{
//        if(templates.getValue(calls.actions.map { (it as RestCallAction).verb }.joinToString(ActionsTemplateHandler.SeparatorTemplate)).times == 1)
//            return null
//        val excluded = actions.filter { a -> !calls.actions.map { it.getName() }.contains(a.getName())}
//        val first = calls.actions.first()
//        val last = calls.actions.last()
//        if(first is RestCallAction && first.verb == HttpVerb.POST){
//            if(last is RestCallAction){
//                val actions = mutableListOf<RestAction>()
//                //POST...DELETE -> POST excluded Action
//                val resource = calls.resource.copy()
//                if(last.verb == HttpVerb.DELETE || !verbs[ActionsTemplateHandler.arrayHttpVerbs.indexOf(HttpVerb.DELETE)]){
//                    excluded.asSequence().sortedBy { actionSorted(it) }.forEach {
//                        if(it is RestCallAction){
//                            val a = it.copy() as RestCallAction
//                            a.bindToSamePathResolution(a.path.copy(), resource.params)
//                            actions.add(a)
//                        }
//                    }
//                }
//                //POST, GET/PATCH/PUT -> DELETE, POST, excluded Actions
//                else{
//                    if(verbs[ActionsTemplateHandler.arrayHttpVerbs.indexOf(HttpVerb.DELETE)]){
//                        excluded.asSequence().sortedBy { actionSorted(it, true) }.forEach {
//                            if(it is RestCallAction){
//                                val a = it.copy() as RestCallAction
//                                a.bindToSamePathResolution(a.path.copy(), resource.params)
//                                actions.add(a)
//                            }
//                        }
//                    }
//                }
//                if(actions.isNotEmpty()) return RestResourceCalls(resource,actions)
//            }
//        }
//        return null
//    }



}