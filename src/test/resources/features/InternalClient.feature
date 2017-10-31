Feature: Internal client

Scenario: Get schema
	When I get list of databases as "#dbs"
	Then I check that "#dbs" contains "OVN_Northbound"
	
Scenario: Concurrent operations should share connection
	When I close ovn northbound client
	And I get list of databases as "#dbsPromise1" no waiting result
	And I get list of databases as "#dbsPromise2" no waiting result
	Then I check that promise "#dbsPromise1" is success
	And I check that promise "#dbsPromise1" is success
	
Scenario: Client should reply to echo message
	When I get list of databases as "#dbs"
	Then I check that "#dbs" contains "OVN_Northbound"
	And I check that client has received an echo message

Scenario: Client should check connection before use it
	When I get list of databases as "#dbs"
	Then I check that "#dbs" contains "OVN_Northbound"
	When I get list of databases as "#dbs" after 6 seconds
	Then I check that "#dbs" contains "OVN_Northbound"