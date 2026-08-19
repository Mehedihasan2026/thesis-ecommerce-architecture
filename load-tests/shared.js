import http from 'k6/http';
import { check } from 'k6';
import { randomSeed } from 'k6';
import exec from 'k6/execution';

randomSeed(12345);

export function buildOptions(vus, duration, extraThresholds = {}) {
  return {
    vus,
    duration,
    thresholds: {
      http_req_failed: ['rate<0.05'],
      http_req_duration: ['p(95)<2000', 'p(99)<3000'],
      checks: ['rate>0.95'],
      ...extraThresholds,
    },
  };
}

export function buildFailureOptions(vus, duration, expectedStatus) {
  return {
    vus,
    duration,
    thresholds: {
      checks: ['rate>0.95'],
      [`http_req_duration{expected_status:${expectedStatus}}`]: ['p(95)<3000'],
    },
  };
}

export function uniqueCustomerId(prefix) {
  return `${prefix}-${exec.vu.idInTest}-${exec.scenario.iterationInTest}-${Date.now()}`;
}

export function fetchFirstProductId(baseUrl) {
  const response = http.get(`${baseUrl}/api/products`, {
    tags: { name: 'list-products' },
  });

  check(response, {
    'product list returns 200': (r) => r.status === 200,
    'product list is not empty': (r) => {
      const body = JSON.parse(r.body);
      return Array.isArray(body) && body.length > 0;
    },
  });

  const products = JSON.parse(response.body);
  return products[0].id;
}

export function addItemToCart(cartBaseUrl, customerId, productId, quantity = 1) {
  const response = http.post(
    `${cartBaseUrl}/api/carts/${customerId}/items`,
    JSON.stringify({ productId, quantity }),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'add-to-cart' },
    }
  );

  check(response, {
    'add to cart returns 201': (r) => r.status === 201,
  });

  return response;
}

export function checkout(orderBaseUrl, customerId, query = '', expectedStatus = 200, extraTags = {}) {
  const suffix = query ? `?${query}` : '';
  const response = http.post(`${orderBaseUrl}/api/orders/checkout/${customerId}${suffix}`, null, {
    tags: {
      name: 'checkout',
      expected_status: String(expectedStatus),
      ...extraTags,
    },
  });

  check(response, {
    [`checkout returns ${expectedStatus}`]: (r) => r.status === expectedStatus,
  });

  return response;
}
