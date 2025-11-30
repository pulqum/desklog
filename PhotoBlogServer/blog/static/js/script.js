// Masonry 레이아웃 및 알약 토글 초기화
(function() {
	function initMasonry() {
		var grid = document.querySelector('.masonry-grid');
		if (!grid || typeof Masonry === 'undefined') return;

		var msnry = new Masonry(grid, {
			itemSelector: '.masonry-grid-item',
			columnWidth: '.masonry-grid-sizer',
			percentPosition: true,
			horizontalOrder: true
		});

		var isSorting = false;    // 정렬 중 재진입 방지
		var pendingSort = null;   // 정렬 도중 들어온 마지막 요청 보관

		// 이미지 로딩 완료 후 재계산
		if (typeof imagesLoaded !== 'undefined') {
			imagesLoaded(grid, function() { msnry.layout(); });
		}

		// 알약 클릭 시 정렬 변경
		var pillOptions = Array.from(document.querySelectorAll('.pill-option'));
		var pillThumb = document.querySelector('.pill-thumb');

		function updatePillThumb(targetEl) {
			if (!pillThumb || !targetEl) return;
			var parent = targetEl.parentElement;
			var parentRect = parent.getBoundingClientRect();
			var rect = targetEl.getBoundingClientRect();
			var left = rect.left - parentRect.left;
			pillThumb.style.left = left + 'px';
			pillThumb.style.width = rect.width + 'px';
		}

		function applySortAndAnimate(opt) {
			// 이미 활성인 경우 무시
			if (opt.classList.contains('pill-active')) return;
			// 정렬 중이면 마지막 요청으로 교체하고 대기
			if (isSorting) { pendingSort = opt; return; }
			isSorting = true;
			pillOptions.forEach(function(o){ o.classList.remove('pill-active'); });
			opt.classList.add('pill-active');
			updatePillThumb(opt);

			// Masonry 아이템 정렬: 발행일 타임스탬프, 동률 시 PK로 안정 정렬
			var sortType = opt.getAttribute('data-sort');
			var items = Array.from(grid.querySelectorAll('.masonry-grid-item'));
			items.sort(function(a, b) {
				var ap = parseInt(a.getAttribute('data-published') || '0', 10);
				var bp = parseInt(b.getAttribute('data-published') || '0', 10);
				if (ap === bp) {
					var apk = parseInt(a.getAttribute('data-pk') || '0', 10);
					var bpk = parseInt(b.getAttribute('data-pk') || '0', 10);
					return sortType === 'asc' ? apk - bpk : bpk - apk;
				}
				return sortType === 'asc' ? ap - bp : bp - ap;
			});
			items.forEach(function(item){ grid.appendChild(item); });
			msnry.reloadItems();
			// 레이아웃 완료되면 락 해제 및 보류된 요청 처리
			msnry.once('layoutComplete', function(){
				isSorting = false;
				if (pendingSort) {
					var next = pendingSort; pendingSort = null;
					// 다음 요청이 현재 활성과 다를 때만 실행
					if (!next.classList.contains('pill-active')) {
						applySortAndAnimate(next);
					}
				}
			});
			msnry.layout();
		}

		pillOptions.forEach(function(opt) {
			opt.addEventListener('click', function() { applySortAndAnimate(opt); });
		});

		// 초기 thumb 위치 설정(기본 최신순 활성화)
		var initialActive = document.querySelector('.pill-option.pill-active');
		if (initialActive) { updatePillThumb(initialActive); }
	}

	function enableCardClick() {
		var items = document.querySelectorAll('.post.is-clickable');
		items.forEach(function(item) {
			item.addEventListener('click', function(e) {
				if (e.target.closest('a')) return;
				var href = item.getAttribute('data-href');
				if (href) { window.location.href = href; }
			});
		});
	}

	// Masonry 아이템에 data-idx 부여(최신순/오래된순 JS 정렬용)
	function setMasonryIdx() {
		var items = document.querySelectorAll('.masonry-grid-item');
		items.forEach(function(item, idx){ item.setAttribute('data-idx', idx); });
	}

	if (document.readyState === 'loading') {
		document.addEventListener('DOMContentLoaded', function(){
			setMasonryIdx();
			initMasonry();
			enableCardClick();
		});
	} else {
		setMasonryIdx();
		initMasonry();
		enableCardClick();
	}
})();