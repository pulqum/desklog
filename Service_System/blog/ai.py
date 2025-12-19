import torch
import os
from django.conf import settings
import pathlib
import threading

# Windows Path fix for pathlib (sometimes needed for torch.hub on Windows)
temp = pathlib.PosixPath
pathlib.PosixPath = pathlib.WindowsPath

# Define paths
BASE_DIR = settings.BASE_DIR
# Assuming Edge_System (YOLOv5) is in the parent directory of the Django project
YOLO_DIR = os.path.abspath(os.path.join(BASE_DIR, '../Edge_System'))
MODEL_PATH = os.path.join(YOLO_DIR, 'yolov5s.pt')

class YoloDetector:
    _instance = None
    _lock = threading.Lock()  # 모델 로딩 동기화를 위한 락

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(YoloDetector, cls).__new__(cls)
            cls._instance.model = None
            cls._instance._loading = False  # 로딩 중 플래그
        return cls._instance

    def load_model(self):
        # 이미 모델이 로드되어 있으면 바로 반환
        if self.model is not None:
            return
        
        # 다른 스레드가 로딩 중이면 대기
        with self._lock:
            # Double-check: 락을 획득한 후 다시 확인
            if self.model is not None:
                return
            
            # 이미 로딩 중이면 대기
            if self._loading:
                # 로딩이 완료될 때까지 대기 (최대 30초)
                import time
                for _ in range(30):
                    time.sleep(1)
                    if self.model is not None:
                        return
                    if not self._loading:
                        break
                return
            
            # 로딩 시작
            self._loading = True
            try:
                print(f"Loading YOLOv5 model from {YOLO_DIR}...")
                # Load model from local source
                self.model = torch.hub.load(YOLO_DIR, 'custom', path=MODEL_PATH, source='local')
                # Set confidence threshold (낮을수록 더 민감하게 탐지)
                self.model.conf = 0.25  # 0.4 → 0.25로 낮춰서 핸드폰/사람 탐지 민감도 향상 
                self.load_error = None
                print(f"YOLOv5 model loaded successfully!")
            except Exception as e:
                print(f"Error loading model: {e}")
                self.model = None
                self.load_error = str(e)
            finally:
                self._loading = False

    def detect(self, image_path):
        if self.model is None:
            self.load_model()
        
        if self.model is None:
            error_msg = getattr(self, 'load_error', 'Unknown Error')
            return 'STUDY', f'AI 로딩 실패: {error_msg}'

        try:
            results = self.model(image_path)
            df = results.pandas().xyxy[0]
            labels = df['name'].tolist()
            
            print(f"Detected labels: {labels}")

            if 'cell phone' in labels:
                return 'PHONE', '딴짓 감지!'
            elif 'person' in labels:
                return 'STUDY', '열공 중'
            else:
                return 'AWAY', '자리 비움'
        except Exception as e:
            print(f"Detection Error: {e}")
            return 'STUDY', 'AI 분석 오류'

    def detect_demo_video(self):
        """
        테스트용: 웹캠이 없는 환경에서 test/demo_video.mp4 파일로 AI 동작 확인.
        """
        demo_path = os.path.abspath(os.path.join(BASE_DIR, '../test/demo_video.mp4'))
        return self.detect(demo_path)

# Create a singleton instance
detector = YoloDetector()
