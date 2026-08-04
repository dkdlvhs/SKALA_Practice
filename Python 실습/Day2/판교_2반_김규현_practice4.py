# ----------------------------------------------------
# 작성자 : 김규현
# 작성목적 : 시각화, 통계 검정, sklearn Pipeline, Plotly 실습
# 작성일 : 2026-08-04
# ----------------------------------------------------


from pathlib import Path

import joblib
import matplotlib

matplotlib.use("Agg")  # GUI가 없는 환경에서도 그래프 저장 가능

import matplotlib.pyplot as plt
from matplotlib import font_manager
import pandas as pd
import plotly.express as px
import seaborn as sns
from scipy.stats import chi2_contingency, ttest_ind
from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.metrics import mean_absolute_error, r2_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, PolynomialFeatures, StandardScaler
from sklearn.linear_model import LinearRegression


BASE_DIR = Path(__file__).resolve().parent
CSV_PATH = BASE_DIR / "sales_100k.csv"
EDA_PATH = BASE_DIR / "practice4_eda.png"
MODEL_PATH = BASE_DIR / "practice4_sales_pipeline.joblib"
PLOTLY_PATH = BASE_DIR / "practice4_region_category_sales.html"

# 데이터 로드 및 전처리
def load_data() -> pd.DataFrame:
    df = pd.read_csv(CSV_PATH, encoding="utf-8-sig")
    # 날짜 컬럼을 datetime으로 변환
    df["order_date"] = pd.to_datetime(df["order_date"], errors="coerce")

    # 숫자형 컬럼을 numeric으로 변환
    for column in ["quantity", "unit_price", "customer_age", "amount"]:
        df[column] = pd.to_numeric(df[column], errors="coerce")
    return df

# 1) EDA 시각화 4종 (2X2 서브플롯)
def create_eda(df: pd.DataFrame) -> None:
    sns.set_theme(style="whitegrid")
    
    # 한글 폰트 설정
    for font_name in ["AppleGothic", "Malgun Gothic", "NanumGothic"]:
        try:
            font_manager.findfont(font_name, fallback_to_default=False)
            plt.rcParams["font.family"] = font_name
            break
        except ValueError:
            continue
    plt.rcParams["axes.unicode_minus"] = False

    # 2x2 서브플롯 생성
    fig, axes = plt.subplots(2, 2, figsize=(15, 10))

    # 히스토그램 (매출액 분포)
    sns.histplot(df["amount"].dropna(), bins=40, kde=True, ax=axes[0, 0])
    axes[0, 0].set(title="매출액 분포", xlabel="매출액", ylabel="빈도")

    # 박스플롯 (지역별 매출액 분포)
    sns.boxplot(data=df, x="region", y="amount", ax=axes[0, 1])
    axes[0, 1].set(title="지역별 매출액 박스플롯", xlabel="지역", ylabel="매출액")
    axes[0, 1].tick_params(axis="x", rotation=30)

    # 월별 총매출 추이
    monthly = (
        df.dropna(subset=["order_date", "amount"])
        .set_index("order_date")["amount"]
        .resample("MS")
        .sum()
    )
    axes[1, 0].plot(monthly.index, monthly.values, marker="o", markersize=3)
    axes[1, 0].set(title="월별 총매출 추이", xlabel="월", ylabel="총매출")
    axes[1, 0].tick_params(axis="x", rotation=30)

    # 상관계수 히트맵 (정량 변수 대상)
    numeric_columns = ["quantity", "unit_price", "customer_age", "amount"]
    corr = df[numeric_columns].corr()
    sns.heatmap(corr, annot=True, fmt=".2f", cmap="RdBu_r", center=0,
                square=True, ax=axes[1, 1])
    axes[1, 1].set_title("수치형 변수 상관계수")

    # 레이아웃 조정 및 저장
    fig.tight_layout()
    fig.savefig(EDA_PATH, dpi=150, bbox_inches="tight")
    plt.close(fig)

# 2) 통계 검정 : t-test + 카이제곱검정
def run_statistical_tests(df: pd.DataFrame) -> None:
    """서울-vs-부산 매출액 Welch t-검정과 지역-카테고리 독립성 검정."""

    # 서울, 부산 매출액 데이터 추출 및 결측치 제거
    seoul = df.loc[df["region"] == "서울", "amount"].dropna()
    busan = df.loc[df["region"] == "부산", "amount"].dropna()

    # t-test
    t_stat, t_pvalue = ttest_ind(seoul, busan, equal_var=False)

    # 지역, 카테고리 분할표(범주형 데이터를 대상으로 데이터 개수 파악할 떄 사용)
    contingency = pd.crosstab(df["region"], df["category"])

    # 카이제곱검정
    chi2, chi_pvalue, dof, expected = chi2_contingency(contingency)

    print("\n=== 2) 통계 검정 (유의수준 0.05) ===")
    print(f"서울 평균: {seoul.mean():,.2f}, 부산 평균: {busan.mean():,.2f}")
    print(f"Welch t-검정: t={t_stat:.4f}, p={t_pvalue:.6f}")
    print(
        "결론: 서울과 부산의 평균 매출액에 유의한 차이가 "
        + ("있습" if t_pvalue < 0.05 else "없음")
    )
    print(f"\n카이제곱 검정: chi2={chi2:.4f}, dof={dof}, p={chi_pvalue:.6f}")
    print(
        "결론: 지역과 카테고리 간에 유의한 관련이 "
        + ("있음" if chi_pvalue < 0.05 else "없음(독립으로 봄)")
    )

