/*
Test ID: TC-04
Test name: Checkout Load
Architecture: Monolith
Endpoint or flow: add to cart and POST /api/orders/checkout/{customerId}
VUs: 20
Duration: 60s
*/
import { buildOptions, uniqueCustomerId, fetchFirstProductId, addItemToCart, checkout } from '../shared.js';

const BASE_URL = __ENV.MONOLITH_BASE_URL || 'http://localhost:8083';
export const options = buildOptions(20, '60s', {
  http_req_duration: ['p(95)<2500', 'p(99)<4000'],
});

export default function () {
  const customerId = uniqueCustomerId('mono-checkout-load');
  const productId = fetchFirstProductId(BASE_URL);
  addItemToCart(BASE_URL, customerId, productId, 1);
  checkout(BASE_URL, customerId, '', 200, { scenario_type: 'load' });
}
