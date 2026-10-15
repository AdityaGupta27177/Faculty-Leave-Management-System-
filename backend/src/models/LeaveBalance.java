package models;

/** LeaveBalance model — tracks how many days a user has for each leave type */
public class LeaveBalance {
    private int id;
    private int userId;
    private int leaveTypeId;
    private String leaveTypeName; // joined
    private String code;          // joined
    private String academicYear;
    private double totalEntitled;
    private double used;
    private double pending;
    private String createdAt;
    private String updatedAt;

    public LeaveBalance() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getLeaveTypeId() { return leaveTypeId; }
    public void setLeaveTypeId(int leaveTypeId) { this.leaveTypeId = leaveTypeId; }
    public String getLeaveTypeName() { return leaveTypeName; }
    public void setLeaveTypeName(String leaveTypeName) { this.leaveTypeName = leaveTypeName; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    public double getTotalEntitled() { return totalEntitled; }
    public void setTotalEntitled(double totalEntitled) { this.totalEntitled = totalEntitled; }
    public double getUsed() { return used; }
    public void setUsed(double used) { this.used = used; }
    public double getPending() { return pending; }
    public void setPending(double pending) { this.pending = pending; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    /** Calculate remaining leave */
    public double getRemaining() {
        return totalEntitled - used - pending;
    }
}
