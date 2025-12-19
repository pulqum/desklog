from django import forms
from django.contrib.auth.forms import UserCreationForm
from django.contrib.auth.models import User
from .models import Post

class PostForm(forms.ModelForm):
    class Meta:
        model = Post
        fields = ('title', 'text', 'image', 'category')

class ImageUploadForm(forms.ModelForm):
    class Meta:
        model = Post
        fields = ('image',)

class SignUpForm(UserCreationForm):
    email = forms.EmailField(required=True, label='이메일')
    
    class Meta:
        model = User
        fields = ('username', 'email', 'password1', 'password2')
        labels = {
            'username': '아이디',
        }
    
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.fields['password1'].label = '비밀번호'
        self.fields['password2'].label = '비밀번호 확인'