+++++++++++++++++++++++++++++++++++++
+ handover-service 1.2.0-SNAPSHOT
++++++++++++++++++++++++++++++++++++++
Flow:
1. Extract custom fields of visitor from request
2. Get Store ID from flow-core service
3. Get agents of store from user-service
    - curl -X GET "http://209.58.160.20:1201/clients/?storeId=8913d06f-a63f-4a16-8059-2a30a517663a&page=0&pageSize=20" -H "accept: */*" -H "Authorization: Bearer Bearer AccessToken"
4. Check which agents are online from rocketchat db
5. Reply with random available agents


++++++++++++++++++++++++++++++++
+ handover-service 1.1-SNAPSHOT
++++++++++++++++++++++++++++++++

Flow:
1. Handover service receives inbound messages from channel wrappers. 
2. It validates and verifies the incoming message and forwards message to live agents (if any one is available)
    - Creates visitor using rocket chat api
    - Creates room using rocket chat api
    - Queries storeId from flow-core API against referenceId
	- Queries agent information from user-service API against storeId
		Chooses random agent if store has >1 agent. If no agent is found, default agent will be used
    - send message for agent using rocket chat api
3. It receives outbound messages from live agent and forwards to respective wrapper (wrapper url is read from custom fields of RC api)
4. Dangling chat handling

++++++++++++++++++++++++++++++++
+ handover-service 0.0.1-SNAPSHOT
++++++++++++++++++++++++++++++++

Flow:
1. Handover service receives inbound messages from channel wrappers. 
2. It validates and verifies the incoming message and forwards message to live agents (if any one is available)
    - Creates visitor using rocket chat api
    - Creates room using rocket chat api
    - send message for agent using rocket chat api
3. It receives outbound messages from live agent and forwards to respective wrapper (wrapper url is read from custom fields of RC api)
4. It also periodically checks agents availability. Forwards offline message if supported by agents interface