/*
Test ID: TC-03
Test name: Catalog Load
Architecture: Microservices
Endpoint or flow: GET /api/products
VUs: 50
Duration: 60s
*/
import http from 'k6/http';
import { check } from 'k6';
import { buildOptions } from '../shared.js';

const CATALOG_BASE_URL = __ENV.CATALOG_BASE_URL || 'http://localhost:8085';
export const options = buildOptions(50, '60s', {
  http_req_duration: ['p(95)<1500', 'p(99)<2500'],
});

export default function () {
  const response = http.get(`${CATALOG_BASE_URL}/api/products`, { tags: { name: 'catalog-load' } });
  check(response, {
    'catalog load status 200': (r) => r.status === 200,
  });
}
