Feature: Load Balancer operations

Background: No ligical switch exist
	Given there not exist any logical switch
	And there not exist any load balancer

Scenario: Create Load balancer
	When I create a logical switch with name "switch"
	And I create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080,192.168.1.2:8080" attached to logical switch "switch"
	Then I check that exist a load balancer with name "lb1:tcp"
	When I get the load balancer with name "lb1:tcp" as "#lb"
	Then I check that load balancer "#lb" has protocol "TCP"
	And I check that load balancer "#lb" has 1 virtual ips
	And I check that load balancer "#lb" contains virtual ip "10.0.0.1:8080" with targets "192.168.1.1:8080,192.168.1.2:8080"
	When I get the logical switch with name "switch" as "#ls"
	Then I check that logical switch "#ls" has 1 load balancer
	And I check that logical switch "#ls" contains load balancer "#lb"
	
Scenario: Create Load balancer with no ports
	When I create a logical switch with name "switch"
	And I create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1", targets "192.168.1.1,192.168.1.2" attached to logical switch "switch"
	Then I check that exist a load balancer with name "lb1:tcp"
	When I get the load balancer with name "lb1:tcp" as "#lb"
	Then I check that load balancer "#lb" has protocol "TCP"
	And I check that load balancer "#lb" has 1 virtual ips
	And I check that load balancer "#lb" contains virtual ip "10.0.0.1" with targets "192.168.1.1,192.168.1.2"
	When I get the logical switch with name "switch" as "#ls"
	Then I check that logical switch "#ls" has 1 load balancer
	And I check that logical switch "#ls" contains load balancer "#lb"
	
Scenario: Create Load balancer with same name should add vip to the existing load balancer
	When I create a logical switch with name "switch"
	And I create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080,192.168.1.2:8080" attached to logical switch "switch"
	Then I check that exist a load balancer with name "lb1:tcp"
	When I get the load balancer with name "lb1:tcp" as "#lb"
	Then I check that load balancer "#lb" has 1 virtual ips
	When I create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.2:8080", targets "192.168.2.1:8080,192.168.2.2:8080" attached to logical switch "switch"
	Then I check that exist a load balancer with name "lb1:tcp"
	When I get the load balancer with name "lb1:tcp" as "#lb"
	Then I check that load balancer "#lb" has 2 virtual ips
	And I check that load balancer "#lb" contains virtual ip "10.0.0.1:8080" with targets "192.168.1.1:8080,192.168.1.2:8080"
	And I check that load balancer "#lb" contains virtual ip "10.0.0.2:8080" with targets "192.168.2.1:8080,192.168.2.2:8080"
	When I get the logical switch with name "switch" as "#ls"
	Then I check that logical switch "#ls" has 1 load balancer
	And I check that logical switch "#ls" contains load balancer "#lb"

Scenario: Create 2 Load balancers concurrently with same name
	When I create a logical switch with name "switch"
	And I asynchronously create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080,192.168.1.2:8080" attached to logical switch "switch" getting "#promise1"
	And I asynchronously create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.2:8080", targets "192.168.2.1:8080,192.168.2.2:8080" attached to logical switch "switch" getting "#promise2"
	Then I check that promises "#promise1,#promise2" are success
	And I check that exist a load balancer with name "lb1:tcp"
	When I get the load balancer with name "lb1:tcp" as "#lb"
	Then I check that load balancer "#lb" has 2 virtual ips
	And I check that load balancer "#lb" contains virtual ip "10.0.0.1:8080" with targets "192.168.1.1:8080,192.168.1.2:8080"
	And I check that load balancer "#lb" contains virtual ip "10.0.0.2:8080" with targets "192.168.2.1:8080,192.168.2.2:8080"
	When I get the logical switch with name "switch" as "#ls"
	Then I check that logical switch "#ls" has 1 load balancer
	And I check that logical switch "#ls" contains load balancer "#lb"
	
Scenario: Create 2 Load balancers concurrently with same name, same virtual ip but different targets
	When I create a logical switch with name "switch"
	And I asynchronously create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080,192.168.1.2:8080" attached to logical switch "switch" getting "#promise1"
	And I asynchronously create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.2.1:8080,192.168.2.2:8080" attached to logical switch "switch" getting "#promise2"
	Then I check that promises "#promise1,#promise2" are success
	And I check that exist a load balancer with name "lb1:tcp"
	When I get the load balancer with name "lb1:tcp" as "#lb"
	Then I check that load balancer "#lb" has 1 virtual ips
	And I check that load balancer "#lb" contains virtual ip "10.0.0.1:8080" with targets "192.168.2.1:8080,192.168.2.2:8080"
	When I get the logical switch with name "switch" as "#ls"
	Then I check that logical switch "#ls" has 1 load balancer
	And I check that logical switch "#ls" contains load balancer "#lb"
	