# 3) sklearn Pipeline 구성 + 저장
def train_save_reload_pipeline(df: pd.DataFrame) -> Pipeline:

    # 수치형 컬럼
    numeric_features = ["quantity", "unit_price", "customer_age"]

    # 범주형 컬럼
    categorical_features = [
        "region", "category", "payment_method", "customer_gender"
    ]

    # 예측 대상(매출액) 결측치 제거
    model_df = df.dropna(subset=["amount"])

    # X: 모델이 입력받을 특성, y: 모델이 맞혀야 할 매출액
    raw_features = numeric_features + categorical_features
    X, y = model_df[raw_features], model_df["amount"]

    # 수치형 컬럼용 전처리 Pipeline
    # 1. imputer: 결측치를 해당 컬럼의 중앙값으로 대체
    # 2. interactions: quantity * unit_price 같은 2차항과 상호작용 특성 생성
    # 3. scaler: 컬럼들의 크기를 표준화하여 비슷한 스케일로 변환
    numeric_pipeline = Pipeline([
        ("imputer", SimpleImputer(strategy="median")),
        ("interactions", PolynomialFeatures(degree=2, include_bias=False)),
        ("scaler", StandardScaler()),
    ])

    # 범주형 컬럼용 전처리 Pipeline
    # 1. imputer: 결측치를 가장 자주 등장한 값으로 대체
    # 2. onehot: '서울', '부산' 같은 문자를 모델이 처리할 수 있는 0/1 컬럼으로 변환
    # handle_unknown="ignore": 예측 시 학습에 없던 새 범주가 나와도 오류를 발생시키지 않음
    categorical_pipeline = Pipeline([
        ("imputer", SimpleImputer(strategy="most_frequent")),
        ("onehot", OneHotEncoder(handle_unknown="ignore")),
    ])

    # ColumnTransformer로 컬럼별 전처리 방법을 지정
    preprocessor = ColumnTransformer([
        ("numeric", numeric_pipeline, numeric_features),
        ("categorical", categorical_pipeline, categorical_features),
    ])

    # 최종 Pipeline: 원본 데이터 -> 전처리 -> 선형회귀 모델 순서로 실행
    # 전처리 과정도 모델과 함께 저장되므로 예측할 때 원본 형태의 X를 바로 넣을 수 있다.
    pipeline = Pipeline([
        ("preprocessor", preprocessor),
        ("model", LinearRegression()),
    ])

    # 전체 데이터의 80%는 학습용, 20%는 평가용으로 분리
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )

    # fit(): 학습 데이터의 전처리 기준을 계산하고 선형회귀 모델을 학습
    pipeline.fit(X_train, y_train)

    # predict(): 평가 데이터에 같은 전처리를 적용한 뒤 매출액을 예측
    prediction = pipeline.predict(X_test)

    print("\n=== 3) sklearn Pipeline 평가 ===")
    # MAE: 실제값과 예측값 차이의 절대값 평균. 작을수록 좋다.
    print(f"MAE: {mean_absolute_error(y_test, prediction):,.2f}")
    # R²: 모델이 매출액의 변화를 설명하는 비율. 보통 1에 가까울수록 좋다.
    print(f"R² : {r2_score(y_test, prediction):.4f}")

    # 전처리기와 학습된 모델을 하나의 파일로 저장
    joblib.dump(pipeline, MODEL_PATH)

    # 저장한 Pipeline을 다시 불러와 바로 예측할 수 있는지 확인
    loaded_pipeline = joblib.load(MODEL_PATH)
    sample_prediction = loaded_pipeline.predict(X_test.head(5))
    print("재로딩 모델 예측(5건):", sample_prediction.round(2).tolist())

    # 다른 함수에서도 사용할 수 있도록 재로딩한 Pipeline을 반환
    return loaded_pipeline

# 4) Ploty 인터랙티브 차트 저장
def create_plotly_chart(df: pd.DataFrame) -> None:

    # 결측치 제거 및 집계
    sales = (
        df.dropna(subset=["region", "category", "amount"])
        .groupby(["region", "category"], as_index=False)["amount"]
        .sum()
        .rename(columns={"amount": "total_sales"})
    )

    # Ploty Express 막대 차트 생성
    fig = px.bar(
        sales,
        x="region",
        y="total_sales",
        color="category",
        barmode="group",
        title="지역·카테고리별 총매출",
        labels={"region": "지역", "total_sales": "총매출", "category": "카테고리"},
    )

    # 레이아웃 조정 및 html로 저장
    fig.update_layout(hovermode="x unified")
    fig.write_html(PLOTLY_PATH, include_plotlyjs=True)


def main() -> None:
    df = load_data()
    print("=== 1) EDA 시각화 ===")
    print(f"데이터 크기: {df.shape}, 결측치 합계: {df.isna().sum().sum():,}")
    create_eda(df)
    print(f"EDA 저장: {EDA_PATH.name}")

    run_statistical_tests(df)
    train_save_reload_pipeline(df)
    print(f"Pipeline 저장: {MODEL_PATH.name}")

    create_plotly_chart(df)
    print("\n=== 4) Plotly 차트 ===")
    print(f"HTML 저장: {PLOTLY_PATH.name}")


if __name__ == "__main__":
    main()
