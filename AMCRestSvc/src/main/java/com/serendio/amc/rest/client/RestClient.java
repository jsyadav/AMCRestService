package com.serendio.amc.rest.client;



import org.springframework.web.client.RestTemplate;

public class RestClient {

    public static void main(String args[]) {
        RestTemplate restTemplate = new RestTemplate();
        String jobId = "8987";
        restTemplate.postForObject("http://localhost:8080/amc/upload", jobId, String.class);
    }
    

}