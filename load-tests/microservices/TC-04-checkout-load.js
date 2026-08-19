/*
Test ID: TC-04
Test name: Checkout Load
Architecture: Microservices
Endpoint or flow: catalog -> cart -> order checkout
VUs: 20
Duration: 60s
*/
import { buildOptions, uniqueCustomerId, fetchFirstProductId, addItemToCart, checkout } from '../shared.js';

const CATALOG_BASE_URL = __ENV.CATALOG_BASE_URL || 'http://localhost:8085';
const CART_BASE_URL = __ENV.CART_BASE_URL || 'http://localhost:8086';
const ORDER_BASE_URL = __ENV.ORDER_BASE_URL || 'http://localhost:8089';
export const options = buildOptions(20, '60s', {
  http_req_duration: ['p(95)<3000', 'p(99)<4500'],
});

export default function () {
  const customerId = uniqueCustomerId('micro-checkout-load');
  const productId = fetchFirstProductId(CATALOG_BASE_URL);
  addItemToCart(CART_BASE_URL, customerId, productId, 1);
  checkout(ORDER_BASE_URL, customerId, '', 200, { scenario_type: 'load' });
}
