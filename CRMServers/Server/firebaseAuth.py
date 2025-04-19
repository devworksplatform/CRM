import firebase_admin
from firebase_admin import credentials
from firebase_admin import auth

def initialize_firebase(credential_path="pets-fort-service-acc.json"):
    try:
        cred = credentials.Certificate(credential_path)
        firebase_admin.initialize_app(cred)
        print("Firebase Admin SDK initialized successfully.")
    except Exception as e:
        print(f"Error initializing Firebase Admin SDK: {e}")
        print("Please ensure the service account key file path is correct.")
        exit()

def create_user_account(email, password):
    try:
        user = auth.create_user(
            email=email,
            password=password
        )
        return user.uid,None
    except auth.EmailAlreadyExistsError:
        return None,f"Error: The email '{email}' is already in use."
    except auth.InvalidEmailError:
        return None,f"Error: The email '{email}' is not valid."
    except auth.WeakPasswordError:
        return None,"Error: The password is too weak. It must be at least 6 characters long."
    except Exception as e:
        return None,f"An unexpected error occurred while creating the user: {e}"

def remove_user_account(uid):
    try:
        auth.delete_user(uid)
        return None
    except auth.UserNotFoundError:
        return f"Error: No user found with UID: {uid}"
    except Exception as e:
        return f"An unexpected error occurred while deleting the user: {e}"

def change_user_password(uid, new_password):
    try:
        auth.update_user(
            uid,
            password=new_password
        )
        return None
    except auth.UserNotFoundError:
        return f"Error: No user found with UID: {uid}"
    except Exception as e:
        return f"An unexpected error occurred while updating the user's password: {e}"



service_account_key_path = "pets-fort-service-acc.json"
initialize_firebase(service_account_key_path)