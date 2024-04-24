package com.epam.aws;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CsvFileRequestHandler implements RequestHandler<S3Event, String> {

    private static final String S3_BUCKET_NAME = "galina-pylyptiy-bucket";
    private static final String DYNAMODB_TABLE_NAME = "training-report";

    private AmazonDynamoDB dynamoDB;
    private AmazonS3 s3Client;

    public CsvFileRequestHandler() {

        this.dynamoDB = AmazonDynamoDBClientBuilder.standard()
                .withCredentials(AwsConfig.getCredentialsProvider())
                .withRegion(AwsConfig.getRegion())
                .build();

        this.s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(AwsConfig.getCredentialsProvider())
                .withRegion(AwsConfig.getRegion())
                .build();
    }

    @Override
    public String handleRequest(S3Event event, Context context) {


        LocalDate currentDate = LocalDate.now();
        String reportName = "Trainers_Trainings_summary_" + currentDate.getYear() + "_" + currentDate.getMonthValue() + ".csv";

        List<String> reportData = new ArrayList<>();
        reportData.add("Trainer First Name,Trainer Last Name,Training Duration Summary");


        try {
            ScanRequest scanRequest = new ScanRequest().withTableName(DYNAMODB_TABLE_NAME);
            ScanResult result = dynamoDB.scan(scanRequest);
            List<Map<String, AttributeValue>> items = result.getItems();
            for (Map<String, AttributeValue> item : items) {
                String trainerFirstName = item.get("Trainer First Name").getS();
                String trainerLastName = item.get("Trainer Last Name").getS();
                int trainingDurationSummary = getSummaryDuration(item);
                reportData.add(trainerFirstName + "," + trainerLastName + "," + trainingDurationSummary);
            }
                String csvContent = String.join("\n", reportData);
                byte[] contentBytes = csvContent.getBytes();
                InputStream inputStream = new ByteArrayInputStream(contentBytes);
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(contentBytes.length);

                s3Client.putObject(new PutObjectRequest(S3_BUCKET_NAME, reportName, inputStream, metadata));

                context.getLogger().log("Report generated and uploaded to S3.");
            } catch(Exception e){
                context.getLogger().log("Error: " + e.getMessage());
            }
            return "Report generated and uploaded to S3: " + String.join("\n", reportData);
        }


    private int getSummaryDuration (Map<String, AttributeValue> item) {
        LocalDate currentDate = LocalDate.now();
            List<AttributeValue> periodDurationList = item.get("period_duration").getL();
            for (AttributeValue periodDuration: periodDurationList){
                Map<String, AttributeValue> yearMonths = periodDuration.getM();
                int year = Integer.parseInt(yearMonths.get("year").getN());
                if(year == currentDate.getYear()){
                    List<AttributeValue> months = yearMonths.get("months").getL();
                    for(AttributeValue month: months){
                        Map<String, AttributeValue> monthDuration = month.getM();
                        String monthName = monthDuration.get("name").getS();
                        if(monthName.equals(currentDate.getMonth().name())) {
                           return Integer.parseInt(monthDuration.get("summaryDuration").getN());
                        }
                    }
                }
            }
            return 0;
        }
    }



