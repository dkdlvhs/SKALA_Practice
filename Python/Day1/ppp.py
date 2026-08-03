# Pydantic v2 실전
from pydantic import BaseModel, Field
from typing import Optional

class SalesRecord(BaseModel):
    date: str
    region: str
    amount: float = Field(gt=0, description=
    '양수')
    category: Optional[str] = None
# 검증 성공
r = SalesRecord(**{'date':'2024','region':'서울','amount':1500})

r .model_dump() # dict 변환
# 검증 실패
try:
    SalesRecord(date='', region='서울', amount=-100)
except Exception as e:
    print(e)