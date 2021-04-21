package com.cloud.textract.handler;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.cloud.textract.service.AnalyzeDocument;

public class LambdaJavaAPI implements RequestHandler<Map<String, Object>, GatewayResponse> {

    @Override
    public GatewayResponse handleRequest(Map<String, Object> object, Context context) {

    	System.out.println("In Method");
        String message = "Hello World";
        
        Map<String, String> queryParams = (Map<String, String>) object.get("queryStringParameters");
	    String fileName = queryParams.get("fileName");

	    Date date = new Date();
	    String modifiedDate= new SimpleDateFormat("yyyy/MM/dd").format(date);
	    fileName = modifiedDate+"/"+fileName;
	    System.out.println("Request fileName "+fileName);
        
        System.out.println(message);
        try {
			AnalyzeDocument analyzer = new AnalyzeDocument();
			message = analyzer.getOutput(fileName).toString();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        GatewayResponse response = new GatewayResponse(
                message,
                200,
                Collections.singletonMap("X-Powered-By", "Cloud Marvericks"),
                false
        );
        Map headers = new HashMap<String, String>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Headers", "*");
        headers.put("Access-Control-Allow-Methods", "*");
        response.setHeaders(headers);;
        return response;
    }
}