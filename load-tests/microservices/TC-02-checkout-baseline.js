/*
Test ID: TC-02
Test name: Checkout Baseline
Architecture: Microservices
Endpoint or flow: catalog -> cart -> order checkout
VUs: 5
Duration: 60s
*/
import { buildOptions, uniqueCustomerId, fetchFirstProductId, addItemToCart, checkout } from '../shared.js';

const CATALOG_BASE_URL = __ENV.CATALOG_BASE_URL || 'http://localhost:8085';
const CART_BASE_URL = __ENV.CART_BASE_URL || 'http://localhost:8086';
const ORDER_BASE_URL = __ENV.ORDER_BASE_URL || 'http://localhost:8089';
export const options = buildOptions(5, '60s', {
  http_req_duration: ['p(95)<2500', 'p(99)<3500'],
});

export default function () {
  const customerId = uniqueCustomerId('micro-checkout-baseline');
  const productId = fetchFirstProductId(CATALOG_BASE_URL);
  addItemToCart(CART_BASE_URL, customerId, productId, 1);
  checkout(ORDER_BASE_URL, customerId, '', 200, { scenario_type: 'baseline' });
}
