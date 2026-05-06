# Metricix User Guide

This guide provides instructions on how to use the Metricix UI for event injection and analytics, as well as how to interact with the developer API directly.

## 1. The Emitter (Event Injection UI)

The Emitter UI, available at **[https://metricix.mihirr.in](https://metricix.mihirr.in)**, is a tool for sending telemetry data to the Metricix engine.

*   **Authentication:**
    *   Select a preset `X-API-Key` from the dropdown (e.g., `mtx_pub_test_123`).
    *   Alternatively, choose "Custom..." to enter your own key. This isolates your data into a unique tenant space.

*   **Event Configuration:**
    *   ***Single Request Mode:***
        *   Select a predefined event type like `button_click` or `page_view`.
        *   Choose "Custom..." to define your own event type name (e.g., `video_played`).
    *   ***Multiple Requests Mode:***
        *   Click "+ Add Event" to build a sequence of up to 5 different event types.
        *   Set the desired quantity for each event in the sequence.

*   **Payload:**
    *   Enter the event details in the **Shared Payload** JSON editor. This data will be sent with every event.
    *   Click the **"Format JSON"** button to automatically clean up and validate the JSON syntax.

*   **Execution:**
    *   Click **"Send Event"** (in Single mode) or **"Send Sequential"** (in Multiple mode) to dispatch the data to the AWS engine.
    *   For multiple events, you can click **"Randomize & Send"** to dispatch them in a shuffled, chaotic order.
    *   Watch the **Live Console Log** at the bottom for real-time HTTP request/response logs from the server.

## 2. The Analytics Dashboard

The Analytics Dashboard is where you can visualize and manage the data you've sent.

*   **Loading Data:**
    *   Select your **Tenant** from the dropdown menu. This list is automatically populated with all active tenants.
    *   Enter the corresponding **API Key** and click **"Load Data"** to fetch all events for that tenant.

*   **Visualization:**
    *   The dashboard features an interactive chart powered by **Chart.js**.
    *   Toggle between **"Bar"** view to see Event Volume by Type, and **"Line"** view to see Total Activities over time.
    *   When using the Line chart, you can switch the time-binning aggregation between **"Per Hour"** and **"Per Day"**.
    *   You can zoom by scrolling with your mouse and pan by clicking and dragging. Double-click the chart to reset the zoom.

*   **Event Ledger:**
    *   A table below the chart displays the most recent 50 events for the selected tenant.
    *   It shows the exact timestamp, event type, and the raw JSON payload for each event.

*   **The Danger Zone:**
    *   The **"Purge Data"** button allows for immediate data removal.
    *   When clicked, it executes a soft-delete request (`DELETE /purge`) that instantly wipes the selected tenant's data from the active database view. This action is irreversible.

## 3. Developer API Reference

For direct integration, developers can hit the Metricix engine at `https://api.mihirr.in/api/v1`. All requests require an `X-API-Key` header for authentication.

### Endpoints

*   `GET /tenants`: Retrieves a list of all active tenants (API keys) that have sent data.
*   `POST /track`: Ingests a single telemetry event.
*   `GET /events`: Retrieves all events for the tenant specified by the `X-API-Key`.
*   `DELETE /purge`: Soft-deletes all events for the tenant specified by the `X-API-Key`.

### cURL Example: Track Event

Here is a copy-pasteable example for sending a `button_click` event.

```bash
curl -s -X POST https://api.mihirr.in/api/v1/track \
  -H "X-API-Key: mtx_pub_your_key_here" \
  -H "Content-Type: application/json" \
  -d '{
    "event_type": "button_click",
    "url": "https://myapp.com/pricing",
    "payload": {
      "button_id": "cta_upgrade",
      "user_id": "usr_a4f92"
    }
  }'
```
