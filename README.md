# Harmony ALM Rest Integration
Innominds Harmony Framework Integration with HP ALM with through REST API.

1. src/main/resources has mapping.csv file. This file has two columns viz,AutomationID and ALMTestID.
2. Harmony Framework will utilize this mapping.csv file.
3. During Automation Test Execution, Harmony Framework will have a test status pass or failure based on actual tests being run.
4. Let's say the current Automation test under execution is a LoginTest.
5. Considering LoginTest went through successfully, we lookup its corresponding ALMID in the mapping.csv file.
6. The next step is to perform ALM authentication and update ALMID with its run details.

Mandatory steps in ALM:
1. An individual Test in Test Plan should be mapped to corresponding Automation Test in Harmony. Make a note of this TestID and utilize it in the mapping.csv
2. All Tests which need automation Run status updated in ALM should be mapped to some Test Set in Test Lab.
