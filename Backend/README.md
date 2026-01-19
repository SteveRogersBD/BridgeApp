# Bridge Agent Backend

This directory contains the intelligent backend for the Bridge application. It hosts a **LangGraph-based Agent** that provides context-aware analysis and action recommendations based on meeting transcripts.

## 🧠 Agentic Architecture

The system moves logic off the client device and into a specialized server, allowing for complex multi-step reasoning without burdening the mobile app.

### System Data Flow

```mermaid
graph TD
    subgraph Client ["📱 Android Application"]
        UI[User Interface] -- "1. Captures Audio/Text" --> Network[GeminiHelper / Retrofit]
    end

    subgraph Server ["🖥️ Backend Service (Python)"]
        API[FastAPI Endpoint /recommend]
        
        subgraph Agent ["🤖 LangGraph Workflow"]
            State[Shared State\n(Transcript, Summary, Recs)]
            Node1[Analyze Context]
            Node2[Recommend Actions]
        end
    end

    subgraph External ["☁️ Google Cloud"]
        LLM[Gemini 1.5 Flash]
    end

    %% Connections
    Network -- "2. POST /recommend\n{ transcript: [...] }" --> API
    API -- "3. Invoke Agent" --> State
    
    State --> Node1
    Node1 -- "4. Analyze Intent" --> LLM
    LLM -.-> Node1
    
    Node1 -- "Update State\n(Summary)" --> Node2
    Node2 -- "5. Generate Actions" --> LLM
    LLM -.-> Node2
    
    Node2 -- "Update State\n(Recommendations)" --> API
    API -- "6. Return JSON" --> Network
    Network -- "7. Update UI" --> UI
```

## 🛠️ Technology Stack

- **Framework**: [FastAPI](https://fastapi.tiangolo.com/) (High-performance Async API)
- **Orchestration**: [LangGraph](https://python.langchain.com/docs/langgraph) (Stateful multi-actor applications)
- **LLM Integration**: [LangChain](https://python.langchain.com/docs/get_started/introduction) + Google Generative AI
- **Model**: Gemini 1.5 Flash

## 🤖 The Workflow

The agent is defined as a graph with a shared state. It does not simply "complete text"; it follows a specific reasoning path:

1.  **State Definition**:
    - `transcript`: List of strings (the conversation history).
    - `summary`: A synthesized understanding of the context.
    - `recommendations`: Concrete actionable steps.

2.  **Node 1: Analyze (`analyze_context`)**
    - **Input**: Raw transcript lines.
    - **Action**: Aggregates the conversation and asks Gemini to summarize the key context and user intent.
    - **Output**: Updates the `summary` state.

3.  **Node 2: Recommend (`generate_recommendations`)**
    - **Input**: The `summary` from the previous step.
    - **Action**: Asks Gemini to produce 5 specific, distinct action items based *only* on that summary.
    - **Output**: Updates the `recommendations` state.

## 🚀 Getting Started

### Prerequisites
- Python 3.10+
- A Google Cloud API Key with access to Gemini.

### Installation

1.  Navigate to the agent directory:
    ```bash
    cd Backend/Agent
    ```

2.  Install dependencies:
    ```bash
    pip install -r requirements.txt
    ```

3.  Set up your environment:
    Create a `.env` file in this directory:
    ```env
    GOOGLE_API_KEY=your_api_key_here
    ```

### Running the Server

Start the API server (wraps the agent):

```bash
python server.py
# OR
uvicorn server:app --reload
```

The server will listen on `http://0.0.0.0:8000`.

### API Usage

**Endpoint**: `POST /recommend`

**Request Body**:
```json
{
  "transcript": [
    "Hey, we need to schedule the marketing review.",
    "Sure, how about next Tuesday at 2 PM?",
    "Sounds good, I'll send the invite."
  ]
}
```

**Response**:
```json
{
  "recommendations": [
    "Send calendar invite for Marketing Review on Tuesday at 2 PM.",
    "Prepare agenda for the marketing meeting.",
    "Review previous campaign metrics before Tuesday.",
    "...",
    "..."
  ]
}
```
