/*
Test ID: TC-07
Test name: Payment Delay Simulation
Architecture: Microservices
Endpoint or flow: catalog -> cart -> order checkout with artificialPaymentDelayMs=1000
VUs: 5
Duration: 30s
*/
import { buildOptions, uniqueCustomerId, fetchFirstProductId, addItemToCart, checkout } from '../shared.js';

const CATALOG_BASE_URL = __ENV.CATALOG_BASE_URL || 'http://localhost:8085';
const CART_BASE_URL = __ENV.CART_BASE_URL || 'http://localhost:8086';
const ORDER_BASE_URL = __ENV.ORDER_BASE_URL || 'http://localhost:8089';
export const options = buildOptions(5, '30s', {
  http_req_duration: ['p(95)<4500', 'p(99)<5500'],
});

export default function () {
  const customerId = uniqueCustomerId('micro-payment-delay');
  const productId = fetchFirstProductId(CATALOG_BASE_URL);
  addItemToCart(CART_BASE_URL, customerId, productId, 1);
  checkout(ORDER_BASE_URL, customerId, 'artificialPaymentDelayMs=1000', 200, { scenario_type: 'delay' });
}
