FROM python:3.11-slim

# 작업 디렉토리 설정
WORKDIR /app

# 시스템 패키지 설치 (YOLOv5에 필요)
RUN apt-get update && apt-get install -y \
    libgl1-mesa-glx \
    libglib2.0-0 \
    git \
    && rm -rf /var/lib/apt/lists/*

# Python 의존성 파일 복사
COPY requirements-server.txt .

# Python 패키지 설치
RUN pip install --no-cache-dir -r requirements-server.txt

# 프로젝트 파일 복사
COPY . .

# 미디어 및 정적 파일 디렉토리 생성
RUN mkdir -p /app/media /app/staticfiles

# 포트 노출
EXPOSE 8000

# Django 서버 실행 (프로덕션에서는 Gunicorn 사용 권장)
CMD ["python", "manage.py", "runserver", "0.0.0.0:8000"]

