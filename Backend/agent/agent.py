from typing import TypedDict, List
from langgraph.graph import StateGraph, END, START
from langchain_google_genai import ChatGoogleGenerativeAI
from dotenv import load_dotenv
import os

load_dotenv()

class ChatState(TypedDict):
    transcript: List[str]
    summary: str
    recommendations: List[str]

llm = ChatGoogleGenerativeAI(model="gemini-1.5-flash",
    google_api_key=os.getenv("GOOGLE_API_KEY"))


def analyze_context(state: ChatState):
    # Step 1: Read transcript and understand what's happening
    # Join list of lines into a single context string
    transcript_text = "\n".join(state['transcript'])
    response = llm.invoke(f"Analyze this meeting transcript: {transcript_text}")
    return {"summary": response.content}


def generate_recommendations(state: ChatState):
    # Step 2: Based on the summary, suggest actions
    summary = state['summary']
    response = llm.invoke(f"Based on this context: {summary}, recommend 5 key actions.")
    return {"recommendations": [response.content]}

workflow = StateGraph(ChatState)
workflow.add_node("analyze", analyze_context)
workflow.add_node("recommend", generate_recommendations)
workflow.set_entry_point("analyze")

workflow.add_edge(START, "analyze")
workflow.add_edge("analyze", "recommend")
workflow.add_edge("recommend", END)
app_agent = workflow.compile()
