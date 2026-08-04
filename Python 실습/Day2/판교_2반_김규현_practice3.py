# ----------------------------------------------------
# 작성자 : 김규현
# 작성목적 : Pandas EDA, Polars Lazy, DuckDB SQL 비교
# 작성일 : 2026-08-04
# ----------------------------------------------------

"""Pandas, Polars Lazy API, DuckDB SQL 비교 실습."""

from pathlib import Path
from statistics import mean, median
from timeit import repeat

import duckdb
import pandas as pd
import polars as pl

CSV_PATH = Path(__file__).with_name("sales_100k.csv")
GROUPS = ["region", "category"]

# 집계에 필요한 필수 컬럼
REQUIRED = ["region", "category", "amount"]

# 1) Pandas EDA 기초 탐색 + 이상치 처리
def pandas_eda():
    # CSV 파일 읽기
    df = pd.read_csv(CSV_PATH, encoding="utf-8-sig")

    # 기초 EDA shape, head, info, describe, isna().sum()
    print("\n=== 1) Pandas 기초 EDA ===")
    print("크기:", df.shape)
    print("\n[상위 5행]\n", df.head())
    print("\n[열 정보]")
    df.info()
    print("\n[기술 통계]\n", df.describe(include="all").T)
    print("\n[결측치]\n", df.isna().sum())
    # 결측치 제거
    df = df.dropna(subset=REQUIRED).copy()

    # 이상치 처리 IQR
    q1, q3 = df["amount"].quantile([0.25, 0.75])
    iqr = q3 - q1
    lower, upper = q1 - 1.5 * iqr, q3 + 1.5 * iqr

    # 정상 범위
    normal = df["amount"].between(lower, upper)
    print(f"\nIQR 정상 범위: {lower:,.2f} ~ {upper:,.2f}")
    print(f"이상치 제거: {(~normal).sum():,}건")
    return df.loc[normal]

# ----------------------------------------------------
# 세 도구의 groupby 집계 비교를 공정하게 하기 위해 CSV 읽기, 결측치 제거, 이상치 제거를 동일하게 수행하도록 함
# ----------------------------------------------------

# 2) Pandas groupby named aggregation
def pandas_groupby():
    # CSV 파일 읽기
    df = pd.read_csv(CSV_PATH, encoding="utf-8-sig")

    # 결측치 제거
    df = df.dropna(subset=REQUIRED).copy()

    # 이상치 처리 IQR
    q1, q3 = df["amount"].quantile([0.25, 0.75])
    iqr = q3 - q1
    lower, upper = q1 - 1.5 * iqr, q3 + 1.5 * iqr

    # IQR 정상 범위 데이터만 선택
    df = df[df["amount"].between(lower, upper)]

    return (
        df.groupby(GROUPS, as_index=False)
        .agg(average=("amount", "mean"), count=("amount", "count"),
             total=("amount", "sum")) # 총매출, 평균, 건수
        .sort_values("total", ascending=False) # 총매출 내림차순 정렬
        .reset_index(drop=True)
    )

# 3) Polars Lazy API grouby
# scan_csv -> filter -> group_by -> agg -> sort -> collect 순으로 처리
def polars_groupby():
    # CSV 파일 읽기 및 결측치 제거
    raw = (
        pl.scan_csv(CSV_PATH) # scan_csv는 LazyFrame을 반환
        .drop_nulls(REQUIRED) # 결측치 제거
        .cache() # IQR 계산과 필터링에서 동일 데이터를 재사용
    )

    # amount의 Q1, Q3 계산
    bounds = raw.select(
        pl.col("amount").quantile(0.25, interpolation="linear").alias("q1"),
        pl.col("amount").quantile(0.75, interpolation="linear").alias("q3"),
    )

    return (
        raw.join(bounds, how="cross") # 각 행에 IQR 경계 계산용 Q1, Q3 결합
        .filter(
            pl.col("amount").is_between(
                pl.col("q1") - 1.5 * (pl.col("q3") - pl.col("q1")),
                pl.col("q3") + 1.5 * (pl.col("q3") - pl.col("q1")),
                closed="both",
            )
        ) # IQR 이상치 제거
        .group_by(GROUPS) # 그룹화
        .agg(
            pl.col("amount").mean().alias("average"), # 평균
            pl.col("amount").count().alias("count"), # 건수
            pl.col("amount").sum().alias("total"), # 총매출
        )
        .sort("total", descending=True) # 총매출 내림차순 정렬
        .collect() # LazyFrame을 DataFrame으로 변환
    )

# 4) DuckDB SQL grouby
# raw CTE에서 결측치 제거, bounds CTE에서 IQR 계산 후, 최종 SELECT에서 이상치 제거 및 그룹화
def duckdb_groupby():
    sql = """
    WITH raw AS (
        SELECT region, category, amount FROM read_csv_auto(?)
        WHERE region IS NOT NULL AND category IS NOT NULL AND amount IS NOT NULL
    ), bounds AS (
        SELECT quantile_cont(amount, .25) AS q1,
               quantile_cont(amount, .75) AS q3 FROM raw
    )
    SELECT region, category, avg(amount) AS average,
           count(amount) AS count, sum(amount) AS total
    FROM raw, bounds
    WHERE amount BETWEEN q1 - 1.5 * (q3-q1) AND q3 + 1.5 * (q3-q1)
    GROUP BY region, category
    ORDER BY total DESC
    """
    return duckdb.execute(sql, [str(CSV_PATH)]).fetchdf()

# 5) timeit으로 세 도구 실행 시간 비교
# 세 도구 모두 CSV 읽기부터 결과 생성까지 전체 과정을 측정
def benchmark():
    """워밍업 후 10회 실행 시간의 최소값, 중앙값, 평균값을 비교한다."""
    jobs = {
        "Pandas": pandas_groupby,
        "Polars Lazy": polars_groupby,
        "DuckDB SQL": duckdb_groupby,
    }

    # 첫 실행의 라이브러리 초기화 비용을 제외하기 위한 워밍업
    for job in jobs.values():
        job()

    # 각 도구를 10회 실행 (number=1 : 한 번 측정할 때 job을 한 번만 실행, repeat=10 : 10회 반복)
    results = {name: repeat(job, number=1, repeat=10)
               for name, job in jobs.items()}

    # 공정한 비교를 위해 최소값, 중앙값, 평균값 계산
    times = {
        name: {
            "minimum": min(values),
            "median": median(values),
            "average": mean(values),
        }
        for name, values in results.items()
    }

    print("\n=== 세 도구 전체 실행 시간 (10회) ===")
    print(f"{'도구':<12} {'최소값':>12} {'중앙값':>12} {'평균값':>12}")
    print("-" * 52)

    # 중앙값을 기준으로 오름차순 정렬
    for name, values in sorted(
        times.items(), key=lambda item: item[1]["median"]
    ):
        print(
            f"{name:<12} "
            f"{values['minimum']:>10.6f}초 "
            f"{values['median']:>10.6f}초 "
            f"{values['average']:>10.6f}초"
        )


def main():
    pandas_eda()
    print("\n=== 2) Pandas named aggregation ===")
    print(pandas_groupby().to_string(index=False))
    print("\n=== 3) Polars Lazy API ===")
    print(polars_groupby())
    print("\n=== 4) DuckDB SQL ===")
    print(duckdb_groupby().to_string(index=False))
    benchmark()


if __name__ == "__main__":
    main()
