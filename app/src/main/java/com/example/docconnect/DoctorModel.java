package com.example.docconnect;

public class DoctorModel {

    public String id;
    public String fullName;
    public String gender;
    public String dob;
    public String speciality;
    public String clinicName;
    public String clinicAddress;
    public String regNo;
    public String council;
    public String profileImageUrl;
    public String licenseImageUrl;
    public String role;
    public boolean isProfileCompleted;
    public String status; // pending / verified / rejected

    // Required empty constructor for Firebase
    public DoctorModel() {}

    public DoctorModel(
            String id,
            String fullName,
            String gender,
            String dob,
            String speciality,
            String clinicName,
            String clinicAddress,
            String regNo,
            String council,
            String profileImageUrl,
            String licenseImageUrl
    ) {
        this.id = id;
        this.fullName = fullName;
        this.gender = gender;
        this.dob = dob;
        this.speciality = speciality;
        this.clinicName = clinicName;
        this.clinicAddress = clinicAddress;
        this.regNo = regNo;
        this.council = council;
        this.profileImageUrl = profileImageUrl;
        this.licenseImageUrl = licenseImageUrl;

        this.role = "doctor";
        this.status = "pending"; // admin will verify
        this.isProfileCompleted = true;
    }
}
