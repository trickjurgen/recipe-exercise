# recipe-exercise
standalone app to store and query recipes

## Objective
Create a standalone java application which allows users to manage their favourite recipes. It should
allow adding, updating, removing and fetching recipes. Additionally users should be able to filter
available recipes based on one or more of the following **criteria**:
1. Whether or not the dish is vegetarian
2. The number of servings
3. Specific ingredients (either include or exclude)
4. Text search within the instructions.

For example, the API should be able to handle the following **search requests**:
- All vegetarian recipes
- Recipes that can serve 4 persons and have “potatoes” as an ingredient
- Recipes without “salmon” as an ingredient that has “oven” in the instructions.

## Requirements
Please ensure that there is some documentation about: 
- the architectural choices
- how to run the application. 

All these requirements needs to be satisfied:
1. It must be a REST application implemented using Java (use a framework of your choice)
2. REST API must be documented
3. Data must be persisted in a database
4. Unit tests must be present
5. Integration tests must be present

---

# Strategy
## Recipe Structure

recipe must have flag to indicate vegetarian

recipe must have :
- name, 
- id, 
- list of ingredients (minimum 0?), 
- preparation steps/instructions,
- number of servings

ingredient must have a :
- name, 
- quantity + unit of measure,
- remark (optional)

minimal table structure could be: 

1) recipe table, linking to 2) ingredients table (FK=recipe.id)

fancier would be: 

1) recipe table, 2) ingredient_type table, 3) recipe_ingredient coupling table (FK1 = recipe.id, FK2= ingredient_type.id)
this might pose some issues with typo's, spelling differences, and/or letter-case usage/capitalization.

## API

API interfaces:
- recipes need to be saved/fetched (CRUD); new = without ID, save/update/delete by ID
- search results may contain either parts of recipe info or full recipes, from that data you can navigate to full recipe by ID.

# Implementation

## DB

Initial setup with H2 (in memory) database. This is easy to initiate and has no issues with a changing datamodel during development.
Later replace this with a MySql instance, Script a docker compose to run it without actual install.
Keep the H2 for testing purposes.

Went for the 'complexer' data model where ingredients (name) are separate entities that are reused across recipes.
This might aid in searching later but it adds complexity in storing and updating entities in general.

## Example / test data

After creating entity objects (from strategy plan above) (and repository interfaces) create a few json files that match the entities.
Use unit tests to map json files to entities and attempt to save to in memory database.
Using this tested json set as example create a batch of data files to use in testing.

## Code

Used a bottom-up approach. 
- data layer
	- first define the data classes
	- define test data
	- create db interfaces
	- validate db functionality with (layer) integration test
- service layer
	- mappers between storage and communication entities
	- unit test mappers
	- create service layer
	- unit test service layer with mocks and testdata
	- update documentation

*currently implementation is about here*

- Interface layer
	- create API (controller/rest/endpoint) layer
	- test controller(s)
	- OpenAPI documentation
	- update documentation
- Monitoring
	- create actuators
	- test actuators
- stability
	- multi-layer integration test?
- external access
	- UI or postman scripts or .. ?

## Nice to haves / experiments?

- use an external service for data?
	- wiremock test for external svc?
- add a SOAP controller (same as json api, but gives xml)
- use testcontainers in integrationtest

## TODO's

- instructions for running db (docker compose)
- instructions for running app (command line; or also in a docker?)
- instructions for generating api doc
	- with maven command in target
	- from running app

