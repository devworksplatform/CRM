# Import necessary libraries
from firebase_functions import https_fn, scheduler_fn
from firebase_admin import initialize_app
import requests
import json

# Initialize the Firebase app (usually done once)
initialize_app()

# Define the target URL where requests will be proxied
# TODO: Replace with your actual target URL
TARGET_URL = "https://ec2-13-203-205-116.ap-south-1.compute.amazonaws.com"

@https_fn.on_request(region="asia-south1")
def proxy_request(req: https_fn.Request):
    """
    Firebase HTTPS function that acts as a proxy.
    It forwards the incoming request (method, headers, path, query params, body)
    to a specified target URL and returns the response.
    """
    try:
        # Extract details from the incoming request
        method = req.method
        # Headers are case-insensitive, so we can pass them directly
        headers = dict(req.headers) # Convert headers to a mutable dictionary
        path = req.path
        # Query parameters are already in a suitable format
        query_params = req.args

        # Get the request body
        # req.data is bytes, req.get_json() attempts to parse JSON
        # We'll try to get JSON first, fallback to raw data if not JSON
        body = req.get_json(silent=True) # silent=True prevents errors if not JSON
        if body is None:
            body = req.data # Use raw bytes if not JSON

        print(f"Received request: {method} {path}")
        print(f"Headers: {headers}")
        print(f"Query Params: {query_params}")
        # Be careful printing body for large requests
        # print(f"Body: {body}")

        # Construct the full target URL
        full_target_url = f"{TARGET_URL}{path}"

        # Make the request to the target URL using the requests library
        # We use requests.request to handle different HTTP methods dynamically
        response = requests.request(
            method=method,
            url=full_target_url,
            headers=headers,
            params=query_params,
            # Pass data for non-GET methods, use json parameter if body is JSON
            data=body if not isinstance(body, dict) and body is not None else None,
            json=body if isinstance(body, dict) else None,
            verify=False # Set to False if you need to ignore SSL certificate errors (not recommended for production)
        )

        print(f"Proxied request to {full_target_url}")
        print(f"Target response status: {response.status_code}")

        # Prepare the response to send back to the client
        # Copy headers from the target response (excluding hop-by-hop headers)
        # Hop-by-hop headers are headers that are meaningful only for a single transport-level connection
        # and are not forwarded by proxies. Examples: Connection, Keep-Alive, Proxy-Authenticate, Proxy-Authorization, TE, Trailers, Transfer-Encoding, Upgrade.
        # requests handles most of this, but it's good practice to be aware.
        response_headers = dict(response.headers)

        # Return the target response data and status code
        return https_fn.Response(
            response.content, # Use response.content for bytes, response.text for string
            status=response.status_code,
            headers=response_headers
        )

    except requests.exceptions.RequestException as e:
        print(f"Error making request to target URL: {e}")
        return https_fn.Response(f"Proxy Error: Could not reach target URL. {e}", status=500)
    except Exception as e:
        print(f"An unexpected error occurred: {e}")
        return https_fn.Response(f"Proxy Error: An internal error occurred. {e}", status=500)



def perform_backup():
    """
    Performs the core backup logic by making a GET request to the backup endpoint.
    """
    full_target_url = f"{TARGET_URL}/backup"
    print(f"Attempting backup request to {full_target_url}")
    try:
        response = requests.request(
            method="GET",
            url=full_target_url,
            verify=False # Set to False if you need to ignore SSL certificate errors (not recommended for production)
        )
        print(f"Backup target response status: {response.status_code}")

        # You might want to add logging or error handling based on the response status
        if response.status_code == 200:
            print("Backup request successful.")
        else:
            print(f"Backup request failed with status code: {response.status_code}")
            # Log response content for debugging if needed
            # print(f"Backup response content: {response.text}")

        return response.content, response.status_code, dict(response.headers)

    except requests.exceptions.RequestException as e:
        print(f"Error making backup request to target URL: {e}")
        # Depending on your needs, you might want to re-raise the exception
        # or handle it differently for scheduled vs. HTTP triggers.
        raise e # Re-raise the exception for the scheduler to potentially retry

@https_fn.on_request(region="asia-south1")
def backup_request(req: https_fn.Request):
    """
    Firebase HTTPS function to trigger a backup manually via HTTP.
    Calls the core perform_backup logic.
    """
    try:
        content, status, headers = perform_backup()
        return https_fn.Response(content, status=status, headers=headers)
    except Exception as e:
        print(f"Error in backup_request (HTTP trigger): {e}")
        return https_fn.Response(f"Backup Error: {e}", status=500)


# @scheduler_fn.on_schedule(schedule="0 * * * *", region="asia-south1")
@scheduler_fn.on_schedule(schedule="0 */8 * * *", region="asia-south1")
def scheduled_backup(event: scheduler_fn.ScheduledEvent):
    """
    Firebase Scheduled function to trigger a backup every hour.
    Calls the core perform_backup logic.
    The schedule "0 * * * *" means at minute 0 of every hour.
    """
    print("Scheduled backup function triggered.")
    try:
        perform_backup()
        print("Scheduled backup completed successfully.")
    except Exception as e:
        print(f"Error during scheduled backup: {e}")
        # The scheduler will handle retries based on function execution results
        # You might want to log this error to a monitoring system like Cloud Logging

