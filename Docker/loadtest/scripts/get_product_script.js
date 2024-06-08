import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  insecureSkipTLSVerify: true,
};

export default function () {
  const res = http.get("https://host.docker.internal/api/v1/products/all", {headers: {Accepts: "application/json"}});
  check(res, { "status is 200": (r) => r.status === 200 });
  sleep(.300);
}
