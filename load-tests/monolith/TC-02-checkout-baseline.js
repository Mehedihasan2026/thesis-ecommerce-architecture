/*
Test ID: TC-02
Test name: Checkout Baseline
Architecture: Monolith
Endpoint or flow: add to cart and POST /api/orders/checkout/{customerId}
VUs: 5
Duration: 60s
*/
import { buildOptions, uniqueCustomerId, fetchFirstProductId, addItemToCart, checkout } from '../shared.js';

const BASE_URL = __ENV.MONOLITH_BASE_URL || 'http://localhost:8083';
export const options = buildOptions(5, '60s', {
  http_req_duration: ['p(95)<2000', 'p(99)<3000'],
});

export default function () {
  const customerId = uniqueCustomerId('mono-checkout-baseline');
  const productId = fetchFirstProductId(BASE_URL);
  addItemToCart(BASE_URL, customerId, productId, 1);
  checkout(BASE_URL, customerId, '', 200, { scenario_type: 'baseline' });
}
