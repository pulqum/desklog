from django.conf import settings
from django.db import models
from django.utils import timezone

class StudySession(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    start_time = models.DateTimeField(default=timezone.now)
    end_time = models.DateTimeField(blank=True, null=True)
    is_active = models.BooleanField(default=True)

    def __str__(self):
        return f"{self.start_time.strftime('%Y-%m-%d %H:%M')} 세션"

    def duration(self):
        if self.end_time:
            return self.end_time - self.start_time
        return timezone.now() - self.start_time

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