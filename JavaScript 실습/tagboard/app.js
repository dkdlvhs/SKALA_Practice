// [순수 자바스크립트 - 글로벌 스코프 방식]
// 외부 라이브러리 및 모듈(import/export)을 절대 사용하지 않는 순수 스크립트입니다.

// 1. DOM 요소 취득
const tagInput = document.getElementById('tag-input');
const tagContainer = document.getElementById('tag-container');

// 2. 키보드 이벤트 리스너(keyup) 연결
tagInput.addEventListener('keyup', function(event) {
    // 사용자가 입력한 키가 'Enter' 인지 확인합니다.
    if (event.key === 'Enter') {
        const tagText = tagInput.value.trim();

        // 3. 입력 값이 비어있지 않은지 검증
        if (tagText !== '') {
            // 태그를 이루는 둥근 span 상자 생성
            const tagSpan = document.createElement('span');
            tagSpan.className = 'tag';
            tagSpan.textContent = tagText;

            // 4. 개별 삭제 버튼 생성
            const removeBtn = document.createElement('button');
            removeBtn.className = 'tag-remove';
            removeBtn.textContent = '×';

            // 삭제 버튼 이벤트 바인딩
            removeBtn.addEventListener('click', function() {
                // 부모 컨테이너에서 해당 태그(span) 엘리먼트를 즉각 축출
                tagContainer.removeChild(tagSpan);
            });

            // 조립: span 내부에 텍스트 다음에 삭제 버튼을 추가
            tagSpan.appendChild(removeBtn);

            // 최종: 태그 보드 영역에 추가
            tagContainer.appendChild(tagSpan);

            // 5. 입력창 청소
            tagInput.value = '';
        }
    }
});