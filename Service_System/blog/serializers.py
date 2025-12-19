from blog.models import Post
from rest_framework import serializers

class PostSerializer(serializers.ModelSerializer):  # HyperlinkedModelSerializer → ModelSerializer
   class Meta:
     model = Post
     fields = ('id', 'title', 'text', 'created_date', 'published_date', 'image', 'category')