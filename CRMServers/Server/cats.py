import json
import requests
import uuid

def generate_unique_id():
    """Generates a random unique ID."""
    return uuid.uuid4().hex[:13]

def add_category(category_data, api_url):
    """Adds a category to the database via the API."""
    category_id = generate_unique_id()
    payload = {
        "id": category_id,
        "name": category_data["name"],
        "image": category_data["img"]
    }
    headers = {'Content-Type': 'application/json'}
    try:
        # response = requests.post(api_url, headers=headers, json=payload)
        # response.raise_for_status()  # Raise an exception for bad status codes
        print(f"Category '{category_data['name']}' image:{category_data["img"]}")
        return category_id
    except requests.exceptions.RequestException as e:
        print(f"Error adding category '{category_data['name']}': {e}")
        return None

def add_subcategory(subcategory_data, parent_id, api_url):
    """Adds a subcategory to the database via the API."""
    subcategory_id = generate_unique_id()
    payload = {
        "id": subcategory_id,
        "parentid": parent_id,
        "name": subcategory_data["name"],
        "image": subcategory_data["img"]
    }
    headers = {'Content-Type': 'application/json'}
    try:
        # response = requests.post(api_url, headers=headers, json=payload)
        # response.raise_for_status()  # Raise an exception for bad status codes
        print(f"\tSubcategory '{subcategory_data['name']}'")
    except requests.exceptions.RequestException as e:
        print(f"Error adding subcategory '{subcategory_data['name']}' under parent '{parent_id}': {e}")

def populate_database_from_file(file_path, category_api_url, subcategory_api_url):
    """Populates the database with categories and subcategories from a JSON file."""
    try:
        with open(file_path, 'r') as f:
            data = json.load(f)
    except FileNotFoundError:
        print(f"Error: File not found at '{file_path}'")
        return
    except json.JSONDecodeError:
        print(f"Error: Invalid JSON format in '{file_path}'")
        return

    for category_key, category_value in data.items():
        category_id = add_category(category_value, category_api_url)
        if category_id and "subcats" in category_value:
            for subcat_key, subcat_value in category_value["subcats"].items():
                add_subcategory(subcat_value, category_id, subcategory_api_url)

# Specify the path to your JSON file here
json_file_path = 'cats.json'

category_api_endpoint = 'https://server.petsfort.in/categories'
subcategory_api_endpoint = 'https://server.petsfort.in/subcategories'

populate_database_from_file(json_file_path, category_api_endpoint, subcategory_api_endpoint)