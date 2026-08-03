# ----------------------------------------------------
# 작성자 : 김규현
# 작성목적 : API 응답 변환, 스키마 검증, 파일 저장 기능을 검사
# 작성일 : 2026-08-03
# ----------------------------------------------------

from pathlib import Path

import pandas as pd
import pytest
from pydantic import ValidationError

from src.models import WeatherRecord
from src.storage import records_to_dataframe, save_and_compare
from src.validators import parse_country, parse_ip, parse_weather

# 정상 날씨 응답이 시간별 레코드로 변환되는지 확인
def test_parse_weather() -> None:
    data = {
        "hourly": {
            "time": ["2026-08-03T12:00", "2026-08-03T13:00"],
            "temperature_2m": [30.5, 31.0],
            "precipitation_probability": [40, 30],
        }
    }

    records = parse_weather(data)

    assert len(records) == 2
    assert records[0].temperature == 30.5
    assert records[1].precipitation_probability == 30

# 강수확률이 허용 범위를 넘으면 검증이 실패하는지 확인
def test_invalid_precipitation_probability() -> None:
    data = {
        "hourly": {
            "time": ["2026-08-03T12:00"],
            "temperature_2m": [30.5],
            "precipitation_probability": [120],
        }
    }

    with pytest.raises(ValidationError):
        parse_weather(data)

# Open-Meteo hourly 배열의 길이 불일치를 검출하는지 확인
def test_weather_array_lengths_must_match() -> None:
    data = {
        "hourly": {
            "time": ["2026-08-03T12:00", "2026-08-03T13:00"],
            "temperature_2m": [30.5],
            "precipitation_probability": [40, 30],
        }
    }

    with pytest.raises(ValueError, match="배열의 길이"):
        parse_weather(data)

# 국가 JSON에서 필요한 필드가 올바르게 추출되는지 확인
def test_parse_country() -> None:
    data = {
        "name": "Korea (Republic of)",
        "capital": "Seoul",
        "region": "Asia",
        "population": 51_780_579,
    }

    record = parse_country(data)

    assert record.name == "Korea (Republic of)"
    assert record.capital == "Seoul"

# 국가 API 오류 응답이 ValueError로 변환되는지 확인
def test_country_api_error_message() -> None:
    data = {
        "success": False,
        "data": None,
        "errors": [{"message": "This API version has been deprecated."}],
    }

    with pytest.raises(ValueError, match="deprecated"):
        parse_country(data)

# IP JSON이 지역 정보 모델로 변환되는지 확인
def test_parse_ip() -> None:
    data = {
        "query": "8.8.8.8",
        "country": "United States",
        "city": "Mountain View",
        "lat": 37.4056,
        "lon": -122.0775,
    }

    record = parse_ip(data)

    assert record.ip == "8.8.8.8"
    assert record.latitude == 37.4056

# DataFrame 변환과 CSV·Parquet 저장 기능을 확인
def test_records_to_dataframe() -> None:
    records = [
        WeatherRecord(
            time="2026-08-03T12:00",
            temperature=30.5,
            precipitation_probability=40,
        )
    ]

    dataframe = records_to_dataframe(records)

    assert list(dataframe.columns) == [
        "time",
        "temperature",
        "precipitation_probability",
    ]
    assert len(dataframe) == 1

# CSV·Parquet 파일이 생성되고 다시 읽을 수 있는지 확인
def test_save_csv_and_parquet(
    tmp_path: Path,
    capsys: pytest.CaptureFixture[str],
) -> None:
    records = [
        WeatherRecord(
            time="2026-08-03T12:00",
            temperature=30.5,
            precipitation_probability=40,
        )
    ]

    save_and_compare("weather", records, output_dir=tmp_path)

    csv_path = tmp_path / "weather.csv"
    parquet_path = tmp_path / "weather.parquet"
    output = capsys.readouterr().out

    assert csv_path.exists()
    assert parquet_path.exists()
    assert len(pd.read_csv(csv_path)) == 1
    assert len(pd.read_parquet(parquet_path)) == 1
    assert "CSV" in output
    assert "Parquet" in output
