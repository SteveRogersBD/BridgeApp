from pydantic import BaseModel
from datetime import datetime
from typing import Optional

# --- User Entity ---
class User(BaseModel):
    user_id: str
    email: str
    password: str
    created_at: Optional[datetime] = None
    display_name: Optional[str] = None

# --- Chat Entity ---
class ChatSession(BaseModel):
    chat_id: Optional[str] = None
    user_id: str
    title: str
    created_at: Optional[datetime] = None

# --- Conversation/Message Entity ---
class Message(BaseModel):
    chat_id: str
    sender: str
    content: str
    timestamp: Optional[datetime] = None