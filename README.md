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
recipe must have
- name, 
- id, 
- list of ingredients (minimum 0?), 
- preparation steps/instructions,
- number of servings
ingredient must have a 
- name, 
- quantity + unit of measure,
- remark (optional)

minimal table structure could be: 1) recipe table, linking to 2) ingredients table (FK=recipe.id)
fancier would be: 1) recipe table, 2) ingredient_type table, 3) recipe_ingredient coupling table (FK1 = recipe.id, FK2= ingredient_type.id)
this might pose some issues with typo's, spelling differences, and/or letter-case usage/capitalization.

API interfaces
- recipes need to saved/fetched (CRUD); new = without ID, save/update/delete by ID
- search results may contain parts of recipe info, navigate to full recipe by ID.



