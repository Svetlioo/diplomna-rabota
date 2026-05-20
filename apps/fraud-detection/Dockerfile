FROM python:3.14-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY --chown=app:app app ./app
USER app
EXPOSE 8000
ENTRYPOINT ["python", "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
