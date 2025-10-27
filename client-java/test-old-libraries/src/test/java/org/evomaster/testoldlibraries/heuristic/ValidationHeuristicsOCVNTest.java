package org.evomaster.testoldlibraries.heuristic;

import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.instrumentation.heuristic.ValidatorHeuristics;
import org.evomaster.testoldlibraries.heuristic.validator.ocvn.YearFilterPagingRequest;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationHeuristicsOCVNTest {

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    public void testValidBean(){

        YearFilterPagingRequest bean = new YearFilterPagingRequest();

        /*
          "notBidTypeId=2UovsPHTsJixv&notBidTypeId=cxST8vTrYc7pNL&" +
                    "procuringEntityId=Q&procuringEntityId=TW9UFPlvaDx&procuringEntityId=xLr0vU&procuringEntityId=NqscaQ5lLcrx&procuringEntityId=kwBUAXyv3m5UXM0x&" +
                    "notBidSelectionMethod=&" +
                    "contrMethod=thet963mO&contrMethod=wVME0zLEqj1p&" +
                    "planningLoc=ryQHUaEbMP8w3RTm&planningLoc=dLanzjbDS7XXK&" +
                    "tenderLoc=&" +
                    "procurementMethod=TzRs8k3lU&procurementMethod=_Xt6kz3FCVBVo4b&procurementMethod=0VW&" +
                    "procuringEntityCityId=SZMt3FSI1UQ&procuringEntityCityId=ZnpIiH&procuringEntityCityId=EFgWo&procuringEntityCityId=a&" +
                    "year=&" +
                    "pageNumber=145&" +
                    "maxAwardValue=0.7342085029250233&" +
                    "monthly=true")
         */
        bean.setNotBidTypeId(tree("2UovsPHTsJixv","cxST8vTrYc7pNL"));
        bean.setProcuringEntityId(tree("Q","TW9UFPlvaDx","xLr0vU","NqscaQ5lLcrx","kwBUAXyv3m5UXM0x"));
        bean.setContrMethod(tree("thet963mO","wVME0zLEqj1p"));
        bean.setPlanningLoc(tree("ryQHUaEbMP8w3RTm","dLanzjbDS7XXK"));
        bean.setProcurementMethod(tree("TzRs8k3lU","_Xt6kz3FCVBVo4b","0VW"));
        bean.setProcuringEntityCityId(tree("SZMt3FSI1UQ","ZnpIiH","EFgWo","a"));
        bean.setPageNumber(145);
        bean.setMaxAwardValue(new BigDecimal(0.7342085029250233));
        bean.setMonthly(true);

        checkValid(bean);

        bean.setNotBidTypeId(null);
        checkValid(bean);

        bean.setProcuringEntityId(null);
        checkValid(bean);

        bean.setContrMethod(null);
        checkValid(bean);

        bean.setPlanningLoc(null);
        checkValid(bean);

        bean.setProcurementMethod(null);
        checkValid(bean);

        bean.setProcuringEntityCityId(null);
        checkValid(bean);

        bean.setPageNumber(null);
        checkValid(bean);

        bean.setMaxAwardValue(null);
        checkValid(bean);

        bean.setMonthly(null);
        checkValid(bean);
    }


    @Test
    public void testNotValid(){

        YearFilterPagingRequest bean = new YearFilterPagingRequest();

        /*
                     "bidTypeId=_EM_51643_XYZ_&bidTypeId=_EM_51644_XYZ_&bidTypeId=6o_QhU9oaMcd&bidTypeId=_EM_51645_XYZ_&" +
                    "notBidTypeId=_EM_51646_XYZ_&notBidTypeId=&" +
                    "supplierId=_EM_51655_XYZ_&supplierId=_EM_51656_XYZ_&supplierId=_EM_51657_XYZ_&" +
                    "notBidSelectionMethod=yEmvkh%5C9zkjt&notBidSelectionMethod=_EM_97067_XYZ_&" +
                    "planningLoc=_EM_51668_XYZ_&planningLoc=_EM_51669_XYZ_&planningLoc=_EM_51670_XYZ_&planningLoc=_EM_51671_XYZ_&" +
                    "procuringEntityCityId=_EM_97079_XYZ_&" +
                    "year=920&year=790&year=185&year=33554950&" +
                    "month=143&" +
                    "pageNumber=904&" +
                    "pageSize=805&" +
                    "minTenderValue=0.7838790626531434&" +
                    "minAwardValue=0.923654721967997&" +
                    "maxAwardValue=0.9690545683747289&" +
                    "flagged=false&" +
                    "monthly=false&" +
                    "EMextraParam123=_EM_51676_XYZ_&" +
                    "yearFilterPagingRequest=_EM_97080_XYZ_&" +
                    "skip=cyTExT8Vi&" +
                    "class=HNYSE_3UEO0")
         */

        bean.setBidTypeId(tree("_EM_51643_XYZ_", "_EM_51644_XYZ_", "6o_QhU9oaMcd", "_EM_51645_XYZ_"));
        bean.setNotBidTypeId(tree("_EM_51646_XYZ_",""));
        bean.setSupplierId(tree("_EM_51655_XYZ_","_EM_51656_XYZ_","_EM_51657_XYZ_"));
        bean.setNotBidSelectionMethod(tree("yEmvkh%5C9zkjt","_EM_97067_XYZ_"));
        bean.setPlanningLoc(tree("_EM_51668_XYZ_","_EM_51669_XYZ_","_EM_51670_XYZ_","_EM_51671_XYZ_"));
        bean.setProcuringEntityCityId(tree("_EM_97079_XYZ_"));
        bean.setYear(tree(920,790,185,33554950));
        bean.setMonth(tree(143));
        bean.setPageNumber(904);
        bean.setPageSize(805);
        bean.setMinTenderValue(new BigDecimal(0.7838790626531434));
        bean.setMinAwardValue(new BigDecimal(0.923654721967997));
        bean.setMaxAwardValue(new BigDecimal(0.9690545683747289));
        bean.setFlagged(false);
        bean.setMonthly(false);
        // last 4 entries are not declared on the bean

        Truthness t = checkNotValid(bean);

        bean.setYear(tree(1920,790,185,33554950)); //1900 - 2200
        t = checkBetterThan(bean, t);

        bean.setYear(tree(1920,790,185)); //1900 - 2200
        t = checkBetterThan(bean, t);

        bean.setYear(null); //1900 - 2200
        t = checkBetterThan(bean, t);

        bean.setMonth(tree(140));
        t = checkBetterThan(bean, t); //1-12

        bean.setMonth(tree(20));
        t = checkBetterThan(bean, t); //1-12

        bean.setMonth(tree(-2));
        t = checkBetterThan(bean, t); //1-12

        bean.setMonth(tree(5));
        t = checkBetterThan(bean, t); //1-12

        bean.setBidTypeId(tree("EM51643XYZ", "_EM_51644_XYZ_", "6o_QhU9oaMcd", "_EM_51645_XYZ_"));
        t = checkBetterThan(bean, t); // ^[a-zA-Z0-9]*$

        bean.setBidTypeId(tree("EM51643XYZ", "", "6o_QhU9oaMcd", "_EM_51645_XYZ_"));
        t = checkBetterThan(bean, t); // ^[a-zA-Z0-9]*$

        bean.setBidTypeId(tree("EM51643XYZ", "", "6o_QhU9oaMcd"));
        t = checkBetterThan(bean, t); // ^[a-zA-Z0-9]*$

        bean.setBidTypeId(tree("EM51643XYZ", "", "EM"));
        t = checkBetterThan(bean, t); // ^[a-zA-Z0-9]*$

        bean.setNotBidTypeId(null);
        checkBetterThan(bean, t);

        checkValid(bean);
    }

    private <T> Truthness checkBetterThan(T bean, Truthness previous){

        Truthness t = ValidatorHeuristics.computeTruthness(validator, bean);
        assertTrue(t.getOfTrue() > previous.getOfTrue());
        return t;
    }


    private <T> Truthness checkNotValid(T bean){

        Set<ConstraintViolation<T>> result = validator.validate(bean);
        assertTrue(result.size() > 0);

        Truthness t = ValidatorHeuristics.computeTruthness(validator, bean);
        assertFalse(t.isTrue());
        assertTrue(t.isFalse());

        return t;
    }

    private <T> void checkValid(T bean){

        Set<ConstraintViolation<T>> result = validator.validate(bean);
        assertEquals(0, result.size());

        Truthness t = ValidatorHeuristics.computeTruthness(validator, bean);
        assertTrue(t.isTrue());
        assertFalse(t.isFalse());
    }

    private <T> TreeSet<T> tree(T... data){
        return new TreeSet<T>(Arrays.asList(data));
    }
}
