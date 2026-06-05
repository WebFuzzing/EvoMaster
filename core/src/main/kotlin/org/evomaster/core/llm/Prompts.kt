package org.evomaster.core.llm

object Prompts {

    const val VALUE_BASED_ON_NAME_SYSTEM = """
        You are a software tester, tasked to test an API. 
        There is a string parameter with a given name [name]. 
        The value of this [name] will be provided at the end of this prompt.
         
        As a tester, you need to create 20 different valid values for this parameter called [name].
        The goal is to get values that will pass the input parameter validation of the API. 
        However, considering the goal of software testing, you need to generate different and variegated inputs.
        Inputs MUST be unique, do not repeat any value.
        
        Your output MUST be a valid JSON array of strings.
        Such value will be parsed with a JSON library (e.g., Jackson) and it MUST not lead to any parsing error. 
        
        For example, consider the [name] being "color", then the output array with 20 strings about color could be:
        
        ["🟦", color-mix(in srgb, red 30%, blue 70%)","--primary-color","hwb(120, 20%, 40%)","rgba(255, 99, 71, 0.5)", "red", "green", "blue", "yellow", "#FF5733", "rgb(255, 99, 71)", "RoyalBlue", "royalblue", "RED", "#FFF", "#F0F", "#a1b2c3", "white", "currentColor", "hsl(361, 100%, 50%)"]
        
        Return ONLY a JSON array of strings. No explanations, no preamble, no markdown formatting, no natural language. 
        Example: ["apple", "banana", "cherry"]

        Your response must start with '[' and end with ']'.
        
        Your response must be on a single line. Do NOT use any line break. 
        Correct example: ["apple", "banana", "cherry"]
        Wrong example: ["apple", 
                        "banana", 
                        "cherry"]
        
        Optionally, after the [name], I might give you a [description] text that explain what the name and parameter is about.
        If this optional [description] is provided, you MUST use its information when choosing half, and only half, of the 
        of the 20 values you are going to create. 
        I.e., if a [description] is provided, 10 values MUST not use it (but still be based on the semantics of [name]), 
        whereas other 10 MUST use it (together with the semantics of [name]).  
        
        IMPORTANT: The requirement to split values between using and not using the description is mandatory and takes precedence 
        over realism, API-validation likelihood, inferred constraints, or any format specified in the description. 
        If a description is provided, exactly 10 of the 20 values must ignore the description and exactly 10 must use it; 
        generating all 20 values according to the description is always considered incorrect.                
    """


    const val VALUE_BASED_ON_NAME_FAILURE = """
        You have failed at your task. 
        I asked you to return a valid array of strings, and only that.
        You need to re-do the task, following the instructions you have been given.
        When I tried to parse such array of strings you gave me, I got 
        the following error message:
    """

    fun getPromptForNameDescription(name: String, description: String?): Pair<String,String> {
        var user = "Your input is\n [name]:$name"
        if(description != null) {
            user += "\n[description]: $description"
        }
        return Pair(VALUE_BASED_ON_NAME_SYSTEM, user)
    }

    fun getPromptForFailedName(error: String): Pair<String,String>{
        return Pair(VALUE_BASED_ON_NAME_FAILURE, error)
    }

}


