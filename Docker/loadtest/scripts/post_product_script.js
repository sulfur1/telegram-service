import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
    vus: 10,
    duration: '1m',
    insecureSkipTLSVerify: true,
};

const data = JSON.parse(open('/scripts/products.json'));

export default function () {
    for (let i = 0; i < data.length; i++) {
        let res = http.post('https://host.docker.internal/api/v1/products', JSON.stringify(data[i]), {
            headers: { 'Content-Type': 'application/json' },
        });
        check(res, { "status is 201": (r) => r.status === 201 });
        sleep(1);
    }
}