package org.evomaster.client.java.instrumentation.testabilityexception;


import static org.evomaster.client.java.instrumentation.testabilityexception.ExceptionHeuristics.*;

/**
 * Created by arcuri82 on 25-Jun-19.
 */
public class IntegerExceptionHeuristics {


    public static double parseInt( String input) {

        if (input == null) {
            return 0d;
        }

        try{
            Integer.parseInt(input);
            //no exception, so was fine
            return 1d;
        }catch (Exception e){
        }

        final double base = FOR_NOT_NULL;

        if (input.length() == 0) {
            return base;
        }

        long distance = 0;

        if(input.length() == 1){
            //cannot be '-'
            distance += distanceToDigit(input.charAt(0));
        } else {
            for(int i=0; i<input.length(); i++){

                int digistDist = distanceToDigit(input.charAt(i));

                if(i==0){
                   //first symbol could be a '-'
                    distance += Math.min(digistDist, distanceToChar(input.charAt(i), '-'));
                } else if(i > 9){

                    //too long string would not be a valid 32bit integer representation
                    distance += MAX_CHAR_DISTANCE;
                } else {
                    distance += digistDist;
                }

            }
        }

        assert distance != 0; //otherwise try/catch would not have thrown exception

        //recall h in [0,1] where the highest the distance the closer to 0
        return base + ( (1d - base) / (distance + 1));
    }


}
