package com.example.docconnect;

/**
 * PatientHistoryModel:  Model for detailed appointment history.
 */
public class PatientHistoryModel {
    private String doctorName;
    private String date;
    private String time;
    private String status;
    private String patientId; // Used for filtering
    private String key;

    // Required empty constructor for Firebase
    public PatientHistoryModel() {}

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
}