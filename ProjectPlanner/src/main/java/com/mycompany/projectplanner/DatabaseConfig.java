package com.mycompany.projectplanner;

public class DatabaseConfig {

    public static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=PP_DB;encrypt=true;trustServerCertificate=true";
    public static final String DB_USER = "java_user"; 
    public static final String DB_PASSWORD = "12345@"; 
    
    // max connections setting and timeouts
    public static final int MAX_CONNECTIONS = 10;
    public static final int CONNECTION_TIMEOUT = 30000; 
}