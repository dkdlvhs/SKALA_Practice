# ----------------------------------------------------
# 작성자 : 김규현
# 작성목적 : API에서 추출한 데이터의 Pydantic 모델 정의 (검증 요소 포함)
# 작성일 : 2026-08-03
# ----------------------------------------------------

from pydantic import BaseModel, Field

# 특정 시각의 서울 기온과 강수확률
class WeatherRecord(BaseModel):

    time: str
    temperature: float
    # 강수확률은 결측값(None)을 허용하되, 값이 있으면 0~100 범위여야 한다.
    precipitation_probability: int | None = Field(ge=0, le=100)

# 국가의 기본 정보
class CountryRecord(BaseModel):

    name: str
    capital: str
    region: str
    # 음수나 0은 유효한 인구수로 보지 않는다.
    population: int = Field(gt=0)

# IP 주소를 기반으로 조회한 지역 정보
class IPRecord(BaseModel):

    ip: str
    country: str
    city: str
    latitude: float
    longitude: float
