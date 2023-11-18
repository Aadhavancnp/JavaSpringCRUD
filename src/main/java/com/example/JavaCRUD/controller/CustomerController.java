package com.example.JavaCRUD.controller;

import com.example.JavaCRUD.dto.CustomerDTO;
import com.example.JavaCRUD.models.Customer;
import com.example.JavaCRUD.models.Login;
import com.example.JavaCRUD.models.Response;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class CustomerController {
    private static final String URL = "https://qa2.sunbasedata.com/sunbase/portal/api/assignment.jsp";
    private static final String AUTH_URL = "https://qa2.sunbasedata.com/sunbase/portal/api/assignment_auth.jsp";

    @Autowired
    private WebClient client;

    @GetMapping("/login")
    public String loginForm(Model model) {
        model.addAttribute("login", new Login());
        return "login";
    }

    @PostMapping("/login")
    public String loginSubmit(@Valid @ModelAttribute("login") Login login, BindingResult bindingResult, Model model, HttpServletResponse response) throws JsonProcessingException {
        if (bindingResult.hasErrors()) {
            model.addAttribute("login", login);
            return "login";
        }
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("login_id", login.getLoginId());
        body.add("password", login.getPassword());
        Mono<String> responseMono = client.post().uri(AUTH_URL).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).bodyValue(body).retrieve().bodyToMono(String.class);
        String responseString = responseMono.block();
        Response response_dto = objectMapper(responseString, Response.class);
        setCookie(response, "token", response_dto.getAccess_token());
        return "redirect:/";
    }


    @GetMapping("/")
    public String index(Model model, HttpServletRequest request) throws JsonProcessingException {
        Mono<Object[]> responseMono = client.get().uri(URL + "?cmd=get_customer_list").header("Authorization", "Bearer " + getCookie(request, "token")).accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(Object[].class);
        Object[] responseList = responseMono.block();
        List<Customer> customers = objectMapperList(responseList, Customer.class);
        model.addAttribute("customers", customers);
        return "index";
    }

    @GetMapping("/customer/add")
    public String customerAdd(Model model) {
        model.addAttribute("customer", new CustomerDTO());
        return "customer-details-add";
    }

    @PostMapping("/customer/add")
    public String customerAddSubmit(@Valid @ModelAttribute("customer") CustomerDTO customerDto, BindingResult bindingResult, Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("customer", customerDto);
            return "customer-details-add";
        }
        HashMap<String, String> body = new HashMap<>();
        body.put("first_name", customerDto.getFirstName());
        body.put("last_name", customerDto.getLastName());
        body.put("street", customerDto.getStreet());
        body.put("address", customerDto.getAddress());
        body.put("city", customerDto.getCity());
        body.put("state", customerDto.getState());
        body.put("email", customerDto.getEmail());
        body.put("phone", customerDto.getPhone());

        Mono<String> responseMono = client.post().uri(URL + "?cmd=create").header("Authorization", "Bearer " + getCookie(request, "token")).contentType(MediaType.APPLICATION_JSON).bodyValue(body).retrieve().bodyToMono(String.class);
        String response = responseMono.block();
        redirectAttributes.addFlashAttribute("message", "Customer added successfully");
        return "redirect:/";
    }

    @GetMapping("/customer/{uuid}/edit")
    public String customerEdit(@PathVariable("uuid") String uuid, Model model, HttpServletRequest request) throws JsonProcessingException {
        Mono<Object[]> responseMono = client.get().uri(URL + "?cmd=get_customer_list").header("Authorization", "Bearer " + getCookie(request, "token")).accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(Object[].class);
        Object[] responseList = responseMono.block();
        List<Customer> customers = objectMapperList(responseList, Customer.class);
        Customer customer = customers.stream().filter(c -> c.getUuid().equals(uuid)).findFirst().get();
        model.addAttribute("customer", customer);
        return "customer-details-edit";
    }

    @PostMapping("/customer/{uuid}/edit")
    public String customerEditSubmit(@Valid @ModelAttribute("customer") Customer customer, BindingResult bindingResult, Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("customer", customer);
            return "customer-details-edit";
        }
        HashMap<String, String> body = new HashMap<>();
        body.put("first_name", customer.getFirstName());
        body.put("last_name", customer.getLastName());
        body.put("street", customer.getStreet());
        body.put("address", customer.getAddress());
        body.put("city", customer.getCity());
        body.put("state", customer.getState());
        body.put("email", customer.getEmail());
        body.put("phone", customer.getPhone());

        Mono<String> responseMono = client.post().uri(URL + "?cmd=update&uuid=" + customer.getUuid()).header("Authorization", "Bearer " + getCookie(request, "token")).contentType(MediaType.APPLICATION_JSON).bodyValue(body).retrieve().bodyToMono(String.class);
        String response = responseMono.block();
        redirectAttributes.addFlashAttribute("message", "Customer updated successfully");
        return "redirect:/";
    }

    @GetMapping("/customer/{uuid}/delete")
    public String customerDelete(@PathVariable("uuid") String uuid, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        Mono<String> response = client.post().uri(URL + "?cmd=delete&uuid=" + uuid).header("Authorization", "Bearer " + getCookie(request, "token")).retrieve().bodyToMono(String.class);
        String result = response.block();
        redirectAttributes.addFlashAttribute("message", "Customer deleted successfully");
        return "redirect:/";
    }


    private <T> T objectMapper(String json, Class<T> clazz) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, clazz);
    }

    private <T> List<T> objectMapperList(Object[] jsonList, Class<T> clazz) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return Arrays.stream(jsonList).map(object -> mapper.convertValue(object, clazz)).collect(Collectors.toList());
    }

    private void setCookie(HttpServletResponse response, String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setSecure(true);
        response.addCookie(cookie);
    }

    private String getCookie(HttpServletRequest request, String key) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(key)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }


}