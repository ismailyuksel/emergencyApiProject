package com.emergency.api.controller;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.emergency.api.model.LocationModel;
import com.emergency.api.model.AccessTokenModel;
import com.emergency.api.model.FacilityModel;
import com.emergency.api.util.FileUtil;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

@Component
public class FacilityController {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private Gson gson;
	
	@Value("${file.name.anatolia}")
	private String fileNameAnatolia;

	@Value("${file.name.europe}")
	private String fileNameEurope;
	
	@Value("${emergency.url}")
	private String emergencyUrl;
	
	private static final int INITIAL_ID = 2001;
    private static final int MIN_DEMAND = 5;
    private static final int MAX_DEMAND = 100;
    private static final double MIN_INIT_COST = 5;
    private static final double MAX_INIT_COST = 100;
    
    
	public void addFacility() {
		
    	List<LocationModel> locationList = getLocationList();
		
    	if(locationList == null) {
    		return;
    	}
    	
    	List<FacilityModel> facilityList = getFacilityList(locationList);
    	
		String token = null;
		try {
			token = getToken();
		} catch (Exception e) {
			logger.error("token error", e);
			return;
		}
		
		if(token == null) {
			logger.error("empty token error");
			return;
		}
		
		String url = emergencyUrl + "/api/facilities";
		CloseableHttpClient httpClient = null;
		
		logger.info("facility count: " + facilityList.size());
		int successCount = 0;
		int errorCount = 0;
		for(FacilityModel facility : facilityList) {
			String requestData = gson.toJson(facility);
			logger.info("Request data: " + requestData);
	        try {
	            httpClient = HttpClients.createDefault();
	            HttpResponse response;
	            try {
	                HttpPost postRequest = new HttpPost(url);
	                StringEntity input = new StringEntity(requestData);
	                input.setContentType("application/json");
	                postRequest.setEntity(input);
	                postRequest.addHeader("Authorization", "Bearer " + token);

	                response = httpClient.execute(postRequest);
	            } catch (Exception e) {
	            	errorCount++;
	                logger.error("http post request error", e);
	                continue;
	            }
	            BufferedReader br = new BufferedReader(
	                    new InputStreamReader((response.getEntity().getContent())));

	            String output = null, line = null;
	            while ((line = br.readLine()) != null) {
	                output = (output == null) ? line : (output + "\n" + line);
	            }
	            String responseData = output;
	            logger.info("Response data: " + responseData);

	            if (response.getStatusLine().getStatusCode() != 200 && response.getStatusLine().getStatusCode() != 201) {
	            	errorCount++;
	            	logger.error("Failed : HTTP error code : "
		                    + response.getStatusLine().getStatusCode());
	            	continue;
	            }
	            successCount++;
	        } catch (Exception e) {
	        	errorCount++;
	            logger.error("Failed to add facility", e);
	            continue;
	        } finally {
	            if (httpClient != null) {
	                try {
	                	httpClient.close();
	                } catch (Exception e) {
	                }
	            }
	        }
		}
		logger.info("successCount : {} errorCount : {}" , successCount, errorCount);
	}

	private List<LocationModel> getLocationList() {
		
		String jsonAnatolia = null;
    	String jsonEurope = null;
    	
		try {
			jsonAnatolia = FileUtil.readStringFromFile(fileNameAnatolia);
			jsonEurope = FileUtil.readStringFromFile(fileNameEurope);
		} catch (IOException e) {
			logger.error("file read error", e);
			return null;
		}
		
		if(jsonAnatolia == null && jsonEurope == null) {
			logger.error("Empty file error");
			return null;
		}
		
		List<LocationModel> locationList = new ArrayList<LocationModel>();
		List<LocationModel> locationTemp = null;
		try {
			TypeToken<List<LocationModel>> token = new TypeToken<List<LocationModel>>(){};
			
			if(jsonAnatolia != null) {
				locationTemp = gson.fromJson(jsonAnatolia, token.getType());
				if(locationTemp != null && !locationTemp.isEmpty()) {
					locationList.addAll(locationTemp);
				}				
			}

			if(jsonEurope != null) {
				locationTemp = gson.fromJson(jsonEurope, token.getType());
				if(locationTemp != null && !locationTemp.isEmpty()) {
					locationList.addAll(locationTemp);
				}				
			}
			
		} catch (JsonSyntaxException e) {
			logger.error("json parse error", e);
			return null;
		}
		
		
		if(locationList.isEmpty()) {
			logger.error("empty location list");
			return null;
		}
		
		return locationList;
	}

	private List<FacilityModel> getFacilityList(List<LocationModel> locationList) {
		Random r = new Random();
		
		List<FacilityModel> facilityList = new ArrayList<>();
		
		int id = INITIAL_ID;
		for (LocationModel location : locationList) {
			
			FacilityModel facility = new FacilityModel();
			
			facility.setId(id);
			facility.setInitialCost(MIN_INIT_COST + (MAX_INIT_COST - MIN_INIT_COST) * r.nextDouble());
			facility.setLatitude(location.getLat());
			facility.setLongitude(location.getLon());
			facility.setSupply(r.nextInt((MAX_DEMAND - MIN_DEMAND) + 1) + MIN_DEMAND);
			
			facilityList.add(facility);
			
			id++;
		}
		
		return facilityList;
	}
	
    private String getToken() throws Exception {
        String url = emergencyUrl + "/api/authenticate";
        CloseableHttpClient httpClient= null;
        try {
            String requestData = "{\"username\": \"admin\", \"password\": \"admin\", \"rememberMe\": false}";
            StringEntity input = new StringEntity(requestData);
            input.setContentType("application/json");
            HttpPost postRequest = null;

            HttpResponse response;
            try {
                postRequest = new HttpPost(url);
                postRequest.setEntity(input);
                httpClient = HttpClients.createDefault();
                
                response = httpClient.execute(postRequest);
            } catch (Exception e) {
            	logger.error("token response has error", e);
                throw e;
            }
            if (response.getStatusLine().getStatusCode() != 200 && response.getStatusLine().getStatusCode() != 201) {
                throw new RuntimeException("Failed : HTTP error code : "
                    + response.getStatusLine().getStatusCode());
            }
            BufferedReader br = new BufferedReader(
                            new InputStreamReader((response.getEntity().getContent())));

            String output = null, line = null;
            while ((line = br.readLine()) != null) {
                output = (output == null) ? line : output + "\n" + line;
            }
            String responseData = output;
            logger.info(responseData);
            AccessTokenModel accessToken = gson.fromJson(output, AccessTokenModel.class);
            if (accessToken != null && accessToken.getAccessToken() != null &&
                    !accessToken.getAccessToken().isEmpty()) {
                return accessToken.getAccessToken();
            }
        } catch (Exception e) {
            logger.error("Failed to get token", e);
            throw e;
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (Exception e) {
                }
            }
        }
        return null;
    }
	
}
