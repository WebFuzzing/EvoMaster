package org.evomaster.core.problem.rest.serviceII.resources

import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.auth.AuthenticationInfo
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.serviceII.BindParams
import org.evomaster.core.problem.rest.serviceII.HandleActionTemplate
import org.evomaster.core.problem.rest.serviceII.RestIndividualII
import org.evomaster.core.search.Action
import org.evomaster.core.search.service.Randomness
import kotlin.math.max

class RestAResource (val path : RestPath, val actions: MutableList<RestAction>, val maxResources: Int = 100, val countTempVisiting : Boolean = false){

    //ancestor is ordered, first is closest ancestor, and last is deepest one.
    private val ancestors : MutableList<RestAResource> = mutableListOf()

    //this attribute makes sense when db exists
    private val existingResources : MutableList<RestResource> = mutableListOf()
    val templates : MutableMap<String, CallsTemplate> = mutableMapOf()

    //By default, a RestAbstractResource is independent, and it also has independent actions
    var independent = true
    var hasIndependentAction = true

    private var probInd = 0.0
    private val unspecifiedPr = -1.0

    var enableSkip = false
    var verbs : Array<Boolean> = Array(6){false}

    fun setAncestors(resources : List<RestAResource>){
        resources.forEach {r ->
            if(!r.path.isEquivalent(this.path) && r.path.isAncestorOf(this.path)) ancestors.add(r)
        }
    }

    fun handleAdded(calls: RestResourceCalls) : RestResourceCalls?{
        if(templates.getValue(calls.actions.map { (it as RestCallAction).verb }.joinToString(HandleActionTemplate.SeparatorTemplate)).times == 1)
            return null
        val excluded = actions.filter { a -> !calls.actions.map { it.getName() }.contains(a.getName())}
        val first = calls.actions.first()
        val last = calls.actions.last()
        if(first is RestCallAction && first.verb == HttpVerb.POST){
            if(last is RestCallAction){
                val actions = mutableListOf<RestAction>()
                //POST...DELETE -> POST excluded Action
                val resource = calls.resource.copy()
                if(last.verb == HttpVerb.DELETE || !verbs[HandleActionTemplate.arrayHttpVerbs.indexOf(HttpVerb.DELETE)]){
                    excluded.asSequence().sortedBy { actionSorted(it) }.forEach {
                        if(it is RestCallAction){
                            val a = it.copy() as RestCallAction
                            a.bindToSamePathResolution(a.path.copy(), resource.params)
                            actions.add(a)
                        }
                    }
                }
                //POST, GET/PATCH/PUT -> DELETE, POST, excluded Actions
                else{
                    if(verbs[HandleActionTemplate.arrayHttpVerbs.indexOf(HttpVerb.DELETE)]){
                        excluded.asSequence().sortedBy { actionSorted(it, true) }.forEach {
                            if(it is RestCallAction){
                                val a = it.copy() as RestCallAction
                                a.bindToSamePathResolution(a.path.copy(), resource.params)
                                actions.add(a)
                            }
                        }
                    }
                }
                if(actions.isNotEmpty()) return RestResourceCalls(resource,actions)
            }
        }
        return null
    }


