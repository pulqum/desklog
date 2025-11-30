import torch
import os
from django.conf import settings
import pathlib

# Windows Path fix for pathlib (sometimes needed for torch.hub on Windows)
temp = pathlib.PosixPath
pathlib.PosixPath = pathlib.WindowsPath

# Define paths
BASE_DIR = settings.BASE_DIR
# Assuming YOLOv5 is in the parent directory of PhotoBlogServer
YOLO_DIR = os.path.abspath(os.path.join(BASE_DIR, '../YOLOv5'))
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
            except Exception as e:
                print(f"Error loading model: {e}")
                self.model = None

    def detect(self, image_path):
        if self.model is None:
            self.load_model()
        
        if self.model is None:
            return 'STUDY', 'AI 로딩 실패 (기본값)'

        try:
            results = self.model(image_path)
            df = results.pandas().xyxy[0]
            labels = df['name'].tolist()
            
            print(f"Detected labels: {labels}")

            if 'cell phone' in labels:
                return 'PHONE', '🚨 딴짓 감지! (휴대폰)'
            elif 'person' in labels:
                return 'STUDY', '📖 열공 중 (사람 감지)'
            else:
                return 'AWAY', '🏃 자리 비움 (사람 없음)'
        except Exception as e:
            print(f"Detection Error: {e}")
            return 'STUDY', 'AI 분석 오류'

# Create a singleton instance
detector = YoloDetector()
