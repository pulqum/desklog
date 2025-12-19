import torch
import os
from django.conf import settings
import pathlib

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

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(YoloDetector, cls).__new__(cls)
            cls._instance.model = None
        return cls._instance

    def load_model(self):
        if self.model is None:
            print(f"Loading YOLOv5 model from {YOLO_DIR}...")
            try:
                # Load model from local source
                self.model = torch.hub.load(YOLO_DIR, 'custom', path=MODEL_PATH, source='local')
                # Set confidence threshold
                self.model.conf = 0.4 
                self.load_error = None
            except Exception as e:
                print(f"Error loading model: {e}")
                self.model = None
                self.load_error = str(e)

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
