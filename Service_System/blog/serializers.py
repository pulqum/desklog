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
    
    def to_representation(self, instance):
        """posts를 시간순(오래된 것부터)으로 정렬"""
        representation = super().to_representation(instance)
        if 'posts' in representation and representation['posts']:
            # published_date 기준으로 정렬 (오래된 것부터)
            representation['posts'] = sorted(
                representation['posts'],
                key=lambda x: x.get('published_date', ''),
                reverse=False  # False = 오래된 것부터
            )
        return representation