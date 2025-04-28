import threading
import requests
import time

url = 'https://server.petsfort.in/orders/checkout/burDg1buBTVIgI6TBNFcO4Qs5p62'
headers = {'Content-Type': 'application/json'}
data = {
    "otherData": {
        "address": "",
        "notes": ""
    },
    "ProductID1745444432634": {
        "count": 1
    }
}

def call_api(thread_id):
    for i in range(2):
        start_time = time.time()
        try:
            response = requests.post(url, headers=headers, json=data)
            response.raise_for_status()
            end_time = time.time()
            print(f"Thread {thread_id} - Call {i+1}: {response.text} (Start: {start_time:.6f}, End: {end_time:.6f}, Duration: {end_time - start_time:.6f})")
        except requests.exceptions.RequestException as e:
            print(f"Thread {thread_id} - Call {i+1} failed: {e} (Start: {start_time:.6f})")
        time.sleep(0.1)

threads = []
num_threads = 1

for i in range(num_threads):
    thread = threading.Thread(target=call_api, args=(i + 1,))
    threads.append(thread)
    thread.start()

for thread in threads:
    thread.join()

print("All API calls completed.")