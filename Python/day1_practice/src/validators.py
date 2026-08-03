# ----------------------------------------------------
# 작성자 : 김규현
# 작성목적 : 각 API의 JSON 구조를 저장에 적합한 Pydantic 모델로 변환 및 검증
# 작성일 : 2026-08-03
# ----------------------------------------------------

from pydantic import ValidationError

from models import CountryRecord, IPRecord, WeatherRecord


# Open-Meteo의 hourly 배열을 시간별 WeatherRecord로 변환
def parse_weather(data: dict) -> list[WeatherRecord]:
    try:
        hourly = data["hourly"]
        # strict=True로 세 배열의 길이가 다른 비정상 응답도 검출한다.
        rows = zip(
            hourly["time"],
            hourly["temperature_2m"],
            hourly["precipitation_probability"],
            strict=True,
        )
    except (KeyError, TypeError) as exc:
        raise ValueError(f"날씨 응답 구조가 올바르지 않습니다: {exc}") from exc

    records: list[WeatherRecord] = []

    try:
        for index, (time, temperature, probability) in enumerate(rows):
            try:
                # 모델을 생성하는 시점에 타입과 강수확률 범위가 검증된다.
                record = WeatherRecord(
                    time=time,
                    temperature=temperature,
                    precipitation_probability=probability,
                )
            except ValidationError as exc:
                print(f"{index}번 날씨 데이터 검증 실패:\n{exc}")
                raise

            records.append(record)
    except ValueError as exc:
        if "zip() argument" in str(exc):
            raise ValueError("날씨 데이터 배열의 길이가 서로 다릅니다.") from exc
        raise

    return records


def parse_country(data: dict) -> CountryRecord:
    """단일 국가 JSON을 CountryRecord로 변환한다."""
    if data.get("success") is False:
        errors = data.get("errors", [])
        message = (
            errors[0].get("message", "알 수 없는 오류")
            if errors
            else "알 수 없는 오류"
        )
        raise ValueError(f"국가 API 오류: {message}")

    # 응답이 {"data": {...}}로 감싸져 있으면 안쪽 국가 객체를 사용한다.
    country = data.get("data", data)
    if not isinstance(country, dict):
        raise ValueError("국가 API의 data 필드가 객체 형식이 아닙니다.")

    try:
        # 원본 응답의 많은 필드 중 저장에 필요한 값만 추출한다.
        return CountryRecord(
            name=country["name"],
            capital=country["capital"],
            region=country["region"],
            population=country["population"],
        )
    except ValidationError as exc:
        print(f"국가 데이터 검증 실패:\n{exc}")
        raise
    except (KeyError, TypeError) as exc:
        raise ValueError(f"국가 데이터 구조가 올바르지 않습니다: {exc}") from exc


def parse_ip(data: dict) -> IPRecord:
    """ip-api의 단일 JSON 응답을 IPRecord로 변환한다."""
    records: list[IPRecord] = []

    # 단일 응답도 다른 파서와 같이 레코드 단위로 검증하기 위해 반복한다.
    for index, ip_data in enumerate([data]):
        try:
            record = IPRecord(
                ip=ip_data["query"],
                country=ip_data["country"],
                city=ip_data["city"],
                latitude=ip_data["lat"],
                longitude=ip_data["lon"],
            )
        except ValidationError as exc:
            print(f"{index}번 IP 데이터 검증 실패:\n{exc}")
            raise
        except (KeyError, TypeError) as exc:
            raise ValueError(
                f"{index}번 IP 데이터 구조가 올바르지 않습니다: {exc}"
            ) from exc

        records.append(record)

    return records[0]
