/*
Test ID: TC-01
Test name: Catalog Baseline
Architecture: Monolith
Endpoint or flow: GET /api/products
VUs: 10
Duration: 60s
*/
import http from 'k6/http';
import { check } from 'k6';
import { buildOptions } from '../shared.js';

const BASE_URL = __ENV.MONOLITH_BASE_URL || 'http://localhost:8083';
export const options = buildOptions(10, '60s', {
  http_req_duration: ['p(95)<1000', 'p(99)<1500'],
});

export default function () {
  const response = http.get(`${BASE_URL}/api/products`, { tags: { name: 'catalog-baseline' } });
  check(response, {
    'catalog baseline status 200': (r) => r.status === 200,
  });
}
