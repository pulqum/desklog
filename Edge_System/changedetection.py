import os
import cv2
import pathlib
import requests
from datetime import datetime
from dotenv import load_dotenv

# .env 파일 로드
load_dotenv()

class ChangeDetection:
    result_prev = []
    HOST = 'http://127.0.0.1:8000'
    username = os.getenv('DJANGO_USERNAME', 'admin')
    password = os.getenv('DJANGO_PASSWORD', 'password')
    token = ''
    title = ""
    text = ""
    
    # 위험도별 객체 분류 (점수 포함)
    DANGER_OBJECTS = {
        'person': 10, 'knife': 15, 'scissors': 12, 'baseball bat': 13,
        'gun': 20, 'rifle': 20
    }  # 위험
    WARNING_OBJECTS = {
        'car': 5, 'truck': 6, 'motorcycle': 5, 'dog': 7, 'cat': 3,
        'bear': 15, 'backpack': 4
    }  # 경고

    def __init__(self, names):
        self.result_prev = [0 for i in range(len(names))]
        res = requests.post(self.HOST + '/api-token-auth/', {
            'username': self.username,
            'password': self.password,
        })
        res.raise_for_status()
        self.token = res.json()['access'] #JWT access 토큰 저장
        print(self.token)

    def add(self, names, detected_current, save_dir, image):
        self.title = ""
        self.text = ""
        change_flag = 0 #변화 감지 플레그
        detected_objects = [] #탐지된 객체 리스트
        i = 0
        while i < len(self.result_prev):
            if self.result_prev[i] == 0 and detected_current[i] == 1:
                change_flag = 1
                detected_objects.append(names[i])
            i += 1

        if detected_objects:
            # 위험도 점수 계산
            total_score = 0
            level_icon = "ℹ️"
            level_text = "감지"
            
            for obj in detected_objects:
                if obj in self.DANGER_OBJECTS:
                    total_score += self.DANGER_OBJECTS[obj]
                elif obj in self.WARNING_OBJECTS:
                    total_score += self.WARNING_OBJECTS[obj]
                else:
                    total_score += 1  # 기본 점수
            
            # 점수에 따른 위험도 판단
            if total_score >= 10:
                level_icon = "🚨"
                level_text = "위험"
            elif total_score >= 5:
                level_icon = "⚠️"
                level_text = "경고"
            
            # title: 위험도 + 점수 + 첫 번째 객체 + 개수
            if len(detected_objects) == 1:
                self.title = f"{level_icon} {level_text}(점수:{total_score}) - {detected_objects[0]} 탐지"
            else:
                self.title = f"{level_icon} {level_text}(점수:{total_score}) - {detected_objects[0]} 외 {len(detected_objects)-1}개"
            
            # text: 시간 + 점수 + 객체 목록
            now = datetime.now()
            self.text = f"{now.strftime('%H:%M:%S')} {level_text} [위험도: {total_score}점] - {', '.join(detected_objects)}"

        self.result_prev = detected_current[:] #객체 검출 상태 저장
        if change_flag == 1:
            self.send(save_dir, image)

    def send(self, save_dir, image):
        now = datetime.now()
        now.isoformat()
        today = datetime.now()
        
        # pathlib.Path를 사용하여 경로 생성
        save_path = pathlib.Path(os.getcwd()) / save_dir / 'detected' / str(today.year) / str(today.month) / str(today.day)
        
        pathlib.Path(save_path).mkdir(parents=True, exist_ok=True)
        
        # 파일 이름 포맷팅
        full_path = save_path / '{0}-{1}-{2}-{3}.jpg'.format(today.hour,today.minute,today.second,today.microsecond)
        
        dst = cv2.resize(image, dsize=(320, 240), interpolation=cv2.INTER_AREA)
        cv2.imwrite(str(full_path), dst) # full_path를 문자열로 변환

        #인증이 필요한 요청에 아래의 headers를 붙임
        headers = {'Authorization': 'JWT ' + self.token, 'Accept': 'application/json'}
        
        # Post Create
        data = {
            'title': self.title,
            'text': self.text,
            'created_date': now,
            'published_date': now
        }
        
        file = {'image': open(full_path, 'rb')}
        
        res = requests.post(self.HOST + '/api_root/Post/', data=data, files=file, headers=headers)
        print(res)
