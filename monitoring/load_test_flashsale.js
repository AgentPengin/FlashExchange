import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '5s', target: 20 },   // Warm-up stage: Ramp up to 20 virtual users
    { duration: '20s', target: 100 }, // High-concurrency burst: 100 concurrent VUs
    { duration: '5s', target: 0 },    // Ramp-down stage: Scale down to 0
  ],
};

export default function () {
  const userId = `user-${__VU}`;
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-User-Id': userId,
    },
  };

  // Dispatch atomic purchase request to Flash Sale API
  const res = http.post('http://host.docker.internal:8080/api/v1/flashsale/buy?itemKey=FLASH_PASS', null, params);

  // Validate expected status codes: 201 (Purchased), 429 (Rate Limited), 410 (Sold Out)
  check(res, {
    'Valid Status (201, 429, 410)': (r) => [201, 429, 410].includes(r.status),
  });

  sleep(0.05); // 50ms pacing between requests per VU
}
