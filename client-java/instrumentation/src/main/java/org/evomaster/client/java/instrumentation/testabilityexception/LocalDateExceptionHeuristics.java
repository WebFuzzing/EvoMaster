package org.evomaster.client.java.instrumentation.testabilityexception;

import java.time.LocalDate;

import static org.evomaster.client.java.instrumentation.testabilityexception.ExceptionHeuristics.*;

/**
 * Created by arcuri82 on 25-Jun-19.
 */
public class LocalDateExceptionHeuristics {

    public static double parse(String input){

        if (input == null) {
            return H_REACHED_BUT_NULL;
        }

        try{
            LocalDate.parse(input);
            //no exception, so was fine
            return 1d;
        }catch (Exception e){
        }

        final double base = ExceptionHeuristics.H_NOT_NULL;

        long distance = 0;

        for(int i=0; i<input.length(); i++){

            char c = input.charAt(i);

            //format YYYY-MM-DD

            if(i>=0 && i<=3){
                //any Y value is ok
                distance += distanceToDigit(c);
            } else if(i==4 || i==7){
                distance += distanceToChar(c, '-');
            } else if(i==5){
                //let's simplify and only allow 01 to 09 for MM
                distance += distanceToChar(c, '0');
            } else if(i==6){
                distance += distanceToRange(c, '1', '9');
            } else if(i==8){
                //let's simplify and only allow 01 to 28
                distance += distanceToRange(c, '0', '2');
            } else if(i==9){
                distance += distanceToRange(c, '1', '8');
            } else {
                distance += MAX_CHAR_DISTANCE;
            }
        }

        if(input.length() < 10){
            //too short
            distance += (MAX_CHAR_DISTANCE * (10 - input.length()));
        }

        assert distance != 0; //otherwise try/catch would not have thrown exception

        //recall h in [0,1] where the highest the distance the closer to 0
        return base + ( (1d - base) / (distance + 1));
    }
}
