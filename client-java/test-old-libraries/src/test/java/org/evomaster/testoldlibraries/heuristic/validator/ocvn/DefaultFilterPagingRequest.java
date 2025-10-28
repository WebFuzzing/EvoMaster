/**
 * From OCVN  https://github.com/devgateway/ocvn
 * MIT license
 */
package org.evomaster.testoldlibraries.heuristic.validator.ocvn;

import cz.jirutka.validator.collection.constraints.EachPattern;
//import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.util.TreeSet;

/**
 * @author mpostelnicu Filtering bean applied to all endpoints
 */
public class DefaultFilterPagingRequest extends GenericPagingRequest {

    @EachPattern(regexp = "^[a-zA-Z0-9]*$")
//    @ApiModelProperty(value = "This corresponds to the tender.items.classification._id")
    private TreeSet<String> bidTypeId;

    @EachPattern(regexp = "^[a-zA-Z0-9]*$")
//    @ApiModelProperty(value =
//            "This corresponds the negated bidTypeId filter, matches elements that are NOT in the TreeSet of Ids")
    private TreeSet<String> notBidTypeId;

    @EachPattern(regexp = "^[a-zA-Z0-9]*$")
//    @ApiModelProperty(value = "This is the id of the organization/procuring entity. "
//            + "Corresponds to the OCDS Organization.identifier")
    private TreeSet<String> procuringEntityId;

    @EachPattern(regexp = "^[a-zA-Z0-9]*$")
//    @ApiModelProperty(value = "This corresponds the negated procuringEntityId filter,"
//            + " matches elements that are NOT in the TreeSet of Ids")
    private TreeSet<String> notProcuringEntityId;

    // @EachPattern(regexp = "^[\\p{L}0-9]*$")
//    @ApiModelProperty(value = "This is the id of the organization/supplier entity. "
//            + "Corresponds to the OCDS Organization.identifier")
    private TreeSet<String> supplierId;

//    @ApiModelProperty(value = "This will filter after tender.procurementMethodDetails."
//            + "Valid examples are Đấu thầu rộng rãi, Đấu thầu hạn chế, etc...")
    private TreeSet<String> bidSelectionMethod;

//    @ApiModelProperty(value = "This corresponds the negated bidSelectionMethod filter,"
//            + " matches elements that are NOT in the list of Ids")
    private TreeSet<String> notBidSelectionMethod;

//    @ApiModelProperty(value = "This will filter after tender.contrMethod.id, Values range from 1 to 5.")
    @EachPattern(regexp = "^[a-zA-Z0-9]*$")
    private TreeSet<String> contrMethod;

//    @ApiModelProperty(value = "This will filter after planning.budget.projectLocation._id")
    private TreeSet<String> planningLoc;

//    @ApiModelProperty(value = "This will filter after tender.items.deliveryLocation._id")
    private TreeSet<String> tenderLoc;

//    @ApiModelProperty(value = "This will filter after tender.procurementMethod")
    private TreeSet<String> procurementMethod;

//    @ApiModelProperty(value = "This will filter after tender.value.amount and will specify a minimum"
//            + "Use /api/tenderValueInterval to get the minimum allowed.")
    private BigDecimal minTenderValue;

//    @ApiModelProperty(value = "This will filter after tender.value.amount and will specify a maximum."
//            + "Use /api/tenderValueInterval to get the maximum allowed.")
    private BigDecimal maxTenderValue;

//    @ApiModelProperty(value = "This will filter after awards.value.amount and will specify a minimum"
//            + "Use /api/awardValueInterval to get the minimum allowed.")
    private BigDecimal minAwardValue;

//    @ApiModelProperty(value = "This will filter after awards.value.amount and will specify a maximum."
//            + "Use /api/awardValueInterval to get the maximum allowed.")
    private BigDecimal maxAwardValue;

//    @ApiModelProperty(value = "This will search after the City Id of the procuring entity."
//            + "The field is organization.address.postalCode")
    private TreeSet<String> procuringEntityCityId;

//    @ApiModelProperty(value = "Filters after tender.submissionMethod='electronicSubmission', also known as"
//            + " eBids")
    private Boolean electronicSubmission;

//    @ApiModelProperty(value = "This will search after the DepartmentId of the procuring entity."
//            + "The field is organization.department._id")
    private TreeSet<Integer> procuringEntityDepartmentId;

//    @ApiModelProperty(value = "This will search after the DepartmentId of the procuring entity."
//            + "The field is organization.group._id")
    private TreeSet<Integer> procuringEntityGroupId;


//    @ApiModelProperty(value = "Only show the releases that were flagged by at least one indicator")
    private Boolean flagged;

