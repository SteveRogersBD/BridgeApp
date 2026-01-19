from fastapi import FastAPI
from common.database import get_db_connection
from common.schemas import ChatSession  # <--- Importing the shared entity

app = FastAPI()
db = get_db_connection()


