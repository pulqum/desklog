from django.conf import settings
from django.db import models
from django.utils import timezone

class StudySession(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    start_time = models.DateTimeField(default=timezone.now)
    end_time = models.DateTimeField(blank=True, null=True)
    is_active = models.BooleanField(default=True)
    
    # 통계 필드
    total_study = models.IntegerField(default=0, help_text="집중한 횟수")
    total_phone = models.IntegerField(default=0, help_text="딴짓한 횟수")
    total_away = models.IntegerField(default=0, help_text="자리비움 횟수")
    focus_score = models.FloatField(default=0.0, help_text="집중 점수 (0-100)")

    def __str__(self):
        return f"{self.start_time.strftime('%Y-%m-%d %H:%M')} 세션"

    def duration(self):
        if self.end_time:
            return self.end_time - self.start_time
        return timezone.now() - self.start_time
    
    def calculate_statistics(self):
        """세션의 통계를 계산하여 저장"""
        posts = self.posts.all()
        total_count = posts.count()
        
        if total_count == 0:
            self.total_study = 0
            self.total_phone = 0
            self.total_away = 0
            self.focus_score = 0.0
        else:
            self.total_study = posts.filter(category='STUDY').count()
            self.total_phone = posts.filter(category='PHONE').count()
            self.total_away = posts.filter(category='AWAY').count()
            
            # 집중 점수 계산: STUDY 비율 * 100
            self.focus_score = round((self.total_study / total_count) * 100, 1)
        
        self.save()

class Post(models.Model):
    session = models.ForeignKey(StudySession, on_delete=models.CASCADE, related_name='posts', null=True, blank=True)
    author = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, default=1)
    title = models.CharField(max_length=200)
    text = models.TextField()
    created_date = models.DateTimeField(default=timezone.now)
    published_date = models.DateTimeField(blank=True, null=True)
    image = models.ImageField(upload_to='intruder_image/%Y/%m/%d/', default='intruder_image/default_error.png')

    CATEGORY_CHOICES = [
        ('STUDY', '공부중'),
        ('PHONE', '딴짓'),
        ('AWAY', '이석'),
    ]
    category = models.CharField(max_length=10, choices=CATEGORY_CHOICES, default='STUDY')

    def publish(self):
        self.published_date = timezone.now()
        self.save()

    def __str__(self):
        return self.title