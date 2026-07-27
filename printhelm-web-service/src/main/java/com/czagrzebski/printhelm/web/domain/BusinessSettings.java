package com.czagrzebski.printhelm.web.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "business_settings")
public class BusinessSettings {

    @Id
    @Column(name = "id")
    private Long id = 1L;

    @Column(name = "business_name", length = 200)
    private String businessName;

    @Column(name = "business_address", length = 500)
    private String businessAddress;

    @Column(name = "business_email", length = 200)
    private String businessEmail;

    @Column(name = "business_phone", length = 100)
    private String businessPhone;

    public Long getId() { return id; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getBusinessAddress() { return businessAddress; }
    public void setBusinessAddress(String businessAddress) { this.businessAddress = businessAddress; }

    public String getBusinessEmail() { return businessEmail; }
    public void setBusinessEmail(String businessEmail) { this.businessEmail = businessEmail; }

    public String getBusinessPhone() { return businessPhone; }
    public void setBusinessPhone(String businessPhone) { this.businessPhone = businessPhone; }
}
