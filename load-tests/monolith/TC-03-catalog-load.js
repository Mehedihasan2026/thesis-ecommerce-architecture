/*
Test ID: TC-03
Test name: Catalog Load
Architecture: Monolith
Endpoint or flow: GET /api/products
VUs: 50
Duration: 60s
*/
import http from 'k6/http';
import { check } from 'k6';
import { buildOptions } from '../shared.js';

const BASE_URL = __ENV.MONOLITH_BASE_URL || 'http://localhost:8083';
export const options = buildOptions(50, '60s', {
  http_req_duration: ['p(95)<1500', 'p(99)<2500'],
});

export default function () {
  const response = http.get(`${BASE_URL}/api/products`, { tags: { name: 'catalog-load' } });
  check(response, {
    'catalog load status 200': (r) => r.status === 200,
  });
}
