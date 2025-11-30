from django.urls import path
from . import views
from django.urls import include
from rest_framework import routers

router = routers.DefaultRouter()
router.register('Post', views.IntruderImage)

urlpatterns = [
    path('', views.session_list, name='session_list'),
    path('session/start/', views.session_start, name='session_start'),
    path('session/end/', views.session_end, name='session_end'),
    path('session/<int:session_id>/remove/', views.session_remove, name='session_remove'),
    path('session/<int:session_id>/', views.post_list, name='post_list'),
    path('post/<int:pk>/', views.post_detail, name='post_detail'), 
    path('post/new/', views.post_new, name='post_new'),
    path('post/image_new/', views.post_image_new, name='post_image_new'),
    path('post/<int:pk>/edit/', views.post_edit, name='post_edit'),
    path('post/<int:pk>/correction/', views.post_correction, name='post_correction'),
    path('post/<int:pk>/remove/', views.post_remove, name='post_remove'),
    path('js_test/', views.js_test, name='js_test'),
    path('api_root/', include(router.urls)),
]