    fun initVerbs(){
        actions.forEach { a->
            when((a as RestCallAction).verb){
                HttpVerb.POST -> verbs[HandleActionTemplate.arrayHttpVerbs.indexOf(HttpVerb.POST)] = true
                HttpVerb.GET -> verbs[HandleActionTemplate.arrayHttpVerbs.indexOf(HttpVerb.GET)] = true
                HttpVerb.PUT -> verbs[HandleActionTemplate.arrayHttpVerbs.indexOf(HttpVerb.PUT)] = true
                HttpVerb.PATCH -> verbs[HandleActionTemplate.arrayHttpVerbs.indexOf(HttpVerb.PATCH)] = true
                HttpVerb.DELETE ->verbs[HandleActionTemplate.arrayHttpVerbs.indexOf(HttpVerb.DELETE)] = true
            }
        }
        verbs[5] = verbs[0]
        if (!verbs[0]){
            verbs[0] = if(ancestors.isEmpty()) false
            else ancestors.filter{ p -> p.actions.filter { a -> (a as RestCallAction).verb == HttpVerb.POST }.isNotEmpty() }.isNotEmpty()
        }

        HandleActionTemplate.initSampleSpaceOnlyPOST(verbs, templates)

        templates.values.find { !it.independent }?.let {
            independent = false
            hasIndependentAction = (templates.size != it.size)
        }

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
        templates.filter { e->!e.key.contains(HandleActionTemplate.SeparatorTemplate) }.values.forEach {
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
        if(BindParams.numOfBodyParam(params) in 1..(params.size -1)){
            val bp = params.find { p -> p is BodyParam }!! as BodyParam
            BindParams.bindParam(bp, path, path, params.filter { p -> !(p is BodyParam )})
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
        if(enableSkip) skipActions()
        selectTemplate({ call : CallsTemplate -> call.independent || (call.template == HttpVerb.POST.toString() && call.size > 1)}, randomness)?.let {
            return genCalls(it.template, randomness, maxTestSize, false, false)
        }
        //return genCalls(HttpVerb.POST.toString(), randomness)
        //return sampleOneAction( null, randomness, 1)
        return genCalls(HttpVerb.POST.toString(), randomness,maxTestSize)
    }


    fun createCallByVerb(verb : HttpVerb, resources: RestResource) : RestResourceCalls?{
        val exist = verbs[HandleActionTemplate.arrayHttpVerbs.indexOf(verb)]
        if(exist){
            var ac = getActionByHttpVerbUntil(verb)!!.copy()
            (ac as RestCallAction).bindToSamePathResolution(ac.path, resources.params)
            return RestResourceCalls(resources.copy(), mutableListOf(ac))
        }
        return null
    }
    fun sampleOneAction(verb : HttpVerb? = null, randomness: Randomness, maxTestSize: Int) : RestResourceCalls{
        val al = if(verb != null) getActionByHttpVerb(actions, verb) else randomness.choose(actions).copy() as RestAction
        return sampleOneAction(al!!, randomness, maxTestSize)
    }

    fun sampleOneAction(action : RestAction, randomness: Randomness, maxTestSize: Int) : RestResourceCalls{
        assert(maxTestSize > 0)
        val copy = action.copy()
        randomizeActionGenes(copy as RestCallAction, randomness)
        return RestResourceCalls(RestResource(this, copy.parameters), mutableListOf(copy))
    }

    fun sampleRestResourceCallsOld(randomness: Randomness, maxTestSize: Int) : RestResourceCalls{
        assert(maxTestSize > 0)

        if(enableSkip) skipActions()
        val chosen = templates.filter { e->
            (e.key.split(HandleActionTemplate.SeparatorTemplate).size) in 2..maxTestSize
        }
        if(chosen.isEmpty())
            return sampleIndResourceCall(randomness, maxTestSize)
        if(existingResources.isEmpty()){
            //there exists an issue when post failed
            val chosenWithPost = chosen.filter { it.key.contains(HttpVerb.POST.toString()) }
            return genCalls(randomness.choose(chosenWithPost).template,randomness, maxTestSize)
        }else{
            //FIXME Man, it should not be executed
            val chosenWithoutPost = chosen.filter { !it.key.contains(HttpVerb.POST.toString()) }
            return genCalls(randomness.choose(chosenWithoutPost).template,randomness, maxTestSize)
        }
    }

    fun sampleRestResourceCalls(randomness: Randomness, maxTestSize: Int) : RestResourceCalls{
        assert(maxTestSize > 0)
        if(enableSkip) skipActions()
        val chosen = templates.filter { !it.value.independent && it.value.size <= maxTestSize }
        if(chosen.isEmpty())
            return sampleIndResourceCall(randomness, maxTestSize)
        if(existingResources.isEmpty()){
            val chosenWithPost = selectTemplate({call : CallsTemplate -> !call.independent && call.size <= maxTestSize}, randomness, chosen)
            chosenWithPost?.let {
                return genCalls(it.template,randomness, maxTestSize)
            }
            return sampleIndResourceCall(randomness, maxTestSize)

        }else{
            val chosenWithoutPost = chosen.filter { !it.key.contains(HttpVerb.POST.toString()) && it.value.size <= maxTestSize}
            return genCalls(randomness.choose(chosenWithoutPost).template,randomness, maxTestSize)
        }
    }


    private fun genCalls(template : String, randomness: Randomness, maxTestSize : Int = 1, checkSize : Boolean = false, createResource : Boolean = true, additionalPatch : Boolean = true) : RestResourceCalls{
        if(!templates.containsKey(template))
            throw java.lang.IllegalArgumentException("$template does not exist in ${path.toString()}")
        val ats = HandleActionTemplate.parseTemplate(template)
        val result : MutableList<RestAction> = mutableListOf()
        var resource : RestResource? = generateResource(template)

        val skipBind : MutableList<RestAction> = mutableListOf()

        if(createResource && ats[0] == HttpVerb.POST){
            val nonPostIndex = ats.indexOfFirst { it != HttpVerb.POST }
            val ac = getActionByHttpVerb(actions, if(nonPostIndex==-1) HttpVerb.POST else ats[nonPostIndex])!!.copy() as RestCallAction
            randomizeActionGenes(ac, randomness)
            result.add(ac)
            var isCreated = createResourcesFor(ac, result, maxTestSize , randomness)
            when(isCreated){
                -1 -> {
                    println("exceed the allowed size!")
                    //throw IllegalStateException("exceed the allowed size!")
                }
                -2 -> {
                    println("cannot find the post action in this resource ${this.path} and its ancestor")//throw IllegalStateException("cannot find the post action in this resource ${this.path} and its ancestor")
                    //var isCreated = createResourcesFor(ac, result, maxTestSize , randomness)
                }
                -3 -> {
                    println("cannot manipulate this resource ${this.path} due to failure to create dependent resource")//TODO report problems of SUT
                    //var isCreated = createResourcesFor(ac, result, maxTestSize , randomness)
                }
            }
            if(checkSize && isCreated == 0 ){
                if(result.size != if(nonPostIndex == -1) 1 else (nonPostIndex + 1)){
                    templates.filter { it.key.contains(HttpVerb.POST.toString()) }.forEach{temp->
                        temp.value.size = result.size + ats.size - (if(nonPostIndex == -1) 1 else (nonPostIndex + 1))
                        temp.value.sizeAssured = true
                    }
                }else{
                    templates.getValue(template).sizeAssured = true
                }
            }
            val lastPost = result.last()
            resource = RestResource(this, (lastPost as RestCallAction).parameters)
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
                        val ac = getActionByHttpVerb(actions, ats[it])!!.copy() as RestCallAction
                        randomizeActionGenes(ac, randomness)
                        result.add(ac)
                    }
                }
            }

        }else{
            ats.forEach {at->
                val ac = getActionByHttpVerb(actions, at)!!.copy() as RestCallAction
                randomizeActionGenes(ac, randomness)
                result.add(ac)
            }

            if(resource == null)
                resource = RestResource(this, chooseLongestPath(result, randomness).also {
                    skipBind.add(it)
                }.parameters)

            if(checkSize){
                templates.getValue(template).sizeAssured = (result.size  == templates.getValue(template).size)
            }
        }

