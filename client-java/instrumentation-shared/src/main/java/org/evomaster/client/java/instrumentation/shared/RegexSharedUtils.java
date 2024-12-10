package org.evomaster.client.java.instrumentation.shared;

public class RegexSharedUtils {


    public static String removeParentheses(String regex){

        String s = regex.trim();
        if(s.startsWith("(") && s.endsWith(")")){
           //would not work for "()()"
            // return removeParentheses(s.substring(1, s.length()-1));
        }
        return s;
    }

    /**
     * Make sure regex starts with ^ and ends with $
     * @param regex
     * @return
     */
    public static String forceFullMatch(String regex){

        String s = removeParentheses(regex);
        if(s.startsWith("^") && s.endsWith("$")){
            //nothing to do
            return s;
        }
        if(s.startsWith("^")){
            return s + "$";
        }
        if(s.endsWith("$")){
            return "^" + s;
        }

        return "^(" + s + ")$";
    }


    /**
     * Make sure that regex would match whole text even if originally would only match a subset
     * @param regex
     * @return
     */
    public static String handlePartialMatch(String regex){

        /*
            Bit tricky... (.*) before/after the regex would not work, as by default . does
            not match line terminators. enabling DOTALL flag is risky, as the original could
            use flags.
            \s\S is just a way to covering everything
         */

        String s = removeParentheses(regex);
        if(s.startsWith("^") && s.endsWith("$")){
            //nothing to do
            return s;
        }
        if(s.startsWith("^")){
            return s + "([\\s\\S]*)";
        }
        if(s.endsWith("$")){
            return "([\\s\\S]*)" + s;
        }

        return String.format("([\\s\\S]*)(%s)([\\s\\S]*)", regex);
    }


}
