# ----------------------------------------------------
# 작성자 : 김규현
# 작성목적 : [심화실습] 파일 I/O, 예외 처리, Pydancit 검증 파이프라인 실습
# 작성일 : 2026-08-03
# ----------------------------------------------------

import json
import logging
import csv, json
from typing import Optional

from pydantic import BaseModel, Field, ValidationError

# =========================================================
# 1) 예외 처리 + 파일 읽기
# =========================================================

# 로깅 설정
logging.basicConfig(level=logging.INFO, force=True)

# JSON 파일을 안전하게 로드하는 함수
def safe_load_json(file_path : str) -> Optional[list[dict]]:
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            return json.load(f)
    except json.JSONDecodeError as e:
        logging.error(f"JSON 파일을 읽는 중 오류 발생: {e}")
        return None  # 오류 발생 시 None 반환
    except FileNotFoundError as e:
        logging.error(f"파일을 찾을 수 없음: {e}")
        return None  # 파일이 없을 경우 None 반환
    finally:
        logging.info('로딩 종료')

# =========================================================
# 2) Pydantic v2 스키마 정의
# =========================================================

class SalesRecord(BaseModel):
    region: str # Not Null
    category: Optional[str] = None # 없어도 됨
    amount: float = Field(gt=0, description='양수') # 0 초과
    month: str # Not Null


# =========================================================
# 3) 검증 파이프라인
# =========================================================

def validate_sales(
    raw_data: list[dict]
) -> tuple[list[dict], list[dict]]:
    """
    원본 데이터를 SalesRecord로 검증합니다.

    - 성공 데이터 → valid 리스트
    - 실패 데이터 → errors 리스트
    """
    valid: list[dict] = []
    errors: list[dict] = []

    for row_number, row in enumerate(raw_data, start=1):
        try:
            record = SalesRecord.model_validate(row)

            valid.append(record.model_dump())

            logging.info(
                "검증 성공: row=%d, data=%s",
                row_number,
                record.model_dump()
            )

        except ValidationError as error:
            errors.append({
                "row": row_number,
                "data": row,
                "error": error.errors()
            })

            logging.error(
                "검증 실패: row=%d, data=%s, error=%s",
                row_number,
                row,
                error.errors()
            )

    logging.info(
        "검증 완료: 성공 %d건, 실패 %d건",
        len(valid),
        len(errors)
    )

    return valid, errors


# =========================================================
# 4) 결과 파일 저장 + 재로딩 확인
# =========================================================

# 검증 성공 데이터를 CSV로 저장
def save_valid_csv(valid: list[dict], file_path: str) -> None:
    fieldnames = ["region", "category", "amount", "month"]

    with open(
        file_path,
        "w",
        encoding="utf-8-sig",
        newline=""
    ) as file:
        writer = csv.DictWriter(file, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(valid)

    logging.info(
        "valid CSV 저장 완료: %s, 총 %d건",
        file_path,
        len(valid)
    )

# 검증 실패 데이터를 JSON으로 저장
def save_errors_json(errors: list[dict], file_path: str) -> None:
    with open(file_path, "w", encoding="utf-8") as file:
        json.dump(
            errors,
            file,
            ensure_ascii=False,
            indent=2
        )

    logging.info(
        "errors JSON 저장 완료: %s, 총 %d건",
        file_path,
        len(errors)
    )


def check_saved_files(
    valid_file_path: str,
    error_file_path: str
) -> None:
    """저장된 파일을 다시 읽어 실제 저장 건수를 확인합니다."""

    with open(
        valid_file_path,
        "r",
        encoding="utf-8-sig",
        newline=""
    ) as file:
        saved_valid = list(csv.DictReader(file))

    with open(error_file_path, "r", encoding="utf-8") as file:
        saved_errors = json.load(file)

    logging.info("재로딩 valid 건수: %d건", len(saved_valid))
    logging.info("재로딩 errors 건수: %d건", len(saved_errors))


# =========================================================
# 실행
# =========================================================

INPUT_FILE = "Day1/Python_Practice2_Data.json"
VALID_FILE = "Day1/valid_sales.csv"
ERROR_FILE = "Day1/error_sales.json"


raw_data = safe_load_json(INPUT_FILE)

# 파일 읽기에 성공한 경우에만 검증 진행
if raw_data is not None:
    valid, errors = validate_sales(raw_data)

    save_valid_csv(valid, VALID_FILE)
    save_errors_json(errors, ERROR_FILE)

    check_saved_files(VALID_FILE, ERROR_FILE)
else:
    logging.error("원본 파일을 읽지 못해 검증을 종료합니다.")