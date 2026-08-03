# ----------------------------------------------------
# 작성자 : 김규현
# 작성목적 : HTTP API에서 날씨, 국가, IP 데이터를 비동기로 수집
# 작성일 : 2026-08-03
# ----------------------------------------------------


import asyncio

import httpx

WEATHER_URL = (
    "https://api.open-meteo.com/v1/forecast"
    "?latitude=37.5665"
    "&longitude=126.9780"
    "&hourly=temperature_2m,precipitation_probability"
    "&forecast_days=3"
    "&timezone=Asia/Seoul"
)

COUNTRY_URL = "https://countries.dev/alpha/KOR"

IP_URL = "http://ip-api.com/json/8.8.8.8"


# 비동기 HTTP 요청을 수행하는 함수
# URL을 호출하고 JSON 응답을 반환하며
# 네트워크 오류나 HTTP 상태 오류가 발생하면 빈 딕셔너리를 반환
async def fetch(client: httpx.AsyncClient, url: str) -> dict:
    try:
        response = await client.get(url, timeout=10.0)
        # 4xx, 5xx 응답을 정상 JSON처럼 처리하지 않도록 예외를 발생시킨다.
        response.raise_for_status()
        return response.json()
    except httpx.RequestError as exc:
        print(f"An error occurred while requesting {exc.request.url!r}.")
        return {}
    except httpx.HTTPStatusError as exc:
        print(
            f"Error response {exc.response.status_code} "
            f"while requesting {exc.request.url!r}."
        )
        return {}
    finally:
        print("수집 완료")

# 세 API를 동시에 호출하고 요청 순서대로 응답을 반환
async def collect_all() -> tuple[dict, dict, dict]:
    async with httpx.AsyncClient(follow_redirects=True) as client:
        # gather를 사용해 각 API 요청을 순차 실행하지 않고 병렬 대기한다.
        return await asyncio.gather(
            fetch(client, WEATHER_URL),
            fetch(client, COUNTRY_URL),
            fetch(client, IP_URL),
        )

## 수집 모듈을 단독 실행하면 원본 응답을 확인할 수 있음
async def main() -> None:
    weather, country, ip_info = await collect_all()

    print(weather)
    print(country)
    print(ip_info)


if __name__ == "__main__":
    asyncio.run(main())
