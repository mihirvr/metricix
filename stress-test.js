import http from 'k6/http';
import { check, sleep } from 'k6';

// --- THE STRESS TEST CONFIGURATION ---
// --- THE CONSTANT ARRIVAL RATE CONFIGURATION ---
export const options = {
    scenarios: {
        constant_request_rate: {
            executor: 'constant-arrival-rate',
            rate: 1000, // 1000 requests per...
            timeUnit: '1s', // ... second
            duration: '1m', // Hold this exact rate for 1 minute
            preAllocatedVUs: 100, // Give k6 a pool of 100 workers to use
            maxVUs: 500, // Allow up to 500 workers if needed
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<15'], 
        http_req_failed: ['rate<0.01'],   
    },
};

// --- THE PAYLOAD GENERATOR ---
export default function () {
    const url = 'http://localhost:8080/api/v1/track';
    
    // We randomize the payload slightly to ensure the JVM and Redis aren't just caching a static string
    const payload = JSON.stringify({
        event_type: 'stress_test_click',
        url: `https://myapp.com/page/${Math.floor(Math.random() * 100)}`,
        payload: {
            user_id: `usr_${Math.floor(Math.random() * 10000)}`,
            action: 'clicked_button',
            timestamp: new Date().toISOString()
        }
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-API-Key': 'mtx_pub_stress_test', // The API key for our test tenant
        },
    };

    // Fire the POST request!
    const res = http.post(url, payload, params);

    // Verify the response was our immediate 202 Accepted
    check(res, {
        'is status 202': (r) => r.status === 202,
    });

    // A tiny 10ms sleep between requests per user so we don't accidentally DDoS our own network stack
    sleep(0.01); 
}