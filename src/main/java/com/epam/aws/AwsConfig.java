package com.epam.aws;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;

public class AwsConfig {

    private static String accessKey = System.getenv("ACCESS_KEY");
    private static String secretKey = System.getenv("SECRET_KEY");

    public static AWSStaticCredentialsProvider getCredentialsProvider() {
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        return new AWSStaticCredentialsProvider(credentials);
    }

    public static Regions getRegion() {
        return Regions.US_EAST_1;
    }
}
