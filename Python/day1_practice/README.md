# 데이터 수집 미니 파이프라인

서로 다른 3개의 공개 API를 비동기로 호출하고, 필요한 필드를
Pydantic v2 모델로 검증한 뒤 CSV와 Parquet 형식으로 저장하는
미니 데이터 파이프라인입니다. 두 저장 형식의 읽기·쓰기 시간도
함께 측정합니다.

## 주요 목표

- `asyncio.gather()`와 `httpx` 기반의 동시 API 수집
- Pydantic v2를 사용한 타입·범위 검증
- API에서 필요한 필드만 추출하여 일관된 레코드로 변환
- pandas와 pyarrow를 사용한 CSV·Parquet 저장
- `perf_counter()`를 사용한 형식별 읽기·쓰기 시간 비교
- pytest 기능 테스트와 Ruff 코드 스타일 검사

## 사용 API

| 구분 | API | 수집 내용 |
| --- | --- | --- |
| 날씨 | Open-Meteo | 서울 3일 시간대별 기온과 강수확률 |
| 국가 | REST Countries | 대한민국 이름, 수도, 권역, 인구 |
| IP | ip-api | `8.8.8.8` 기반 국가, 도시, 위·경도 |

## 처리 흐름

```text
Open-Meteo ─┐
Countries  ─┼─ asyncio.gather() → JSON 응답 → Pydantic 검증
ip-api     ─┘                                      │
                                                      ├─ CSV 저장/재읽기
                                                      └─ Parquet 저장/재읽기
```

1. `collectors.py`가 세 API 요청을 동시에 시작합니다.
2. `validators.py`가 응답에서 필요한 필드를 추출합니다.
3. `models.py`의 Pydantic 모델이 타입과 값의 범위를 검증합니다.
4. `storage.py`가 검증된 레코드를 DataFrame으로 변환합니다.
5. CSV·Parquet 파일을 저장하고 각각 다시 읽으며 소요 시간을 출력합니다.

## 프로젝트 구조

```text
day1_practice/
├── src/
│   ├── __init__.py
│   ├── collectors.py   # 비동기 HTTP 수집
│   ├── main.py         # 전체 파이프라인 실행
│   ├── models.py       # Pydantic 데이터 모델
│   ├── storage.py      # CSV·Parquet 저장과 시간 측정
│   └── validators.py   # JSON 필드 추출과 모델 검증
├── tests/
│   └── test_models.py  # 변환·검증·저장 테스트
├── data/                 # 실행 시 생성되는 결과 파일
├── pyproject.toml        # pytest·Ruff 설정
├── requirements.txt      # Python 패키지 목록
└── README.md
```

## 환경 구성

Python 3.11 기준입니다.

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

Windows PowerShell에서는 가상환경을 다음과 같이 활성화합니다.

```powershell
.venv\Scripts\Activate.ps1
```

## 실행 방법

프로젝트 최상위 폴더에서 실행합니다.

```bash
python src/main.py
```

가상환경을 활성화하지 않았다면 macOS/Linux에서 다음 명령을
사용할 수 있습니다.

```bash
.venv/bin/python src/main.py
```

정상 실행 시 `data/` 폴더에 다음 파일이 생성됩니다.

```text
weather.csv
weather.parquet
country.csv
country.parquet
ip.csv
ip.parquet
```

저장 후에는 다음과 같은 성능 비교 로그가 출력됩니다.

```text
[weather] 저장 성능 비교
CSV     쓰기: 0.001234초, 읽기: 0.001012초
Parquet 쓰기: 0.010234초, 읽기: 0.008765초
```

측정값은 컴퓨터 환경과 데이터 크기에 따라 달라집니다. 작은 데이터에서는
Parquet 초기화 비용 때문에 CSV가 더 빠르게 측정될 수 있습니다.

## 데이터 검증 규칙

- 날씨 데이터의 시간·기온·강수확률 배열 길이는 같아야 합니다.
- 기온은 `float`로 변환 가능해야 합니다.
- 강수확률은 `None` 또는 0~100 범위의 정수여야 합니다.
- 국가 인구는 0보다 큰 정수여야 합니다.
- 필수 JSON 필드가 빠지거나 응답 구조가 다르면 저장을 중단합니다.

## 테스트와 코드 검사

API의 실시간 상태와 무관하게 테스트할 수 있도록 샘플 JSON과 pytest의
임시 폴더를 사용합니다.

```bash
pytest -q
ruff check .
```

현재 테스트 범위:

- 정상 날씨 데이터의 시간별 변환
- 0~100 범위를 벗어난 강수확률 거부
- Open-Meteo hourly 배열 길이 불일치 검출
- 국가·IP 응답의 Pydantic 모델 변환
- Pydantic 모델의 DataFrame 변환
- CSV·Parquet 파일 생성과 재읽기

`pyproject.toml`은 pytest의 테스트 경로와 Ruff의 Python 3.11, 줄 길이,
기본 오류·import 정렬 규칙을 관리합니다.