        result.filterNot { ac -> skipBind.contains(ac) }.forEach { ac ->
            if((ac as RestCallAction).parameters.isNotEmpty()){
                ac.bindToSamePathResolution(ac.path, resource!!.params)
            }
        }
        //FIXME repair params?
        assert(resource!=null)
        assert(result.isNotEmpty())

        if(additionalPatch && !templates.getValue(template).independent && template.contains(HttpVerb.PATCH.toString())&&result.size + 1 <= maxTestSize){
            val index = result.indexOfFirst { (it is RestCallAction) && it.verb == HttpVerb.PATCH }
            val copy = result.get(index).copy() as RestAction
            result.add(index, copy)
        }
        return RestResourceCalls(resource!!, result)


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

    private fun chooseLongestPath(actions: List<RestAction>, randomness: Randomness): RestCallAction {

        if (actions.isEmpty()) {
            throw IllegalArgumentException("Cannot choose from an empty collection")
        }

        val max = actions.filter { it is RestCallAction }.asSequence().map { a -> (a as RestCallAction).path.levels() }.max()!!
        val candidates = actions.filter { a -> a is RestCallAction && a.path.levels() == max }

        return randomness.choose(candidates).copy() as RestCallAction
    }

    private fun chooseClosestAncestor(target: RestCallAction, verbs: List<HttpVerb>, randomness: Randomness): RestCallAction? {

        var others = sameOrAncestorEndpoints(target)
        others = hasWithVerbs(others, verbs).filter { t -> t.getName() != target.getName() }

        if (others.isEmpty()) {
            return null
        }

        return chooseLongestPath(others, randomness)
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

    private fun createResourcesFor(target: RestCallAction, test: MutableList<RestAction>, maxTestSize: Int, randomness: Randomness)
            : Int {

        if (test.size >= maxTestSize) {
            return -1
        }
        var template = chooseClosestAncestor(target, listOf(HttpVerb.POST), randomness)?:return (if(target.verb == HttpVerb.POST) 0 else -2)

        val post = createActionFor(template, target, randomness)

        test.add(0, post)

        /*
            Check if POST depends itself on the creation of
            some intermediate resource
         */
        if (post.path.hasVariablePathParameters() &&
                (!post.path.isLastElementAParameter()) ||
                post.path.getVariableNames().size >= 2) {
            val dependencyCreated = createResourcesFor(post, test, maxTestSize, randomness)
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

    //TODO skip Action based on probability i.e., prob = 0.0
    private fun skipActions(){
        if(verbs[HandleActionTemplate.arrayHttpVerbs.indexOf(HttpVerb.GET)]){
            val action = getActionByHttpVerb(actions, HttpVerb.GET)
            if(action is RestCallAction){
                if(action.parameters.size == 0){
                    verbs[HandleActionTemplate.arrayHttpVerbs.indexOf(HttpVerb.GET)] = false
                    templates.remove(HttpVerb.GET.toString())
                }
            }
        }
    }

    fun isAnyAction() : Boolean{
        skipActions()
        verbs.forEach {
            if (it) return true
        }
        return false
    }

    class CallsTemplate (val template: String, val independent : Boolean, var size : Int = 1, var probability : Double = -1.0, var times : Int = 0, var sizeAssured : Boolean = false)

}