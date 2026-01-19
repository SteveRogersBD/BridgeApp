import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore
import os

# A global variable to hold the database connection so we don't connect multiple times
_db = None


def get_db_connection():
    global _db

    # If we are already connected, just return the existing connection
    if _db is not None:
        return _db

    # Check if Firebase is already initialized
    if not firebase_admin._apps:
        # Build the path to the key file.
        # This assumes database.py is in 'Backend/common/' and key is in 'Backend/'
        current_dir = os.path.dirname(os.path.abspath(__file__))
        key_path = os.path.join(current_dir, "..", "serviceAccountKey.json")

        if os.path.exists(key_path):
            cred = credentials.Certificate(key_path)
            firebase_admin.initialize_app(cred)
            print("Firebase connected successfully!")
        else:
            print(f"Error: Key not found at {key_path}")
            # Optional: Fallback for cloud environments
            # firebase_admin.initialize_app()

    # Get the Firestore client
    _db = firestore.client()
    return _db