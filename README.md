# group50-ridehail-payment-service
Handles payment processing, fare calculation, refunds, and payment reconciliation


| HTTP Method | Endpoint                           | Description          |
| ----------- | ---------------------------------- | -------------------- |
| `GET`       | `/api/v1/payments`                 | Fetch all payments   |
| `GET`       | `/api/v1/payments/{id}`            | Get payment by ID    |
| `POST`      | `/api/v1/payments`                 | Create a new payment |
| `GET`       | `/api/v1/payments/rider/{riderId}` | Payments by Rider ID |
| `GET`       | `/api/v1/payments/trip/{tripId}`   | Payments by Trip ID  |



group50-payment-service

Build:
./gradlew clean build

Run:
java -jar build/libs/group50-payment-service-1.0.0.jar

Service listens on port 4100 (configurable)
