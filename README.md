# Payment Service

The Payment Service handles trip payment charging, refunding, and payment lookup for the ride-hailing platform.

It exposes a small set of domain-specific endpoints (not CRUD), persists payment records in PostgreSQL, and supports JSON logging with correlation IDs.

**Tech Stack:**
Java 25 • Spring Boot 3.5+ • Gradle • PostgreSQL • Docker • Docker Compose
JSON Logging • Correlation ID Filter • Resilience4j Rate Limiting

---

# Quickstart — Run with Docker

```bash
# 0) Clean previous containers (optional)
docker compose down -v
```

```bash
# 1) Build Payment Service JAR
cd payment-service
./gradlew clean bootJar
```

```bash
# 2) Build Docker image (fresh)
docker build --no-cache -t rhf/payment-service:latest .
cd ..
```

```bash
# 3) Start Payment Service + PostgreSQL
docker compose up -d
```

```bash
# 4) Health check
curl http://localhost:9084/actuator/health
```

---

# Exposed Ports

| Component       | Port | Description                |
| --------------- |------|----------------------------|
| Payment Service | 9084 | REST API                   |
| PostgreSQL      | 5434 | Container 5432 → Host 5434 |

---

# API Endpoints

The Payment Service exposes three domain-specific operations:

```
POST    /v1/payments/charge        Charge a completed trip
PATCH   /v1/payments/{id}/refund   Refund a successful payment
GET     /v1/payments/trip/{tripId} Get payment info by trip ID
```

Rate limiting is applied using Resilience4j:

* `paymentChargeLimiter`
* `paymentRefundLimiter`

---

# Endpoint Details and Sample Calls

## 1. Charge a Trip

`POST /v1/payments/charge`

Allowed **only for COMPLETED trips**.

### Request Body

```json
{
  "trip_id": 123,
  "amount": 19.50,
  "method": "CARD",
  "reference": "txn123",
  "status": "COMPLETED"
}
```

### Example

```bash
curl -X POST http://localhost:9084/v1/payments/charge \
  -H "Content-Type: application/json" \
  -d '{"trip_id":123,"amount":19.50,"status":"COMPLETED"}'
```

### Success Response

```json
{
  "status": "SUCCESS",
  "payment_id": 10,
  "trip_id": 123,
  "amount": 19.50,
  "method": "CARD"
}
```

### Validation Responses

Trip not completed:

```json
{
  "status": "FAILED",
  "message": "Payment allowed only for COMPLETED trips"
}
```

Rate limited:

```json
{
  "error": "Rate limit exceeded"
}
```

---

## 2. Refund a Payment

`PATCH /v1/payments/{id}/refund`

Only successful payments may be refunded.

### Example

```bash
curl -X PATCH http://localhost:9084/v1/payments/10/refund
```

### Success

```json
{
  "status": "REFUNDED",
  "payment_id": 10
}
```

### Errors

Payment not found:

```json
{
  "error": "Payment not found"
}
```

Invalid state:

```json
{
  "status": "CREATED",
  "message": "Only successful payments can be refunded"
}
```

Rate limited:

```json
{
  "error": "Rate limit exceeded"
}
```

---

## 3. Get Payment by Trip ID

`GET /v1/payments/trip/{tripId}`

### Example

```bash
curl http://localhost:9084/v1/payments/trip/123
```

### Response

```json
{
  "payment_id": 10,
  "trip_id": 123,
  "amount": 19.50,
  "method": "CARD",
  "status": "SUCCESS"
}
```

No record:

```json
{
  "error": "No payment found for this trip"
}
```

---

# JSON Logging and Correlation ID

The service uses a correlation ID filter to ensure all logs for a request share a common ID.

### Behavior:

* Reads inbound `X-Correlation-Id`
* Generates a new UUID if missing
* Stores ID in MDC for logging
* Returns ID back in response headers

### Example Log

```json
{
  "timestamp": "2025-02-11T12:10:05Z",
  "level": "INFO",
  "correlationId": "c36d148c-8657-45d6-b0e6-0f3d87751f8b",
  "service": "payment-service",
  "method": "POST",
  "path": "/v1/payments/charge",
  "status": 201,
  "durationMs": 44
}
```

---

# Database

### Schema

Automatically loaded via:

```
payment-service/src/main/resources/rhfd_payments.csv
```

### Example Table Fields

* payment_id
* trip_id
* method
* amount
* status
* reference
* created_at
* updated_at

### Seed Data

If present:

```
payment-service/src/main/resources/payments.csv
```

---

# Docker Compose Commands

```bash
docker compose up -d                       # start containers
docker compose down -v                     # stop + remove volumes
docker compose logs -f payment-service     # follow logs
docker exec -it payment-db psql -U payment -d paymentdb   # database shell
```

---