    public DefaultFilterPagingRequest() {
        super();
    }

    public TreeSet<String> getBidTypeId() {
        return bidTypeId;
    }

    public void setBidTypeId(final TreeSet<String> bidTypeId) {
        this.bidTypeId = bidTypeId;
    }

    public TreeSet<String> getProcuringEntityId() {
        return procuringEntityId;
    }

    public void setProcuringEntityId(final TreeSet<String> procuringEntityId) {
        this.procuringEntityId = procuringEntityId;
    }

    public TreeSet<String> getBidSelectionMethod() {
        return bidSelectionMethod;
    }

    public void setBidSelectionMethod(final TreeSet<String> bidSelectionMethod) {
        this.bidSelectionMethod = bidSelectionMethod;
    }

    public TreeSet<String> getContrMethod() {
        return contrMethod;
    }

    public void setContrMethod(TreeSet<String> contrMethod) {
        this.contrMethod = contrMethod;
    }

    public TreeSet<String> getTenderLoc() {
        return tenderLoc;
    }

    public void setTenderLoc(final TreeSet<String> tenderDeliveryLocationGazetteerIdentifier) {
        this.tenderLoc = tenderDeliveryLocationGazetteerIdentifier;
    }

    public BigDecimal getMinTenderValue() {
        return minTenderValue;
    }

    public void setMinTenderValue(final BigDecimal minTenderValueAmount) {
        this.minTenderValue = minTenderValueAmount;
    }

    public BigDecimal getMaxTenderValue() {
        return maxTenderValue;
    }

    public void setMaxTenderValue(final BigDecimal maxTenderValueAmount) {
        this.maxTenderValue = maxTenderValueAmount;
    }

    public BigDecimal getMinAwardValue() {
        return minAwardValue;
    }

    public void setMinAwardValue(final BigDecimal minAwardValue) {
        this.minAwardValue = minAwardValue;
    }

    public BigDecimal getMaxAwardValue() {
        return maxAwardValue;
    }

    public void setMaxAwardValue(final BigDecimal maxAwardValue) {
        this.maxAwardValue = maxAwardValue;
    }

    public TreeSet<String> getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(final TreeSet<String> supplierId) {
        this.supplierId = supplierId;
    }

    public TreeSet<String> getNotBidTypeId() {
        return notBidTypeId;
    }

    public void setNotBidTypeId(TreeSet<String> notBidTypeId) {
        this.notBidTypeId = notBidTypeId;
    }

    public TreeSet<String> getNotProcuringEntityId() {
        return notProcuringEntityId;
    }

    public void setNotProcuringEntityId(TreeSet<String> notProcuringEntityId) {
        this.notProcuringEntityId = notProcuringEntityId;
    }

    public TreeSet<String> getProcuringEntityCityId() {
        return procuringEntityCityId;
    }
    public Boolean getElectronicSubmission() {
        return electronicSubmission;
    }

    public void setElectronicSubmission(Boolean electronicSubmission) {
        this.electronicSubmission = electronicSubmission;
    }

    public void setProcuringEntityCityId(TreeSet<String> procuringEntityCityId) {
        this.procuringEntityCityId = procuringEntityCityId;
    }

    public TreeSet<String> getProcurementMethod() {
        return procurementMethod;
    }

    public void setProcurementMethod(TreeSet<String> procurementMethod) {
        this.procurementMethod = procurementMethod;
    }

    public TreeSet<Integer> getProcuringEntityDepartmentId() {
        return procuringEntityDepartmentId;
    }

    public Boolean getFlagged() {
        return flagged;
    }

    public void setFlagged(Boolean flagged) {
        this.flagged = flagged;
    }


    public void setProcuringEntityDepartmentId(TreeSet<Integer> procuringEntityDepartmentId) {
        this.procuringEntityDepartmentId = procuringEntityDepartmentId;
    }

    public TreeSet<Integer> getProcuringEntityGroupId() {
        return procuringEntityGroupId;
    }

    public void setProcuringEntityGroupId(TreeSet<Integer> procuringEntityGroupId) {
        this.procuringEntityGroupId = procuringEntityGroupId;
    }

    public TreeSet<String> getNotBidSelectionMethod() {
        return notBidSelectionMethod;
    }

    public void setNotBidSelectionMethod(TreeSet<String> notBidSelectionMethod) {
        this.notBidSelectionMethod = notBidSelectionMethod;
    }

    public TreeSet<String> getPlanningLoc() {
        return planningLoc;
    }

    public void setPlanningLoc(TreeSet<String> planningLoc) {
        this.planningLoc = planningLoc;
    }
}