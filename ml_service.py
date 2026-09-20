from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from pydantic import BaseModel, ValidationError, Field
from typing import List, Union, Optional
import numpy as np

app = FastAPI(title="Legal Metrology Risk Prediction Engine")


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    body = await request.body()
    print(f"Invalid /predict-heatmaps payload: {body[:500]!r}; errors={exc.errors()}")
    return JSONResponse(status_code=422, content={"detail": exc.errors()})


class InspectionRecord(BaseModel):
    latitude: float
    longitude: float
    past_violations: int = Field(default=1, validation_alias="totalViolationsCount")
    months_since_inspection: int = 6
    merchant_density: int = 10
    baseline_risk: Optional[float] = Field(default=None, validation_alias="baselineRiskScore")

    class Config:
        populate_by_name = True


def calculate_spatial_density(coords: np.ndarray, radius_deg: float = 0.015) -> np.ndarray:
    n_points = len(coords)
    if n_points == 0:
        return np.array([])
    
    density_scores = np.zeros(n_points)
    for i in range(n_points):
        distances = np.sqrt(np.sum((coords - coords[i]) ** 2, axis=1))
        density_scores[i] = np.sum(distances <= radius_deg) - 1
    return density_scores


@app.post("/predict-heatmaps")
@app.get("/predict-heatmaps")
def generate_heatmap_clusters(records: Union[List[dict], dict, None] = None):
    # Fallback store coordinates if no records are provided
    if not records:
        records = [
            {"latitude": 19.0865, "longitude": 72.8890, "totalViolationsCount": 5, "baselineRiskScore": 0.92},
            {"latitude": 19.0650, "longitude": 72.8680, "totalViolationsCount": 3, "baselineRiskScore": 0.88},
            {"latitude": 19.0810, "longitude": 72.9080, "totalViolationsCount": 0, "baselineRiskScore": 0.45},
            {"latitude": 19.0550, "longitude": 72.8350, "totalViolationsCount": 2, "baselineRiskScore": 0.76}
        ]

    if isinstance(records, dict):
        records = [records]

    valid_records: List[InspectionRecord] = []
    for raw_record in records:
        try:
            valid_records.append(InspectionRecord.model_validate(raw_record))
        except ValidationError as exc:
            print(f"Skipping invalid inspection record: {raw_record!r}; errors={exc.errors()}")

    if not valid_records:
        return []

    coordinates = [[r.latitude, r.longitude] for r in valid_records]
    coords_arr = np.array(coordinates)
    
    density_scores = calculate_spatial_density(coords_arr, radius_deg=0.015)

    heatmap_data = []
    for i, record in enumerate(valid_records):
        # Base weight derived from baseline risk or calculated risk factors
        if record.baseline_risk is not None:
            base_risk = record.baseline_risk
        else:
            violation_weight = min(1.0, record.past_violations * 0.2)
            recency_weight = min(1.0, record.months_since_inspection * 0.05)
            cluster_boost = 0.2 if density_scores[i] >= 1 else 0.0
            base_risk = round(0.4 * violation_weight + 0.4 * recency_weight + cluster_boost, 2)

        final_weight = min(1.0, max(0.4, base_risk + (record.past_violations * 0.05)))

        # 1. Primary heat anchor centered directly on venue coordinates
        heatmap_data.append([
            record.latitude,
            record.longitude,
            round(final_weight, 2)
        ])

        # 2. Gaussian spatial cluster spread around high-risk venues (weight > 0.60)
        if final_weight > 0.60:
            num_cluster_points = int(final_weight * 6)
            lat_offsets = np.random.normal(0, 0.0012, num_cluster_points)
            lng_offsets = np.random.normal(0, 0.0012, num_cluster_points)

            for d_lat, d_lng in zip(lat_offsets, lng_offsets):
                sub_weight = round(final_weight * np.random.uniform(0.6, 0.85), 2)
                heatmap_data.append([
                    round(record.latitude + d_lat, 6),
                    round(record.longitude + d_lng, 6),
                    sub_weight
                ])

    return heatmap_data


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)