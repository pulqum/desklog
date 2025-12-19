from django.shortcuts import render
from django.utils import timezone
from .models import Post, StudySession
from django.shortcuts import render, get_object_or_404
from .forms import PostForm, ImageUploadForm
from django.shortcuts import redirect
from rest_framework import viewsets
from .serializers import PostSerializer
from .ai import detector  # Import AI detector
from django.contrib.auth.decorators import login_required
from django.views.decorators.http import require_POST
from django.http import JsonResponse

@login_required
def session_list(request):
    sessions = StudySession.objects.filter(user=request.user).order_by('-start_time') if request.user.is_authenticated else []
    active_session = StudySession.objects.filter(user=request.user, is_active=True).first() if request.user.is_authenticated else None
    
    # Check for legacy posts (posts without session)
    has_legacy_posts = Post.objects.filter(author=request.user, session__isnull=True).exists() if request.user.is_authenticated else False
    
    return render(request, 'blog/session_list.html', {
        'sessions': sessions, 
        'active_session': active_session,
        'has_legacy_posts': has_legacy_posts
    })

@login_required
def session_start(request):
    # Close any existing active sessions
    StudySession.objects.filter(user=request.user, is_active=True).update(is_active=False, end_time=timezone.now())
    # Start new session
    StudySession.objects.create(user=request.user)
    return redirect('session_list')

@login_required
def session_end(request):
    StudySession.objects.filter(user=request.user, is_active=True).update(is_active=False, end_time=timezone.now())
    return redirect('session_list')

@login_required
def session_remove(request, session_id):
    session = get_object_or_404(StudySession, pk=session_id)
    if request.method == 'POST':
        session.delete()
        return redirect('session_list')
    return render(request, 'blog/session_confirm_delete.html', {'session': session})

@login_required
def post_list(request, session_id):
    if session_id == 0: # Special ID for legacy posts
        posts = Post.objects.filter(session__isnull=True).order_by('-published_date')
        session = {'start_time': timezone.now(), 'is_legacy': True} # Mock session object
    else:
        session = get_object_or_404(StudySession, pk=session_id)
        posts = session.posts.all().order_by('-published_date')
    
    return render(request, 'blog/post_list.html', {'posts': posts, 'session': session})

@login_required
def post_detail(request, pk):
    post = get_object_or_404(Post, pk=pk)
    return render(request, 'blog/post_detail.html', {'post': post})

@login_required
def post_new(request):
    if request.method == "POST":
        form = PostForm(request.POST, request.FILES)
        if form.is_valid():
            post = form.save(commit=False)
            post.author = request.user
            post.published_date = timezone.now()
            
            # Link to active session
            active_session = StudySession.objects.filter(user=request.user, is_active=True).first()
            if active_session:
                post.session = active_session
            
            post.save()  # Save first to create file
            
            # Run AI Detection
            if post.image:
                try:
                    cat, title_suffix = detector.detect(post.image.path)
                    post.category = cat
                    post.title = title_suffix
                    post.save()
                except Exception as e:
                    print(f"AI Error: {e}")

            return redirect('post_detail', pk=post.pk)
    else:
        form = PostForm()
    return render(request, 'blog/post_edit.html', {'form': form})

@login_required
def post_image_new(request):
    if request.method == "POST":
        form = ImageUploadForm(request.POST, request.FILES)
        if form.is_valid():
            post = form.save(commit=False)
            post.author = request.user
            post.published_date = timezone.now()
            
            # Default values (will be updated by AI)
            post.title = "분석 중..."
            post.text = "AI가 이미지를 분석하고 있습니다."
            post.category = 'STUDY'

            # Link to active session
            active_session = StudySession.objects.filter(user=request.user, is_active=True).first()
            if active_session:
                post.session = active_session
            
            post.save()
            
            # Run AI Detection
            if post.image:
                try:
                    cat, title_suffix = detector.detect(post.image.path)
                    post.category = cat
                    post.title = title_suffix
                    if cat == 'PHONE':
                        post.text = "스마트폰 사용이 감지되었습니다."
                    elif cat == 'AWAY':
                        post.text = "자리를 비우셨군요."
                    else:
                        post.text = "열심히 공부 중입니다!"
                    post.save()
                except Exception as e:
                    print(f"AI Error: {e}")
                    post.title = "AI 분석 실패"
                    post.text = f"오류가 발생했습니다: {e}"
                    post.save()

            if active_session:
                return redirect('post_list', session_id=active_session.pk)
            else:
                return redirect('post_list', session_id=0)
    else:
        form = ImageUploadForm()
    return render(request, 'blog/post_image_upload.html', {'form': form})

@login_required
def post_edit(request, pk):
    post = get_object_or_404(Post, pk=pk)
    if request.method == "POST":
        form = PostForm(request.POST, request.FILES, instance=post)
        if form.is_valid():
            post = form.save(commit=False)
            post.author = request.user
            post.published_date = timezone.now()
            post.save()
            return redirect('post_detail', pk=post.pk)
    else:
        form = PostForm(instance=post)
    return render(request, 'blog/post_edit.html', {'form': form})

@login_required
def post_correction(request, pk):
    post = get_object_or_404(Post, pk=pk)
    post.category = 'STUDY'
    post.title = '[정정됨] 공부 중'
    post.save()
    return redirect('post_detail', pk=post.pk)

@login_required
def post_remove(request, pk):
    post = get_object_or_404(Post, pk=pk)
    post.delete()
    return redirect('post_list')

def js_test(request):
    return render(request, 'blog/js_test.html', {})

class IntruderImage(viewsets.ModelViewSet):
    queryset = Post.objects.all()
    serializer_class = PostSerializer


@login_required
@require_POST
def session_capture(request, session_id):
    """
    Receive a webcam frame, run YOLO detection, and only create a Post
    when the detected category changes compared to the latest post in this session.
    Returns JSON so the frontend can show feedback without reloading.
    """
    session = get_object_or_404(StudySession, pk=session_id, user=request.user, is_active=True)

    frame_file = request.FILES.get("frame")
    if not frame_file:
        return JsonResponse({"status": "error", "message": "No frame uploaded."}, status=400)

    # Get previous category for change detection
    last_post = session.posts.order_by("-created_date").first()
    last_category = last_post.category if last_post else None

    # Create a new post for this frame
    post = Post(
        session=session,
        author=request.user,
        title="분석 중...",
        text="AI가 이미지를 분석하고 있습니다.",
        category="STUDY",
        published_date=timezone.now(),
    )
    post.image = frame_file
    post.save()

    # Run AI detection
    try:
        cat, title_suffix = detector.detect(post.image.path)
        post.category = cat
        post.title = title_suffix
        if cat == "PHONE":
            post.text = "스마트폰 사용이 감지되었습니다."
        elif cat == "AWAY":
            post.text = "자리를 비우셨군요."
        else:
            post.text = "열심히 공부 중입니다!"
        post.save()
    except Exception as e:
        # On AI error, keep the post but mark appropriately
        post.title = "AI 분석 실패"
        post.text = f"오류가 발생했습니다: {e}"
        post.category = "STUDY"
        post.save()
        return JsonResponse(
            {"status": "error", "message": "AI 분석 중 오류가 발생했습니다.", "category": "STUDY"},
            status=500,
        )

    # Change detection: only keep the post if category actually changed
    if last_category is not None and last_category == post.category:
        # No change → delete this redundant post
        post.delete()
        return JsonResponse({"status": "no_change", "category": last_category})

    return JsonResponse(
        {
            "status": "changed",
            "category": post.category,
            "title": post.title,
            "text": post.text,
            "post_id": post.pk,
        }
    )