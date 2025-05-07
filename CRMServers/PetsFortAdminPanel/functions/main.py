# Import necessary libraries
from firebase_functions import https_fn
from firebase_admin import initialize_app
import requests
import json

# Initialize the Firebase app (usually done once)
initialize_app()

# Define the target URL where requests will be proxied
# TODO: Replace with your actual target URL
TARGET_URL = "https://ec2-13-203-205-116.ap-south-1.compute.amazonaws.com"

# @https_fn.on_request()
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

