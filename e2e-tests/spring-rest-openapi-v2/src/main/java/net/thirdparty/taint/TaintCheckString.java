package net.thirdparty.taint;

/**
 * Created by arcuri82 on 06-Sep-19.
 */
public class TaintCheckString {

    public static boolean check(String s){

        return s.equals("Another long string, but in a third-party package which is not part of coverage calculation for  the SUT");
    }
}
