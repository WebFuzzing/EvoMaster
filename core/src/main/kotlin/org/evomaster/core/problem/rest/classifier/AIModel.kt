package org.evomaster.core.problem.rest.classifier

import org.evomaster.core.problem.rest.classifier.quantifier.ModelEvaluation
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult

/**
 * Interface representing an AI-based model for analyzing or predicting the behavior of RESTful web services.
 * Implementations of this interface can be used to update the internal model with observations
 * and classify the likely outcome or response pattern of a given API call.
 */
interface AIModel {

    /**
     * Updates the internal AI model using the given input and its corresponding output.
     *
     * This method is typically called after an actual REST API call is executed.
     * The model learns from the relationship between the request (`input`) and the observed result (`output`)
     * to improve its ability to predict future behaviors or outcomes.
     * @param input the REST API request that was sent
     * @param output the observed result or response from the API for the given request
     */
    fun updateModel(input: RestCallAction, output: RestCallResult)

    /**
     * Classifies the given REST API call input based on the model's current understanding.
     * This method predicts or assesses the expected outcome of the request without actually executing it.
     * It can be used to guide test generation, prioritize execution paths, or filter irrelevant test cases.
     * @param input the REST API request to be classified
     * @return an instance of [AIResponseClassification] describing the confidence level
     */
    fun classify(input: RestCallAction): AIResponseClassification

    /**
     *  Return metrics such as accuracy, precision, and F1Score of the model.
     *  The model is learned dynamically throughout the search.
     *  Especially at the beginning of the search, the model will be unreliable.
     *  These metrics estimate can then be used by EvoMaster to make a non-deterministic decision on whether
     *  the model should be used or not yet for classification, or if it needs more training first.
     *
     *  An API can have many different endpoints.
     *  Based on the training data, the metrics might be different between endpoints.
     */
    fun estimateMetrics(endpoint: Endpoint): ModelEvaluation

    /**
     * Return the overall measurements of the metrics on how efficient the model is.
     * This is based on all endpoints.
     * If the model internally stores separated submodels for each endpoint,
     * then this could be seen as the average metrics.
     */
    fun estimateOverallMetrics(): ModelEvaluation

}
