package com.camunda.bigbank.test_data;

import java.util.Map;

import org.json.JSONObject;

import java.util.HashMap;

public class InterestRateTestData {
    // Starting Variables
    private double oldHouseValue;
	private double oldHouseMortgage;
	private double savings;
	private double yearlyIncome; 
	private Boolean vip;
    private double requestedAmount;
    private double houseValue;
    private double age;
    private Boolean selfEmployment;
    private Boolean indefiniteContract;
    private double desiredInterestRatePeriod;
    private double baseRate;

    // Expected results
    private int soldHouseValue;
    private int personalValue;
    private String mortgageRisk;
    private String inclusiveMortgageRisk;
    private double interestRate;

    public InterestRateTestData(JSONObject json){
        try{
            oldHouseValue = json.getDouble("oldHouseValue");
            oldHouseMortgage = json.getDouble("oldHouseMortgage");
            savings = json.getDouble("savings");
            yearlyIncome = json.getDouble("yearlyIncome");
            vip = json.getBoolean("vip");
            requestedAmount = json.getDouble("requestedAmount");
            houseValue = json.getDouble("houseValue");
            age = json.getDouble("age");
            selfEmployment = json.getBoolean("selfEmployment");
            indefiniteContract = json.getBoolean("indefiniteContract");
            desiredInterestRatePeriod = json.getDouble("desiredInterestRatePeriod");
            baseRate = json.getDouble("baseRate");
            soldHouseValue = json.getInt("soldHouseValue");      
            personalValue = json.getInt("personalValue");      
            mortgageRisk = json.getString("mortgageRisk");      
            inclusiveMortgageRisk = json.getString("inclusiveMortgageRisk");      
            interestRate = json.getDouble("interestRate");   
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    /**
     * Create a map of all variables required to start an interest rate process
     * @return - a map of all variables required to start an interest rate process
     */
    public Map<String, Object> startingVariablesToMap(){
        Map<String, Object> classedMap = new HashMap<String, Object>();
        classedMap.put("oldHouseValue", oldHouseValue);
        classedMap.put("oldHouseMortgage", oldHouseMortgage);
        classedMap.put("savings", savings);
        classedMap.put("yearlyIncome", yearlyIncome);
        classedMap.put("vip", vip);
        classedMap.put("requestedAmount", requestedAmount);
        classedMap.put("houseValue", houseValue);
        classedMap.put("age", age);
        classedMap.put("selfEmployment", selfEmployment);
        classedMap.put("indefiniteContract", indefiniteContract);
        classedMap.put("desiredInterestRatePeriod", desiredInterestRatePeriod);
        classedMap.put("baseRate", baseRate);
        return classedMap;
    }

    /**
     * Create a map of all variables that dictate the expected outcomes from the tests.
     * @return - a map of all variables that dictate the expected outcomes from the tests.
     */
    public Map<String,Object> expectedResultsToMap(){
        Map<String, Object> classedMap = new HashMap<String, Object>();
        classedMap.put("soldHouseValue", soldHouseValue);
        classedMap.put("personalValue", personalValue);
        classedMap.put("mortgageRisk", mortgageRisk);
        classedMap.put("inclusiveMortgageRisk", inclusiveMortgageRisk);
        classedMap.put("interestRate", interestRate);
        return classedMap;
    }
}
