package models;

/**
 * LeaveApplication model — represents a leave request.
 */
public class LeaveApplication {
    private int id;
    private String applicationNo;
    private int userId;
    private String facultyName;      // joined
    private String departmentName;   // joined
    private int leaveTypeId;
    private String leaveTypeName;    // joined
    private String startDate;
    private String endDate;
    private boolean halfDayStart;
    private boolean halfDayEnd;
    private double totalDays;
    private String reason;
    private String supportingDoc;
    private String status;           // DRAFT, SUBMITTED, PENDING, APPROVED, REJECTED, CANCELLED, RETURNED
    private int approverId;
    private String approverName;     // joined
    private String approvalComments; // latest approval comment
    private String submittedAt;
    private String decidedAt;
    private String createdAt;
    private String updatedAt;

    public LeaveApplication() {}

    // --- Getters and Setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getApplicationNo() { return applicationNo; }
    public void setApplicationNo(String applicationNo) { this.applicationNo = applicationNo; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getFacultyName() { return facultyName; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public int getLeaveTypeId() { return leaveTypeId; }
    public void setLeaveTypeId(int leaveTypeId) { this.leaveTypeId = leaveTypeId; }

    public String getLeaveTypeName() { return leaveTypeName; }
    public void setLeaveTypeName(String leaveTypeName) { this.leaveTypeName = leaveTypeName; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public boolean isHalfDayStart() { return halfDayStart; }
    public void setHalfDayStart(boolean halfDayStart) { this.halfDayStart = halfDayStart; }

    public boolean isHalfDayEnd() { return halfDayEnd; }
    public void setHalfDayEnd(boolean halfDayEnd) { this.halfDayEnd = halfDayEnd; }

    public double getTotalDays() { return totalDays; }
    public void setTotalDays(double totalDays) { this.totalDays = totalDays; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getSupportingDoc() { return supportingDoc; }
    public void setSupportingDoc(String supportingDoc) { this.supportingDoc = supportingDoc; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getApproverId() { return approverId; }
    public void setApproverId(int approverId) { this.approverId = approverId; }

    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }

    public String getApprovalComments() { return approvalComments; }
    public void setApprovalComments(String comments) { this.approvalComments = comments; }

    public String getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(String submittedAt) { this.submittedAt = submittedAt; }

    public String getDecidedAt() { return decidedAt; }
    public void setDecidedAt(String decidedAt) { this.decidedAt = decidedAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Check if the application can be cancelled.
     */
    public boolean isCancellable() {
        return "SUBMITTED".equals(status) || "PENDING".equals(status);
    }

    /**
     * Check if the application can be edited.
     */
    public boolean isEditable() {
        return "DRAFT".equals(status) || "RETURNED".equals(status);
    }
}
