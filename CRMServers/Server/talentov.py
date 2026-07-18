import json
import urllib.parse
import os
import requests
import re

def sanitize_filename(s):
    # Remove invalid characters and control characters from filenames
    s = re.sub(r'[<>:"/\\|?*\t\r\n]', '_', s)  # Replaces all invalids including tab, CR, LF
    return s

# def sanitize_filename(s):
#     # Remove or replace characters that are invalid in Windows filenames
#     return re.sub(r'[<>:"/\\|?*]', '_', s)

# File to store processed candidate IDs
processed_ids_file = "candidate_ids.txt"

# Load already processed IDs
if os.path.exists(processed_ids_file):
    with open(processed_ids_file, "r", encoding="utf-8") as f:
        processed_ids = set(line.strip() for line in f if line.strip())
else:
    processed_ids = set()

# Load the JSON data
with open("C:/Users/Jay/Downloads/myDatabase_indexeddb_export.json", encoding="utf-8") as f:
    json_data = f.read()
data = json.loads(json_data)

print("JSON loaded")

# Directory to save downloaded resumes
download_dir = "downloaded_resumes"
os.makedirs(download_dir, exist_ok=True)

# Process each candidate
c = 0
total_candidates = len(data["dataStore"])
for idx, candidate in enumerate(data["dataStore"], 1):  # Start index at 1
    c += 1
    candidate_id = candidate.get("id", "")
    if candidate_id in processed_ids:
        continue

    candidate_name = sanitize_filename(candidate.get("CANDIDATE_NAME", "").replace(" ", "_"))
    current_company = sanitize_filename(candidate.get("CURRENT_COMPANY", "").replace(" ", "_"))
    email_id = sanitize_filename(candidate.get("EMAIL_ID", "").replace(" ", "_"))
    # candidate_name = candidate.get("CANDIDATE_NAME", "").replace(" ", "_")
    # current_company = candidate.get("CURRENT_COMPANY", "").replace(" ", "_")
    # email_id = candidate.get("EMAIL_ID", "").replace(" ", "_")
    resume_url = candidate.get("RESUME_URL", "")

    if resume_url:
        try:
            filename = os.path.basename(urllib.parse.unquote(urllib.parse.urlparse(resume_url).path))
            download_filename = f"{candidate_id}_{candidate_name}_{current_company}_{email_id}_{filename}"
            full_path = os.path.join(download_dir, download_filename)

            # Download the file
            response = requests.get(resume_url, timeout=30)
            response.raise_for_status()  # Raise error if download failed

            # Save the file
            with open(full_path, "wb") as f:
                f.write(response.content)
            print("Downloaded:", download_filename)

            # Save the processed ID
            with open(processed_ids_file, "a", encoding="utf-8") as f:
                f.write(candidate_id + "\n")
            processed_ids.add(candidate_id)

            # Display current progress (cur/total, curpercentage/100%)
            cur_percentage = (c / total_candidates) * 100
            print(f"Downloaded {c}/{total_candidates} resumes ({cur_percentage:.2f}%)")
            print("-" * 50)

        except Exception as e:
            print(f"Download failed for {candidate_id}: {e}")
            print("Stopping process.")
            break

print("Total downloaded:", c)
