import firebase_admin
from firebase_admin import credentials
from firebase_admin import auth
from firebase_admin import storage
from firebase_admin import db
from firebase_admin import messaging

def initialize_firebase(credential_path="pets-fort-service-acc.json"):
    try:
        cred = credentials.Certificate(credential_path)
        if not firebase_admin._apps:
            firebase_admin.initialize_app(cred, {
                'storageBucket': 'pets-fort.firebasestorage.app',
                'databaseURL': 'https://pets-fort-default-rtdb.asia-southeast1.firebasedatabase.app'
            })
            print("Firebase Admin SDK initialized successfully.")
        else:
            print("Firebase Admin SDK already initialized.")
        # firebase_admin.initialize_app(cred)
        # print("Firebase Admin SDK initialized successfully.")
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

def upload_file_to_storage(local_file_path, storage_path):
    try:
        bucket = storage.bucket() # Get the default storage bucket
        blob = bucket.blob(storage_path) # Create a blob (file) reference

        # Upload the file
        blob.upload_from_filename(local_file_path)
        blob.make_public()

        # Get the download URL
        # Note: make_public() is needed for get_public_url(), otherwise use generate_signed_url()
        download_url = blob.public_url # Use this if you made the file public

        # Or generate a signed URL for temporary access (more secure)
        # from datetime import timedelta
        # download_url = blob.generate_signed_url(timedelta(seconds=300), method='GET')

        print(f"File '{local_file_path}' uploaded to '{storage_path}' successfully.")
        # You might want to return a public URL or signed URL here
        # For simplicity, let's just return success for now or you can return the blob object
        return blob.path,download_url, None # Returns the path within the bucket, not the full URL by default

    except FileNotFoundError:
        return None, None, f"Error: Local file not found at '{local_file_path}'."
    except Exception as e:
        return None, None, f"An unexpected error occurred while uploading the file: {e}"


service_account_key_path = "pets-fort-service-acc.json"
initialize_firebase(service_account_key_path)



def send_topic_notification(topic: str, title: str, body: str, data: dict = None):
    message = messaging.Message(
        notification=messaging.Notification(
            title=title,
            body=body,
        ),
        data=data if data else {},
        topic=topic,
    )

    try:
        response = messaging.send(message)
        print(f'Successfully sent message: {response}')
    except Exception as e:
        print(f'Error sending message: {e}')


# target_topic = 'all_users'
# notification_title = 'New Announcement!'
# notification_body = 'Check out the latest news and updates.'
# custom_data = {
#     'news_id': '12345',
#     'category': 'important'
# }

# send_topic_notification(target_topic, notification_title, notification_body, custom_data)