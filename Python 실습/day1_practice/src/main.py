# ----------------------------------------------------
# 작성자 : 김규현
# 작성목적 : 데이터 수집, 검증, 저장을 순서대로 연결하는 프로젝트 실행 지점
# 작성일 : 2026-08-03
# ----------------------------------------------------

import asyncio

from pydantic import ValidationError

from collectors import collect_all
from storage import save_and_compare
from validators import parse_country, parse_ip, parse_weather

# API 수집, 검증, 저장 파이프라인을 실행
async def run_pipeline() -> None:
    print("3개 API 데이터 수집을 시작합니다.")
    weather_data, country_data, ip_data = await collect_all()

    # 각 API의 최상위 JSON 형식을 먼저 확인해 명확한 오류를 남긴다.
    if not isinstance(weather_data, dict):
        raise TypeError("날씨 API 응답이 객체 형식이 아닙니다.")
    if not isinstance(country_data, dict):
        raise TypeError("국가 API 응답이 객체 형식이 아닙니다.")
    if not isinstance(ip_data, dict):
        raise TypeError("IP API 응답이 객체 형식이 아닙니다.")

    # 필요한 필드만 추출하고 Pydantic 모델로 타입·범위를 검증한다.
    weather_records = parse_weather(weather_data)
    country_record = parse_country(country_data)
    ip_record = parse_ip(ip_data)
    print("모든 API 데이터가 Pydantic 검증을 통과했습니다.")

    # API별 스키마가 다르므로 각각 별도 파일로 저장한다.
    save_and_compare("weather", weather_records)
    save_and_compare("country", country_record)
    save_and_compare("ip", ip_record)
    print("\n모든 데이터를 data 폴더에 저장했습니다.")

# 비동기 파이프라인을 실행하고 검증 실패를 사용자에게 알림
def main() -> None:
    try:
        asyncio.run(run_pipeline())
    except (ValidationError, ValueError, TypeError) as exc:
        raise SystemExit(f"파이프라인 실행 실패: {exc}") from exc


if __name__ == "__main__":
    main()
