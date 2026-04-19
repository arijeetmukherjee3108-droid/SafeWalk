# 🛡️ SafeHer

> **AI-Powered Women's Safety Navigation & Incident Reporting Platform**

SafeHer is a comprehensive women's safety application that combines **Machine Learning**, **Blockchain**, and **Real-Time Navigation** to help women navigate cities safely and report incidents with tamper-proof evidence.

---

## ✨ Key Features

### 🗺️ AI-Powered Safe Route Navigation
- **Three-Route Comparison System** — Queries Mapbox with Walking, Cycling, and Driving profiles to generate three distinct real-world paths
- **ML Risk Scoring** — Each route is independently evaluated by our FastAPI ML model that analyzes neighborhood safety based on crime data, lighting, and crowd density
- **Color-Coded Visualization** — Routes are rendered in **Green** (Safe), **Yellow** (Moderate), and **Red** (High Risk) directly on the map
- **Interactive Selection** — Tap any route on the map or its corresponding card to select it for navigation
- **Collapsible Search UI** — Input panel auto-collapses when routes are displayed to maximize map visibility

### 🚨 SOS Emergency System
- **3-Second Hold to Trigger** — Press and hold the SOS button with a visual progress ring
- **Multi-Channel Alerts** — Simultaneously sends SMS alerts to registered guardians and in-app notifications to nearby community members
- **Police Notification** — Auto-logs emergency alerts to the nearest police station
- **Location Sharing** — Shares real-time Google Maps location link with all recipients

### 📋 Blockchain-Secured Incident Reporting
- **Immutable Evidence** — Reports are permanently stored on the **Ethereum Sepolia** blockchain
- **IPFS Storage** — Evidence images are uploaded to **Pinata/IPFS** for decentralized storage
- **Multipart Submission** — Supports category, description, suspect name, GPS coordinates, and photo evidence
- **Dual Storage** — Reports are saved to both Firestore (for querying) and blockchain (for immutability)
- **Verification Dialog** — Shows Etherscan TX link and IPFS URL after each submission

### 🔔 Alert History
- **Guardian Alerts** — View all SOS alerts received as a guardian
- **Community Alerts** — View nearby emergency alerts received as a community member
- **Real-Time Updates** — Powered by Firestore snapshot listeners
- **Quick Action** — Tap any alert to open the victim's location in Google Maps

### 👤 User Profile & Guardian Management
- **Google Sign-In** — Seamless authentication via Firebase Auth
- **Guardian System** — Add/remove emergency contacts who receive SOS alerts
- **Community Mode** — Opt-in to receive nearby emergency alerts from other users
- **Anonymous Reporting** — Cryptographic identity protection for reporters

---

## 🏗️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Frontend** | Kotlin, Android SDK, ViewBinding |
| **Maps** | OSMDroid (OpenStreetMap) |
| **Routing** | Mapbox Directions API (Walking/Cycling/Driving profiles) |
| **ML Backend** | FastAPI + Python ML Model (deployed on Render) |
| **Blockchain** | Ethereum Sepolia Testnet, Solidity Smart Contracts |
| **File Storage** | Pinata (IPFS) for evidence images |
| **Database** | Firebase Firestore |
| **Authentication** | Firebase Auth (Google Sign-In) |
| **Notifications** | Firebase Cloud Messaging (FCM) |
| **SMS** | Android SmsManager |
| **Networking** | Retrofit2 + OkHttp + Gson |
| **Architecture** | MVVM + Navigation Component + SafeArgs |

---

## 📁 Project Structure

```
app/src/main/java/com/example/safewalk/
├── data/
│   ├── model/
│   │   ├── MLModels.kt            # ML prediction request/response
│   │   └── Report.kt              # Incident report data model
│   └── network/
│       ├── MLApiService.kt        # ML prediction API (POST /predict)
│       ├── BlockchainApiService.kt # Web3 API (POST /api/reports/submit)
│       └── RetrofitClient.kt      # Retrofit instances (ML + Blockchain)
├── service/
│   └── MyFirebaseMessagingService.kt  # FCM push notifications
├── ui/
│   ├── alerts/
│   │   └── AlertsFragment.kt      # Alert history (Guardian + Community)
│   ├── auth/
│   │   └── AuthActivity.kt        # Google Sign-In flow
│   ├── dialogs/
│   │   ├── GuardianSheet.kt       # Add/manage guardians bottom sheet
│   │   └── SOSDialog.kt           # Emergency SOS trigger dialog
│   ├── home/
│   │   └── HomeFragment.kt        # Dashboard with quick actions
│   ├── map/
│   │   └── MapFragment.kt         # Map + 3-route comparison system
│   ├── profile/
│   │   └── ProfileFragment.kt     # User profile & settings
│   └── report/
│       ├── ReportFragment.kt      # Incident report form
│       ├── ReportPreviewFragment.kt # Preview + blockchain submission
│       ├── ReportsListFragment.kt  # All/My Reports with filters
│       ├── ReportAdapter.kt        # Report card RecyclerView adapter
│       └── ReportViewModel.kt      # Report form state management
└── MainActivity.kt                 # Navigation host + bottom nav
```

---

## 🔗 API Endpoints

**Base URL:** `https://ml-api-6v4v.onrender.com`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/predict` | ML route safety prediction (JSON) |
| `POST` | `/api/reports/submit` | Submit incident to blockchain (multipart/form-data) |
| `POST` | `/api/reports/resolve` | Resolve a blockchain report (JSON) |

### Submit Report Fields
| Field | Type | Required |
|-------|------|----------|
| `lat` | number | ✅ |
| `lng` | number | ✅ |
| `incident_type` | string | ✅ |
| `description` | string | ❌ |
| `suspect_name` | string | ❌ |
| `evidence_file` | file | ✅ |

### Submit Report Response
```json
{
  "status": "success",
  "blockchain_receipt": "https://sepolia.etherscan.io/tx/0x...",
  "ipfs_url": "https://gateway.pinata.cloud/ipfs/Qm...",
  "resolution_secret": "a8008120aaed...",
  "message": "Report successfully filed on-chain and cached for ML mapping."
}
```

---

## 🚀 Setup & Installation

### Prerequisites
- Android Studio (Hedgehog or later)
- JDK 17+
- Firebase project with Firestore, Auth, and FCM enabled

### Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/SafeHer.git
   cd SafeHer
   ```

2. **Firebase Setup**
   - Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
   - Enable **Google Sign-In** in Authentication
   - Enable **Cloud Firestore**
   - Download `google-services.json` and place it in `app/`

3. **API Keys**
   - Add your Mapbox access token in `local.properties`:
     ```properties
     MAPBOX_ACCESS_TOKEN=pk.your_mapbox_token_here
     ```

4. **Build & Run**
   ```bash
   ./gradlew assembleDebug
   ```
   Or open in Android Studio and click ▶️ Run.

---

## 🔒 Security Features

- **Blockchain Immutability** — Reports cannot be altered or deleted once submitted to Ethereum
- **IPFS Evidence** — Evidence images are stored on a decentralized network, preventing censorship
- **Anonymous Authentication** — Cryptographic identity protection for reporters
- **End-to-End SMS** — Emergency alerts sent via device SMS for reliability when internet is unavailable
- **Resolution Secret** — Only the original reporter can resolve/close their report using a unique secret key

---

## 🏆 Built For

**Hackofiesta 2026** — Women's Safety Track

---

## 📄 License

This project is built for demonstration and hackathon purposes.
