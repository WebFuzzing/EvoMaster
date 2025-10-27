/**
 * From OCVN  https://github.com/devgateway/ocvn
 * MIT license
 */

package org.evomaster.testoldlibraries.heuristic.validator.ocvn;

import cz.jirutka.validator.collection.constraints.EachRange;
//import io.swagger.annotations.ApiModelProperty;

import java.util.TreeSet;

/**
 * @author mpostelnicu
 *
 */
public class YearFilterPagingRequest extends DefaultFilterPagingRequest {

//    @ApiModelProperty(value = "This parameter will filter the content based on year. " + "The minimum year allowed is "
//            + MIN_REQ_YEAR + " and the maximum allowed is " + MAX_REQ_YEAR
//            + ".It will check if the startDate and endDate are within the year range. "
//            + "To check which fields are used to read start/endDate from, have a look at each endpoint definition.")
    @EachRange(min = MIN_REQ_YEAR, max = MAX_REQ_YEAR)
    protected TreeSet<Integer> year;

//    @ApiModelProperty(value = "This parameter will filter the content based on month. "
//            + "The minimum month allowed is "
//            + MIN_MONTH + " and the maximum allowed is " + MAX_MONTH
//            + "This parameter does nothing if used without the year parameter, as filtering and aggregating by month "
//            + "makes no sense without filtering by year. This parameter is also ignored when using multiple year "
//            + "parameters, so it only works if and only if the year parameter has one value.")
    @EachRange(min = MIN_MONTH, max = MAX_MONTH)
    protected TreeSet<Integer> month;
//    @ApiModelProperty(value = "When true, this parameter will add an extra layer of monthly grouping of all results."
//            + " The default is false")
    private Boolean monthly = false;

    public Boolean getMonthly() {
        return monthly;
    }

    public void setMonthly(Boolean monthly) {
        this.monthly = monthly;
    }

    /**
     *
     */
    public YearFilterPagingRequest() {
        super();
    }

    public TreeSet<Integer> getYear() {
        return year;
    }

    public void setYear(final TreeSet<Integer> year) {
        this.year = year;
    }

    public TreeSet<Integer> getMonth() {
        return month;
    }

    public void setMonth(TreeSet<Integer> month) {
        this.month = month;
    }
}
