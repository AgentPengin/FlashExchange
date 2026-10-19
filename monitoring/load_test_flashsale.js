import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '5s', target: 20 }, 
        { duration: '20s', target: 100 }, 
        { duration: '5s', target: 0 },   
    ],
};

export default function () {
    const userId = `user-${__VU}}`;
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-User-Id': userId,
        },
    };
    
    const res = http.post('http://host.docker.internal:8080/api/v1/flashsale/buy?itemKey=FLASH_PASS', null, params);

    check(res, {
        'Valid Status (201, 429, 410)': (r) =>  [201, 429, 410].includes(r.status),
    });

    sleep(0.05);
};

