from fastapi import FastAPI
from pydantic import BaseModel
from agent import app_agent

app = FastAPI()

class Request(BaseModel):
    transcript: list[str]

@app.post("/recommend")
async def get_recommendations(request: Request):
    result = app_agent.invoke({"transcript": request.transcript})
    return {
        "recommendations": result["recommendations"]
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)