Scenario: Create 2 Load balancers concurrently with same name, same virtual ip but different targets with different count
	When I create a logical switch with name "switch"
	And I asynchronously create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080,192.168.1.2:8080" attached to logical switch "switch" getting "#promise1"
	And I asynchronously create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.2.1:8080" attached to logical switch "switch" getting "#promise2"
	Then I check that promises "#promise1,#promise2" are success
	And I check that exist a load balancer with name "lb1:tcp"
	When I get the load balancer with name "lb1:tcp" as "#lb"
	Then I check that load balancer "#lb" has 1 virtual ips
	And I check that load balancer "#lb" contains virtual ip "10.0.0.1:8080" with targets "192.168.2.1:8080"
	When I get the logical switch with name "switch" as "#ls"
	Then I check that logical switch "#ls" has 1 load balancer
	And I check that logical switch "#ls" contains load balancer "#lb"
	
Scenario: Create 2 Load balancers concurrently with same name, same virtual ip and same targets
	When I create a logical switch with name "switch"
	And I asynchronously create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080,192.168.1.2:8080" attached to logical switch "switch" getting "#promise1"
	And I asynchronously create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080,192.168.1.2:8080" attached to logical switch "switch" getting "#promise2"
	Then I check that promises "#promise1,#promise2" are success
	And I check that exist a load balancer with name "lb1:tcp"
	When I get the load balancer with name "lb1:tcp" as "#lb"
	Then I check that load balancer "#lb" has 1 virtual ips
	And I check that load balancer "#lb" contains virtual ip "10.0.0.1:8080" with targets "192.168.1.1:8080,192.168.1.2:8080"
	When I get the logical switch with name "switch" as "#ls"
	Then I check that logical switch "#ls" has 1 load balancer
	And I check that logical switch "#ls" contains load balancer "#lb"
	
Scenario: Delete Load balancer virtual ip 
	When I create a logical switch with name "switch"
	And I create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080,192.168.1.2:8080" attached to logical switch "switch"
	And I create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.2:8080", targets "192.168.2.1:8080,192.168.2.2:8080" attached to logical switch "switch"
	Then I check that exist a load balancer with name "lb1:tcp"
	When I get the load balancer with name "lb1:tcp" as "#lb"
	Then I check that load balancer "#lb" has 2 virtual ips
	And I check that load balancer "#lb" contains virtual ip "10.0.0.1:8080" with targets "192.168.1.1:8080,192.168.1.2:8080"
	And I check that load balancer "#lb" contains virtual ip "10.0.0.2:8080" with targets "192.168.2.1:8080,192.168.2.2:8080"
	When I delete "TCP" virtual ip "10.0.0.2:8080" of load balancer "lb1"
	When I get the load balancer with name "lb1:tcp" as "#lb"
	Then I check that load balancer "#lb" has 1 virtual ips
	And I check that load balancer "#lb" contains virtual ip "10.0.0.1:8080" with targets "192.168.1.1:8080,192.168.1.2:8080"
	
Scenario: Delete Load balancer by created name
	When I create a logical switch with name "switch"
	And I create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080,192.168.1.2:8080" attached to logical switch "switch"
	Then I check that exist a load balancer with name "lb1:tcp"
	When I delete load balancer with name "lb1:tcp" with forced option
	Then I check that does not exist load balancer with name "lb1:tcp"
	
Scenario: Delete Load balancer by name and protocol
	When I create a logical switch with name "switch"
	And I create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080,192.168.1.2:8080" attached to logical switch "switch"
	Then I check that exist a load balancer with name "lb1:tcp"
	When I delete "TCP" load balancer with name "lb1" with forced option
	Then I check that does not exist load balancer with name "lb1:tcp"
	
Scenario: Delete unexisting Load balancer
	And I create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080,192.168.1.2:8080" attached to logical switch "switch"
	Then I check that exist a load balancer with name "lb1:tcp"
	When I delete load balancer with name "lb2:tcp"
	Then I check that exist a load balancer with name "lb1:tcp"
	
Scenario: Delete multiple load balancers
	When I create a logical switch with name "switch"
	And I create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080,192.168.1.2:8080" and external ids "testing=true" attached to logical switch "switch"
	Then I check that exist a load balancer with name "lb1:tcp"
	When I create a "TCP" load balancer with name "lb2", virtual ip "10.0.0.2:8080", targets "192.168.2.1:8080,192.168.2.2:8080" and external ids "testing=true" attached to logical switch "switch"
	Then I check that exist a load balancer with name "lb1:tcp"
	When I delete load balancers with external ids "testing=true" with forced option
	Then I check that exist 0 load balancers
	
Scenario: Get Load balancer by name and protocol
	When I create a logical switch with name "switch"
	And I create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080,192.168.1.2:8080" attached to logical switch "switch"
	Then I check that exist a "TCP" load balancer with name "lb1"
	
Scenario: Delete load balancer without force oprion
	When I create a logical switch with name "switch"
	And I create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080,192.168.1.2:8080" attached to logical switch "switch"
	Then I check that exist a load balancer with name "lb1:tcp"
	When I delete load balancer with name "lb1:tcp" getting "#promise"
	Then I check that promise "#promise" is not success
	And I check that promise "#promise" contains ovsdb error "referential integrity violation"
