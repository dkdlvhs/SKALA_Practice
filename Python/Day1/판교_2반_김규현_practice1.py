# ----------------------------------------------------
# 작성자 : 김규현
# 작성목적 : [심화실습] 자료구조 집계, 컴프리헨션, 제너레이터 실습
# 작성일 : 2026-08-03
# 변경내역 : 초기 작성 및 코드별 기능 설명 주석 추가
# ----------------------------------------------------
import json, sys
from collections import Counter, defaultdict

# JSON 파일의 매출 거래 데이터를 읽어 리스트로 저장
with open("Day1/Python_Practice2_Data.json", "r", encoding="utf-8") as f:
    sales = json.load(f)


# 1) 리스트/딕셔너리 컴프리헨션
# amount ≥ 1000인 거래만 필터링하고, 지역별 총매출 dict를 컴프리헨션으로 계산
print("1) 리스트/딕셔너리 컴프리헨션")

# 지역별 총매출을 누적하기 위해 기본값이 0인 딕셔너리를 생성
region_total = defaultdict(int)

# 1,000 이상인 거래만 선별하여 해당 지역의 총매출에 누적
for sale in sales:
    if sale["amount"] >= 1000:
        region_total[sale["region"]] += sale["amount"]

# 출력과 후속 처리를 위해 defaultdict를 일반 dict로 변환
region_total = dict(region_total)

print(region_total)

# 2) Counter + defaultdict
# Counter로 지역별 거래 건수를, defaultdict로 카테고리별 amount 리스트
print("2) Counter + defaultdict")

# Counter를 사용해 지역별 거래 발생 횟수를 집계
Counter_region = Counter(sale["region"] for sale in sales)

# 새로운 카테고리에 빈 리스트를 자동 생성하는 딕셔너리를 준비
defaultdict_category = defaultdict(list)

# 각 거래의 매출액을 해당 카테고리의 리스트에 추가
for sale in sales:
    defaultdict_category[sale["category"]].append(sale["amount"])

print(Counter_region)
print(Counter_region.most_common(1))  # 가장 많은 거래 건수 지역
print(defaultdict_category)


# 3) 제너레이터 - 메모리 비교
# amount > 1000 인 행만 yield 하는 제너레이터를 작성하고 리스트 버전과 메모리 크기를 비교
print("3) 제너레이터 - 메모리 비교")

# 제너레이터 버전
def amount_generator():
    """1,000을 초과하는 매출 거래를 하나씩 생성한다."""
    for sale in sales:
        if sale["amount"] > 1000:
            yield sale

# 함수를 호출하되 데이터는 아직 생성하지 않은 제너레이터 객체를 생성
generator_version = amount_generator()

# 리스트 버전
# 조건에 맞는 모든 거래를 메모리에 즉시 저장하여 제너레이터와 비교
list_version = [sale for sale in sales if sale["amount"] > 1000]

# 제너레이터 객체와 리스트 객체가 차지하는 메모리 크기를 출력
print(f"제너레이터 버전 크기: {sys.getsizeof(generator_version)}")
print(f"리스트 버전 크기: {sys.getsizeof(list_version)}")


# 4) 종합 - 월별 카테고리 매출 집계
# sales 데이터를 month,category 기준으로 그룹핑해 총매출 dict를 완성
# (컴프리헨션 + defaultdict)
print("4) 종합 - 월별 카테고리 매출 집계")

# 월과 카테고리를 2단계 키로 사용하는 중첩 defaultdict를 생성
# 새로운 카테고리의 초기 매출액은 0.0으로 자동 설정됨
monthly_category_sales = defaultdict(lambda: defaultdict(float))

# 각 거래의 월별·카테고리별 매출액을 누적
for sale in sales:
    month = sale["month"]
    category = sale["category"]
    amount = sale["amount"]

    monthly_category_sales[month][category] += amount

# 출력 및 후속 처리를 위해 내부 defaultdict를 일반 dict로 변환
result = {
    month: dict(categories)
    for month, categories in monthly_category_sales.items()
}

print(result)
