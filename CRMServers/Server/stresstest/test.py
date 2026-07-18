# This file contains the WSGI configuration required to serve up your
# web application at http://<your-username>.pythonanywhere.com/
# It works by setting the variable 'application' to a WSGI handler of some
# description.

import sys

# project_home = '/home/jayaraj87/mysite'
# if project_home not in sys.path:
#     sys.path.insert(0, project_home) # Using insert(0, project_home) is also common

# Import the ASGI to WSGI adapter
from a2wsgi import ASGIMiddleware
# from main import app as fastapi_application




from fastapi import FastAPI

app = FastAPI()

@app.get("/")
def read_root():
    return {"message": "Hello, World! Fastapi"}

# Wrap your FastAPI ASGI application to make it a WSGI application
application = ASGIMiddleware(app)


