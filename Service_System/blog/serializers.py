from blog.models import Post, StudySession
from rest_framework import serializers

class PostSerializer(serializers.ModelSerializer):  # HyperlinkedModelSerializer → ModelSerializer
   class Meta:
     model = Post
     fields = ('id', 'title', 'text', 'created_date', 'published_date', 'image', 'category', 'session')

class StudySessionSerializer(serializers.ModelSerializer):
    posts = PostSerializer(many=True, read_only=True)
    
    class Meta:
        model = StudySession
        fields = ('id', 'start_time', 'end_time', 'is_active', 'total_study', 'total_phone', 'total_away', 'focus_score', 'posts')