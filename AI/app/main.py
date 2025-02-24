from fastapi import FastAPI, UploadFile, Form
from api.diagnostic_ai import DiagnosticAI

app = FastAPI()

diag = DiagnosticAI()

@app.get("/test")
async def test():
    return 'test page'

@app.post("/image")
def process_diagnostic(image_bytes: UploadFile = Form(...), sse_id: int = Form(...)):
    return diag.process_diagnostic(image_bytes, sse_id)
