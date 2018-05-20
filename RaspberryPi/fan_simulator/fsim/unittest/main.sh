#!/bin/bash
echo "START SERVER"
export FLASK_APP=../main.py

# listens on http://localhost:5000
flask run &
