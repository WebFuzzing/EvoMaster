package org.evomaster.core.llm

object Prompts {

    const val VALUE_BASED_ON_NAME = """
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
        
        Your input is [name]:
    """
}


