package com.foo.rpc.examples.spring.thrifttest;

import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ThriftTestImp implements ThriftTest.Iface{
    /**
     * Prints "testVoid()" and returns nothing.
     */
    @Override
    public void testVoid() throws TException {
        System.out.println("testVoid()");
    }

    /**
     * Prints 'testString("%s")' with thing as '%s'
     *
     * @param thing@return string - returns the string 'thing'
     */
    @Override
    public String testString(String thing) throws TException {
        System.out.printf("testString(\"%s\")%n", thing);
        return thing;
    }

    /**
     * Prints 'testBool("%s")' where '%s' with thing as 'true' or 'false'
     *
     * @param thing@return bool  - returns the bool 'thing'
     */
    @Override
    public boolean testBool(boolean thing) throws TException {
        System.out.printf("testBool(\"%s\")%n", thing);
        return thing;
    }

    /**
     * Prints 'testByte("%d")' with thing as '%d'
     * The types i8 and byte are synonyms, use of i8 is encouraged, byte still exists for the sake of compatibility.
     *
     * @param thing@return i8 - returns the i8/byte 'thing'
     */
    @Override
    public byte testByte(byte thing) throws TException {
        System.out.printf("testByte(\"%d\")%n", thing);
        return thing;
    }

    /**
     * Prints 'testI32("%d")' with thing as '%d'
     *
     * @param thing@return i32 - returns the i32 'thing'
     */
    @Override
    public int testI32(int thing) throws TException {
        System.out.printf("testI32(\"%d\")%n", thing);
        return thing;
    }

    /**
     * Prints 'testI64("%d")' with thing as '%d'
     *
     * @param thing@return i64 - returns the i64 'thing'
     */
    @Override
    public long testI64(long thing) throws TException {
        System.out.printf("testI64(\"%d\")%n", thing);
        return thing;
    }

    /**
     * Prints 'testDouble("%f")' with thing as '%f'
     *
     * @param thing@return double - returns the double 'thing'
     */
    @Override
    public double testDouble(double thing) throws TException {
        System.out.printf("testDouble(\"%f\")%n", thing);
        return thing;
    }

    /**
     * Prints 'testBinary("%s")' where '%s' is a hex-formatted string of thing's data
     *
     * @param thing@return binary  - returns the binary 'thing'
     */
    @Override
    public ByteBuffer testBinary(ByteBuffer thing) throws TException {
        System.out.printf("testBinary(\"%s\")%n", thing);
        return thing;
    }

    /**
     * Prints 'testStruct("{%s}")' where thing has been formatted into a string of comma separated values
     *
     * @param thing@return Xtruct - returns the Xtruct 'thing'
     */
    @Override
    public Xtruct testStruct(Xtruct thing) throws TException {
        System.out.printf("testStruct(\"{%s}\")%n", String.join(",",Arrays.asList(thing.string_thing, ""+thing.i32_thing, ""+thing.i64_thing, ""+thing.byte_thing)));
        return thing;
    }

    /**
     * Prints 'testNest("{%s}")' where thing has been formatted into a string of the nested struct
     *
     * @param thing@return Xtruct2 - returns the Xtruct2 'thing'
     */
    @Override
    public Xtruct2 testNest(Xtruct2 thing) throws TException {
        String xtruct = String.join(",", Arrays.asList(thing.struct_thing.string_thing, ""+thing.struct_thing.i32_thing, ""+thing.struct_thing.i64_thing, ""+thing.struct_thing.byte_thing));
        System.out.printf("testNest(\"{%s}\")%n", String.join(",", Arrays.asList(""+thing.i32_thing, xtruct, ""+thing.byte_thing)));
        return thing;
    }

    /**
     * Prints 'testMap("{%s")' where thing has been formatted into a string of 'key => value' pairs
     * separated by commas and new lines
     *
     * @param thing@return map<i32,i32> - returns the map<i32,i32> 'thing'
     */
    @Override
    public Map<Integer, Integer> testMap(Map<Integer, Integer> thing) throws TException {
        for (Map.Entry e: thing.entrySet()){
            System.out.printf("testMap(\"{%s}\")%n", "<"+e.getKey() + ","+ e.getValue()+">");
        }
        return thing;
    }

    /**
     * Prints 'testStringMap("{%s}")' where thing has been formatted into a string of 'key => value' pairs
     * separated by commas and new lines
     *
     * @param thing@return map<string,string> - returns the map<string,string> 'thing'
     */
    @Override
    public Map<String, String> testStringMap(Map<String, String> thing) throws TException {
        for (Map.Entry e: thing.entrySet()){
            System.out.printf("testStringMap(\"{%s}\")%n", "<"+e.getKey() + ","+ e.getValue()+">");
        }
        return thing;
    }

    /**
     * Prints 'testSet("{%s}")' where thing has been formatted into a string of values
     * separated by commas and new lines
     *
     * @param thing@return set<i32> - returns the set<i32> 'thing'
     */
    @Override
    public Set<Integer> testSet(Set<Integer> thing) throws TException {
        for (Integer e: thing){
            System.out.printf("testSet(\"{%s}\")%n", "<"+e+">");
        }
        return thing;
    }

    /**
     * Prints 'testList("{%s}")' where thing has been formatted into a string of values
     * separated by commas and new lines
     *
     * @param thing@return list<i32> - returns the list<i32> 'thing'
     */
    @Override
    public List<Integer> testList(List<Integer> thing) throws TException {
        for (Integer e: thing){
            System.out.printf("testList(\"{%s}\")%n", "<"+e+">");
        }
        return thing;
    }

    /**
     * Prints 'testEnum("%d")' where thing has been formatted into its numeric value
     *
     * @param thing@return Numberz - returns the Numberz 'thing'
     */
    @Override
    public Numberz testEnum(Numberz thing) throws TException {
        System.out.printf("testList(\"{%s}\")%n", Arrays.stream(Numberz.values()).map(s-> s.getValue()+"").collect(Collectors.joining(",")));
        return thing;
    }

    /**
     * Prints 'testTypedef("%d")' with thing as '%d'
     *
     * @param thing@return UserId - returns the UserId 'thing'
     */
    @Override
    public long testTypedef(long thing) throws TException {
        System.out.printf("testTypedef(\"{%d}\")%n", ""+thing);
        return thing;
    }

    /**
     * Prints 'testMapMap("%d")' with hello as '%d'
     *
     * @param hello@return map<i32,map<i32,i32>> - returns a dictionary with these values:
     *                     {-4 => {-4 => -4, -3 => -3, -2 => -2, -1 => -1, }, 4 => {1 => 1, 2 => 2, 3 => 3, 4 => 4, }, }
     */
    @Override
    public Map<Integer, Map<Integer, Integer>> testMapMap(int hello) throws TException {
        System.out.printf("testMapMap(\"{%d}\")%n", hello);
        Map<Integer, Map<Integer, Integer>> map = new HashMap<Integer, Map<Integer, Integer>>(){{
            put(-4, new HashMap<Integer, Integer>(){{
                put(-4, -4);
                put(-3, -3);
                put(-2, -2);
                put(-1, -1);
            }});

            put(4, new HashMap<Integer, Integer>(){{
                put(1, 1);
                put(2, 2);
                put(3, 3);
                put(4, 4);
            }});
        }};

        return map;
    }

    /**
     * So you think you've got this all worked out, eh?
     * <p>
     * Creates a map with these values and prints it out:
     * { 1 => { 2 => argument,
     * 3 => argument,
     * },
     * 2 => { 6 => <empty Insanity struct>, },
     * }
     *
     * @param argument
     * @return map<UserId, map < Numberz, Insanity>> - a map with the above values
     */
    @Override
    public Map<Long, Map<Numberz, Insanity>> testInsanity(Insanity argument) throws TException {

        Map<Long, Map<Numberz, Insanity>> map = new HashMap<Long, Map<Numberz, Insanity>>(){{
            put(1L, new HashMap<Numberz, Insanity>(){{
                put(Numberz.TWO, argument);
                put(Numberz.THREE, argument);

            }});

            put(2L, new HashMap<Numberz, Insanity>(){{
                put(Numberz.SIX, new Insanity());
            }});
        }};

        return map;
    }

    /**
     * Prints 'testMulti()'
     *
     * @param arg0
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @param arg5
     * @return Xtruct - returns an Xtruct with string_thing = "Hello2, byte_thing = arg0, i32_thing = arg1
     * and i64_thing = arg2
     */
    @Override
    public Xtruct testMulti(byte arg0, int arg1, long arg2, Map<Short, String> arg3, Numberz arg4, long arg5) throws TException {
        System.out.println("testMulti()");
        Xtruct xtruct = new Xtruct();
        xtruct.byte_thing = arg0;
        xtruct.i32_thing = arg1;
        xtruct.i64_thing = arg2;
        xtruct.string_thing = String.format("Hello2, byte_thing = %d, i32_thing = %d and i64_thing = %d", arg0, arg1, arg2);
        return xtruct;
    }

    /**
     * Print 'testException(%s)' with arg as '%s'
     *
     * @param arg
     */
    @Override
    public void testException(String arg) throws Xception, TException {
        System.out.printf("testException(%s)%n", arg);
    }

    /**
     * Print 'testMultiException(%s, %s)' with arg0 as '%s' and arg1 as '%s'
     *
     * @param arg0
     * @param arg1
     * @return Xtruct - an Xtruct with string_thing = arg1
     */
    @Override
    public Xtruct testMultiException(String arg0, String arg1) throws Xception, Xception2, TException {
        System.out.printf("testMultiException(%s, %s)%n", arg0, arg1);
        Xtruct xtruct = new Xtruct();
        xtruct.string_thing = arg1;
        return xtruct;
    }

    /**
     * Print 'testOneway(%d): Sleeping...' with secondsToSleep as '%d'
     * sleep 'secondsToSleep'
     * Print 'testOneway(%d): done sleeping!' with secondsToSleep as '%d'
     *
     * @param secondsToSleep
     */
    @Override
    public void testOneway(int secondsToSleep) throws TException {
        System.out.printf("testOneway(%d)%n", secondsToSleep);
        int maxs = Math.min(Math.max(secondsToSleep, 0), 3);
        try {
            Thread.sleep(maxs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
