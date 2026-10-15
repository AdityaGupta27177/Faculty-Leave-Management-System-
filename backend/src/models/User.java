package models;

/**
 * User model — represents a faculty member, HOD, or admin.
 * Demonstrates OOP: encapsulation with private fields and public getters/setters.
 */
public class User {
    private int id;
    private String googleId;
    private String email;
    private String name;
    private String profilePicture;
    private int departmentId;
    private String departmentName; // joined field
    private String designation;
    private String role; // FACULTY, HOD, ADMIN
    private int approverId;
    private String approverName; // joined field
    private boolean active;
    private boolean approved;
    private String createdAt;
    private String updatedAt;

    public User() {}

    // --- Getters and Setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

    public int getDepartmentId() { return departmentId; }
    public void setDepartmentId(int departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public int getApproverId() { return approverId; }
    public void setApproverId(int approverId) { this.approverId = approverId; }

    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Check if the user has a specific role.
     * Demonstrates polymorphism-ready design.
     */
    public boolean hasRole(String checkRole) {
        return this.role != null && this.role.equals(checkRole);
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", email='" + email + "', role='" + role + "'}";
    }
}
