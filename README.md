# Scanticipate

An end-to-end compliance analysis and geospatial inspector dispatch system built for **Legal Metrology Rules, 2011 (Packaged Commodities)**.

The application utilizes **Google Gemini Multimodal VLM** to automatically audit packaged goods packaging photos for mandatory statutory declarations (MRP, net quantity, manufacturing date, generic name, manufacturer details, consumer care contact), alongside an administrative geospatial dashboard featuring dynamic FastAPI spatial risk heatmaps, store-anchored pin markers, distance-sorted inspector routing navigation, and multi-product inspection session batching.

---

## Key Features

- **AI-Powered VLM Packaging Audits:** Automated Legal Metrology 2011 compliance verification using Gemini model with exponential backoff retries and model failover.
- **Multi-Image & Multi-Product Session Support:** Enables field inspectors to capture multiple photos per product (front, back, sides) and batch multiple products per site submission.
- **Interactive Admin Leaflet Map Dashboard:** Features real-time ML risk heatmaps generated dynamically via FastAPI, custom store markers with hover tooltips, and interactive audit history modals.
- **Inspector Navigation Routing:** Built-in Leaflet Routing Machine providing turn-by-turn navigation paths connecting inspectors directly to assigned retail store venues.
- **Inspector Task Request & Onboarding Workflows:** Dynamic request/approval task dispatching with Haversine proximity-based inspector sorting, ad-hoc self-initiated audits, and role-based session authentication.

---

## Tech Stack

- **Backend:** Java 26, Spring Boot 4, Spring Data JPA, H2 Database
- **AI Engine:** Google Gemini VLM API (`gemini-3.8-flash` / `gemini-3.7-flash`/ `gemini-3.6-flash`)
- **Spatial ML Microservice:** Python 3.10+, FastAPI, NumPy, Uvicorn
- **Frontend:** Vanilla HTML5, CSS3, JavaScript (ES15), Leaflet.js, Leaflet.heat, Leaflet Routing Machine

## Getting Started

### Prerequisites

- **Java JDK 17** or higher
- **Maven 3.8+** (or use the included `./mvnw` wrapper)
- **Python 3.10+** and `pip`
- **Google Gemini API Key** (obtainable via [Google AI Studio](https://aistudio.google.com/))

---

## Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/hexa-sih/Scanticipate.git
```

### 2. Enter the root folder

```bash
cd Scanticipate
```

### 3. Set Google Gemini API Key

Windows (Powershell)

```bash
$env:X_GOOG_API_KEY="---YOUR_API_KEY---"
```

Windows (Command Prompt)

```bash
set X_GOOG_API_KEY = ---YOUR_API_KEY---
```

### 4. Launch the Application

```bash
.\mvnw spring-boot:run
```

### 5. Install Python Dependencies

```bash
pip install fastapi uvicorn numpy requests
```

### 6. Run Fast API Risk Service

```bash
python ml_service.py
```

## Application Portals

**Login Panel**: http://localhost:8080/index.html

**Database Console**: http://localhost:8080/h2-console

**Note**: The login IDs, Passwords and Store Details are provided in the "USER_AND_STORE_DATA.txt" file.

---

**Developed for Smart India Hackathon (SIH 26034) - Legal Metrology Rules Compliance Enforcement.**
