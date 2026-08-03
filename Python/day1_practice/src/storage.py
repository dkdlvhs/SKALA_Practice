# ----------------------------------------------------
# 작성자 : 김규현
# 작성목적 : 검증을 통과한 데이터를 CSV·Parquet으로 저장하고 성능을 비교
# 작성일 : 2026-08-03
# ----------------------------------------------------

from collections.abc import Sequence
from pathlib import Path
from time import perf_counter

import pandas as pd
from pydantic import BaseModel

# Pydantic 모델 하나 또는 목록을 DataFrame으로 변환
def records_to_dataframe(
    records: BaseModel | Sequence[BaseModel],
) -> pd.DataFrame:
    if isinstance(records, BaseModel):
        record_list = [records]
    else:
        record_list = list(records)

    if not record_list:
        raise ValueError("저장할 데이터가 없습니다.")

    rows: list[dict] = []

    for index, record in enumerate(record_list):
        if not isinstance(record, BaseModel):
            raise TypeError(f"{index}번 데이터가 Pydantic 모델이 아닙니다.")
        # model_dump로 Pydantic 모델을 pandas가 처리할 수 있는 dict로 변환한다.
        rows.append(record.model_dump())

    return pd.DataFrame(rows)

# 저장된 CSV·Parquet 파일을 다시 읽어 실제 소요 시간을 측정하고 출력
def save_and_compare(
    name: str,
    records: BaseModel | Sequence[BaseModel],
    output_dir: str | Path = "data",
) -> None:
    dataframe = records_to_dataframe(records)
    directory = Path(output_dir)
    directory.mkdir(parents=True, exist_ok=True)

    csv_path = directory / f"{name}.csv"
    parquet_path = directory / f"{name}.parquet"

    # 각 형식을 한 번씩 저장하고 다시 읽어 실제 소요 시간을 측정한다.
    # csv 저장
    start = perf_counter()
    dataframe.to_csv(csv_path, index=False)
    csv_write_time = perf_counter() - start

    # csv 읽기
    start = perf_counter()
    pd.read_csv(csv_path)
    csv_read_time = perf_counter() - start

    # parquet 저장
    start = perf_counter()
    dataframe.to_parquet(parquet_path, index=False)
    parquet_write_time = perf_counter() - start

    # parquet 읽기
    start = perf_counter()
    pd.read_parquet(parquet_path)
    parquet_read_time = perf_counter() - start

    print(f"\n[{name}] 저장 성능 비교")
    print(f"CSV     쓰기: {csv_write_time:.6f}초, 읽기: {csv_read_time:.6f}초")
    print(
        f"Parquet 쓰기: {parquet_write_time:.6f}초, "
        f"읽기: {parquet_read_time:.6f}초"
    )
