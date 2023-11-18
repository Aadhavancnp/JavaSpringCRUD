package com.example.JavaCRUD.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Customer {
    @JsonProperty("uuid")
    private String uuid;
    @JsonProperty("first_name")
    @NotNull
    private String firstName;
    @JsonProperty("last_name")
    @NotNull
    private String lastName;
    @JsonProperty("street")
    private String street;
    @JsonProperty("address")
    private String address;
    @JsonProperty("city")
    private String city;
    @JsonProperty("state")
    private String state;
    @JsonProperty("email")
    private String email;
    @JsonProperty("phone")
    private String phone;
}
